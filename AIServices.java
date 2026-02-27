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
                    You are a senior software engineer working at a top-tier tech company.

                    Your task is to refactor and optimize the given Java code to production-grade quality.

                    STRICT REQUIREMENTS:

                    1. Follow clean code principles.
                    2. Improve time and space complexity wherever possible.
                    3. Use meaningful and professional variable/method names.
                    4. Apply proper object-oriented design if applicable.
                    5. Separate concerns into methods.
                    6. Add industry-level comments explaining logic.
                    7. Follow Java best practices.
                    8. Ensure readability and maintainability.
                    9. Remove redundant logic.
                    10. Handle edge cases if applicable.

                    OUTPUT FORMAT RULES:

                    - Return only valid Java code.
                    - No markdown fences.
                    - No explanations before the code.
                    - Code must compile.
                    - After the full code, add a section:

                    -----IMPROVEMENTS MADE-----

                    List bullet points explaining:
                    - What was optimized
                    - What structural improvements were made
                    - What complexity improvements were applied
                    - What clean code principles were implemented

                    Now refactor the following code:

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
