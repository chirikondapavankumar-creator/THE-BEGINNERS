import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * CodeRefine HTTP Server.
 * Listens on port 3000 and exposes a /refine endpoint that accepts
 * a JSON body {"code":"..."} and returns {"result":"..."} with refined Java.
 */
public class Server {

    private static final int    PORT           = 3000;
    private static final int    MAX_BODY_BYTES = 50_000; // 50 KB input cap
    private static final int    THREAD_POOL    = 4;

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/refine", new CodeHandler());
        server.createContext("/explain", new ExplainHandler());
        server.createContext("/", new StaticHandler());
        server.setExecutor(Executors.newFixedThreadPool(THREAD_POOL));
        server.start();
        System.out.println("✅ CodeRefine server started at http://localhost:" + PORT);
        System.out.println("   GROQ_API_KEY set: " + (System.getenv("GROQ_API_KEY") != null ? "YES" : "NO ⚠️"));
    }

    // ─── Request handler ──────────────────────────────────────────────────────

    static class CodeHandler implements HttpHandler {

        private final AIServices aiService = new AIServices();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS headers — required for browser requests
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            // Handle preflight
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            // Only accept POST
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed. Use POST.\"}");
                return;
            }

            try {
                // Read and size-check body
                byte[] rawBytes = exchange.getRequestBody().readAllBytes();
                if (rawBytes.length > MAX_BODY_BYTES) {
                    sendJson(exchange, 413, "{\"error\":\"Request body too large. Max 50KB.\"}");
                    return;
                }

                String requestBody = new String(rawBytes, StandardCharsets.UTF_8).trim();
                System.out.println("📥 Request (" + rawBytes.length + " bytes): "
                    + requestBody.substring(0, Math.min(120, requestBody.length())));

                // Extract and validate code field
                String userCode  = extractFieldFromJson(requestBody, "code");
                String language  = extractFieldFromJson(requestBody, "language");
                String mode      = extractFieldFromJson(requestBody, "mode");
                String fromLang  = extractFieldFromJson(requestBody, "fromLang");
                String toLang    = extractFieldFromJson(requestBody, "toLang");

                if (userCode == null) {
                    sendJson(exchange, 400, "{\"error\":\"Missing or malformed \\\"code\\\" field in JSON body.\"}");
                    return;
                }
                if (userCode.isBlank()) {
                    sendJson(exchange, 400, "{\"error\":\"The \\\"code\\\" field is empty.\"}");
                    return;
                }

                // Call AI service
                String refinedCode = aiService.refineCode(userCode, language, mode, fromLang, toLang);

                // Detect error strings returned from AIServices
                if (refinedCode.startsWith("Error:") || refinedCode.startsWith("API error")) {
                    System.err.println("⚠️  AI service returned error: " + refinedCode);
                    sendJson(exchange, 502, "{\"error\":\"" + escapeJson(refinedCode) + "\"}");
                    return;
                }

                String jsonResponse = "{\"result\":\"" + escapeJson(refinedCode) + "\"}";
                sendJson(exchange, 200, jsonResponse);
                System.out.println("✅ Refinement complete — " + refinedCode.length() + " chars returned.");

            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, "{\"error\":\"Internal server error: " + escapeJson(e.getMessage()) + "\"}");
            }
        }

        // ─── JSON helpers ─────────────────────────────────────────────────────

        /**
         * Manually extracts the value of the "code" key from a JSON string.
         * Handles standard JSON escape sequences.
         *
         * @param json the raw JSON request body
         * @return the unescaped code string, or null if not found / malformed
         */
        private String extractFieldFromJson(String json, String field) {
            if (json == null || json.isBlank()) return null;

            String key    = "\"" + field + "\"";
            int keyIdx    = json.indexOf(key);
            if (keyIdx == -1) return null;

            int colonIdx  = json.indexOf(':', keyIdx + key.length());
            if (colonIdx == -1) return null;

            int openQuote = json.indexOf('"', colonIdx + 1);
            if (openQuote == -1) return null;

            StringBuilder sb = new StringBuilder();
            int i = openQuote + 1;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    switch (next) {
                        case '"'  -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case 'n'  -> sb.append('\n');
                        case 'r'  -> sb.append('\r');
                        case 't'  -> sb.append('\t');
                        default   -> sb.append(next);
                    }
                    i += 2;
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                    i++;
                }
            }
            return sb.toString();
        }

        /**
         * Escapes a string for safe embedding inside a JSON string value.
         */
        private String escapeJson(String text) {
            if (text == null) return "";
            return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        }

        /**
         * Sends a UTF-8 encoded JSON response with the given HTTP status code.
         */
        private void sendJson(HttpExchange exchange, int statusCode, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // ─── Explain Handler ─────────────────────────────────────────────────────
    static class ExplainHandler implements HttpHandler {

        private final AIServices aiService = new AIServices();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed.\"}");
                return;
            }

            try {
                byte[] rawBytes = exchange.getRequestBody().readAllBytes();
                String requestBody = new String(rawBytes, java.nio.charset.StandardCharsets.UTF_8).trim();
                String code = extractFieldFromJson(requestBody, "code");

                if (code == null || code.isBlank()) {
                    sendJson(exchange, 400, "{\"error\":\"Missing code field.\"}");
                    return;
                }

                String explanation = aiService.explainCode(code);
                String jsonResponse = "{\"explanation\":\"" + escapeJson(explanation) + "\"}";
                sendJson(exchange, 200, jsonResponse);

            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        private String extractFieldFromJson(String json, String field) {
            if (json == null || json.isBlank()) return null;
            String key = "\"" + field + "\"";
            int keyIdx = json.indexOf(key);
            if (keyIdx == -1) return null;
            int colonIdx = json.indexOf(':', keyIdx + key.length());
            if (colonIdx == -1) return null;
            int openQuote = json.indexOf('"', colonIdx + 1);
            if (openQuote == -1) return null;
            StringBuilder sb = new StringBuilder();
            int i = openQuote + 1;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    switch (next) {
                        case '"'  -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case 'n'  -> sb.append('\n');
                        case 'r'  -> sb.append('\r');
                        case 't'  -> sb.append('\t');
                        default   -> sb.append(next);
                    }
                    i += 2;
                } else if (c == '"') { break; }
                else { sb.append(c); i++; }
            }
            return sb.toString();
        }

        private String escapeJson(String text) {
            if (text == null) return "";
            return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        }

        private void sendJson(HttpExchange exchange, int statusCode, String body) throws IOException {
            byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (java.io.OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }


    // ─── Static File Handler ─────────────────────────────────────────────────
    // Serves index.html at http://localhost:3000
    // This avoids file:// CORS issues with fetch() calls to localhost
    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            String path = exchange.getRequestURI().getPath();

            // Only serve the root / or /index.html
            if (!path.equals("/") && !path.equals("/index.html")) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            // Look for index.html next to the running jar/class files
            java.io.File file = new java.io.File("index.html");
            if (!file.exists()) {
                // Try FrontEnd folder
                file = new java.io.File("FrontEnd/index.html");
            }

            if (!file.exists()) {
                String msg = "index.html not found. Place it in the same folder as Server.java";
                byte[] bytes = msg.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(404, bytes.length);
                try (java.io.OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                return;
            }

            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (java.io.OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

}
