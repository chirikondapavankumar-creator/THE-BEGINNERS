import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AIServices {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String API_KEY = "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"; 

    public String refineCode(String userCode) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            String prompt = "You are a senior Java architect.\n"
                + "Refactor the following code to industry-grade Java.\n"
                + "Return ONLY valid Java code, no markdown, no backticks.\n"
                + "After the code, add this line exactly:\n"
                + "-----IMPROVEMENTS MADE-----\n"
                + "Then list the improvements.\n\n"
                + "Code to refactor:\n" + userCode;

            String requestBody = "{"
                + "\"model\":\"llama-3.3-70b-versatile\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"You are an expert Java code optimizer. Return plain Java only, no markdown.\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}"
                + "],"
                + "\"temperature\":0.2"
                + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Groq status: " + response.statusCode());

            if (response.statusCode() != 200) {
                System.err.println("Groq error: " + response.body());
                return "API error " + response.statusCode();
            }

            return extractContent(response.body());

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractContent(String json) {
        try {
            String key = "\"content\":";
            int keyIdx = json.indexOf(key);
            if (keyIdx == -1) {
                System.err.println("No content key found in: " + json);
                return "Error: unexpected API response.";
            }

            int i = keyIdx + key.length();
            while (i < json.length() && json.charAt(i) != '"') i++;
            if (i >= json.length()) return "Error: malformed response.";
            i++; // skip opening quote

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
            return sb.toString().trim();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing response: " + e.getMessage();
        }
    }
}
