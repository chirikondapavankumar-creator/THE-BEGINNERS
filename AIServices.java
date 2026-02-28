import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AIServices {

    private static final String API_URL        = "https://api.groq.com/openai/v1/chat/completions";
    private static final String API_KEY        = "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"; // ← paste your key here
    private static final String MODEL          = "llama-3.3-70b-versatile";
    private static final int    MAX_INPUT_CHARS = 12_000;
    private static final int    MAX_TOKENS      = 4096;

    // ─── System prompt ────────────────────────────────────────────────────────
    private static final String SYSTEM_PROMPT =
        "You are an expert code optimizer and software architect.\n"
      + "Your job is to refactor code submitted by the user into clean, production-grade code.\n\n"
      + "LANGUAGE RULE — this is the most important rule:\n"
      + "- Detect the programming language of the input code.\n"
      + "- Output the refactored code in THE SAME LANGUAGE as the input.\n"
      + "- If the input is Python, output Python.\n"
      + "- If the input is Java, output Java.\n"
      + "- If the input is JavaScript, output JavaScript.\n"
      + "- If the input is C++, output C++.\n"
      + "- NEVER convert code from one language to another.\n\n"
      + "ABSOLUTE RULES — never break these:\n"
      + "1. Output ONLY valid, compilable/runnable code in the detected language. No markdown, no backticks, no inline explanations.\n"
      + "2. PRESERVE every numeric literal EXACTLY as it appears in the input — including 0.\n"
      + "   - `v = 0` must stay `v = 0`. Zero is a valid value, never remove it.\n"
      + "   - `count = 9` must stay `count = 9`.\n"
      + "   - 0, 1, 2, or ANY integer is a real value — NEVER treat it as missing or blank.\n"
      + "   - NEVER produce `int v = ;` or `v = ` with no value — this means you dropped a value. FORBIDDEN.\n"
      + "3. NEVER produce incomplete statements. Every assignment and return must have a value.\n"
      + "4. Preserve the original program logic and behavior exactly.\n"
      + "5. Add appropriate documentation comments for the detected language:\n"
      + "   - Java → Javadoc\n"
      + "   - Python → docstrings\n"
      + "   - JavaScript → JSDoc\n"
      + "   - C++ → inline comments\n"
      + "6. Improve naming, structure, and readability only — never at the cost of rules 2 or 3.\n"
      + "7. Optimize complexity where possible without changing behavior.\n"
      + "8. Handle null/None inputs and edge cases defensively.\n"
      + "9. After the complete code, output this line exactly:\n"
      + "-----IMPROVEMENTS MADE-----\n"
      + "Then list each improvement as a numbered point. Start with: 'Language detected: <language>'\n"
      + "10. Do NOT add any text before the code.";

    // ─── Public API ───────────────────────────────────────────────────────────

    public String refineCode(String userCode, String language) {
        if (userCode == null || userCode.isBlank()) {
            return "Error: No code provided.";
        }
        if (userCode.length() > MAX_INPUT_CHARS) {
            return "Error: Input too large. Please submit under " + MAX_INPUT_CHARS + " characters.";
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

            String requestBody = buildRequestBody(userCode, language);

            System.out.println("📤 Sending to Groq...");
            System.out.println("   Input preview: " + userCode.substring(0, Math.min(80, userCode.length())));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Groq status: " + response.statusCode());

            if (response.statusCode() != 200) {
                System.err.println("Groq error body: " + response.body());
                return "API error " + response.statusCode() + ": " + extractErrorMessage(response.body());
            }

            String content = extractContent(response.body());
            System.out.println("📥 Output preview: " + content.substring(0, Math.min(120, content.length())));

            String validationError = validateOutput(content);
            if (validationError != null) {
                System.err.println("⚠️  Validation failed: " + validationError);
                return "Error: Model returned invalid output — " + validationError;
            }

            return content;

        } catch (java.net.http.HttpTimeoutException e) {
            return "Error: Request timed out. Please try again.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Builds the Groq API JSON request body.
     */
    private String buildRequestBody(String userCode, String language) {
        String systemContent = escapeJson(SYSTEM_PROMPT);
        String numberReminder = buildNumberReminder(userCode);

        String langInstruction = (language != null && !language.isBlank())
            ? "Refactor the following code in " + language + ". Output ONLY " + language + " code — do NOT convert to any other language.\n"
            : "Refactor the following code. Keep it in the SAME programming language — do NOT convert it.\n";

        String userContent = escapeJson(
            langInstruction
          + "CRITICAL — the following numeric values appear in the code and MUST appear unchanged in your output:\n"
          + numberReminder + "\n"
          + "Do NOT drop, blank, or omit any of them.\n\n"
          + userCode
        );

        return "{"
            + "\"model\":\"" + MODEL + "\","
            + "\"messages\":["
            + "{\"role\":\"system\",\"content\":\"" + systemContent + "\"},"
            + "{\"role\":\"user\",\"content\":\"" + userContent + "\"}"
            + "],"
            + "\"temperature\":0.1,"
            + "\"max_tokens\":" + MAX_TOKENS + ","
            + "\"top_p\":0.85"
            + "}";
    }

    /**
     * Scans the user code and returns a comma-separated list of all numeric
     * literals found, so they can be explicitly listed in the prompt.
     */
    private String buildNumberReminder(String code) {
        java.util.Set<String> numbers = new java.util.LinkedHashSet<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\\b\\d+\\.?\\d*\\b")
            .matcher(code);
        while (m.find()) {
            numbers.add(m.group());
        }
        return numbers.isEmpty() ? "(none)" : String.join(", ", numbers);
    }

    /**
     * Validates the model output before returning it to the frontend.
     * Returns null if valid, or an error description string if not.
     */
    private String validateOutput(String output) {
        if (output == null || output.isBlank()) {
            return "Output was empty.";
        }
        if (output.matches("(?s).*=\\s*;.*")) {
            return "Output contains empty assignment (= ;) — model dropped a value.";
        }
        return null;
    }

    /**
     * Extracts the "content" field from the Groq JSON response.
     */
    private String extractContent(String json) {
        try {
            String key = "\"content\":";
            int keyIdx = json.indexOf(key);
            if (keyIdx == -1) {
                System.err.println("No content key in response: " + json);
                return "Error: unexpected API response format.";
            }

            int i = keyIdx + key.length();
            while (i < json.length() && json.charAt(i) != '"') i++;
            if (i >= json.length()) return "Error: malformed response.";
            i++;

            StringBuilder sb = new StringBuilder();
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
                        case 'u'  -> {
                            if (i + 5 < json.length()) {
                                String hex = json.substring(i + 2, i + 6);
                                try {
                                    sb.append((char) Integer.parseInt(hex, 16));
                                    i += 6;
                                    continue;
                                } catch (NumberFormatException e) {
                                    sb.append(next);
                                }
                            }
                        }
                        default -> sb.append(next);
                    }
                    i += 2;
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                    i++;
                }
            }
            return sb.toString().trim();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing response: " + e.getMessage();
        }
    }

    /**
     * Extracts a readable message from a Groq error response body.
     */
    private String extractErrorMessage(String json) {
        try {
            String key = "\"message\":\"";
            int idx = json.indexOf(key);
            if (idx == -1) return json.substring(0, Math.min(120, json.length()));
            int start = idx + key.length();
            int end   = json.indexOf('"', start);
            return end == -1 ? json.substring(start) : json.substring(start, end);
        } catch (Exception e) {
            return "Unknown error.";
        }
    }

    /**
     * Escapes a string for safe embedding inside a JSON string value.
     */
    private String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
