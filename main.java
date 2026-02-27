import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        StringBuilder codeBuilder = new StringBuilder();

        System.out.println("=================================");
        System.out.println("   Welcome to CodeRefine ");
        System.out.println("=================================");
        System.out.println("Paste your Java code below.");
        System.out.println("Type END on a new line to finish.\n");

        while (true) {
            String line = scanner.nextLine();

            if (line.equalsIgnoreCase("END")) {
                break;
            }

            codeBuilder.append(line).append("\n");
        }

        String userCode = codeBuilder.toString();

        AIServices aiService = new AIServices();
        String refinedCode = aiService.refineCode(userCode);

        System.out.println("\n--- AI Response ---");
        System.out.println(refinedCode);

        scanner.close();
    }
}
