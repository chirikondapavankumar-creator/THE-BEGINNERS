import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AIServices {

    // Groq API endpoint
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

   
    private static final String API_KEY = "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";

    public String refineCode(String userCode) {

        try {
            HttpClient client = HttpClient.newHttpClient();

            String prompt = """
You are a senior Java architect.

Your task is to refactor the provided Java code to industry-grade quality
while strictly preserving its functional behavior.

MANDATORY RULES:

1. Do NOT change numeric literals.
2. Do NOT modify loop conditions unless logically equivalent.
3. Preserve program behavior exactly.
4. Ensure the code compiles.
5. Improve structure, readability, and maintainability.
6. Apply SOLID principles where applicable.
7. Replace inefficient algorithms only if behavior remains identical.
8. Add input validation.
9. Add meaningful comments.
10. Use professional naming conventions.

Before returning the final code:
- Verify that all original numeric constants still exist.
- Verify that arithmetic logic is preserved.

Return only valid Java code.
After the code, add:

-----IMPROVEMENTS MADE-----

List structural, architectural, and performance improvements.

Now refactor:

""" + userCode;

            String requestBody = """
                    {
                      "model": "llama-3.3-70b-versatile",
                      "messages": [
                        {"role":"system","content":"You are an expert Java code optimizer."},
                        {"role":"user","content":"%s"}
                      ],
                      "temperature": 0.2
                    }
                    """.formatted(escapeJson(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return extractContent(response.body());

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    // Escape JSON characters
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    
    private String extractContent(String json) {
        try {
            String key = "\"content\":\"";
            int start = json.indexOf(key);
            if (start == -1)
                return json;

            start += key.length();

            int end = json.indexOf("\"}", start);
            if (end == -1)
                return json;

            String result = json.substring(start, end);

            return result.replace("\\n", "\n")
                    .replace("\\\"", "\"");
        } catch (Exception e) {
            return json;
        }
    }
}
