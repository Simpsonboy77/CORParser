import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;

public class SkillParser {

    public static void main(String[] args) throws IOException {
        String inputPdf = "CoR_Rules_Guide_2025.pdf";
        String outputPath = "CoR_Skills_Parsed.csv";

        PDDocument document = Loader.loadPDF(new File(inputPdf));
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();

        List<String[]> parsedSkills = new ArrayList<>();
        parsedSkills.add(new String[]{"Name", "Tier", "Class", "Type", "EXP Cost", "Steam Cost", "Aether Cost", "Prerequisites", "Description"});

        String currentTier = null;
        Pattern chapterPattern = Pattern.compile("(?i)chapter\\s+(1[4-8])");

        Pattern skillPattern = Pattern.compile(
                "^\\s*([\\w'’ \\-/]+):\\s*" +                                      // Name
                        "\\(?\\s*(\\d+)\\s*(?i:Exp|EXP)\\)?\\s*" +                         // EXP
                        "(?:\\(?\\s*(?:To use|Cost)\\s*:\\s*(\\d+)\\s*(Steam|Aether)\\s*\\)?)?\\s*" + // Optional cost
                        "(?:\\(?Pre-req\\s*:?\\s*(.*?)\\)?)?\\s*" +                        // Optional prereq
                        "(.*?)$",                                                         // Description
                Pattern.CASE_INSENSITIVE
        );

        String[] lines = text.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher chapterMatcher = chapterPattern.matcher(line);
            if (chapterMatcher.find()) {
                switch (chapterMatcher.group(1)) {
                    case "14": currentTier = "Novice"; break;
                    case "15": currentTier = "Apprentice"; break;
                    case "16": currentTier = "Journeyman"; break;
                    case "17": currentTier = "Master"; break;
                    case "18": currentTier = "Grand Master"; break;
                }
                continue;
            }

            Matcher skillMatcher = skillPattern.matcher(line);
            if (skillMatcher.find()) {
                String name = skillMatcher.group(1).trim();
                String expCost = skillMatcher.group(2);
                String costAmount = skillMatcher.group(3);
                String costType = skillMatcher.group(4);
                String prereq = skillMatcher.group(5) != null ? skillMatcher.group(5).trim() : "";
                StringBuilder description = new StringBuilder(skillMatcher.group(6).trim());

                // Read lines until one contains only a single character
                int j = i + 1;
                while (j < lines.length && lines[j].trim().length() > 1) {
                    description.append(" ").append(lines[j].trim());
                    j++;
                }

                // Skip to the next unprocessed line
                i = j;

                String steamCost = "";
                String aetherCost = "";

                if ("Steam".equalsIgnoreCase(costType)) {
                    steamCost = costAmount;
                } else if ("Aether".equalsIgnoreCase(costType)) {
                    aetherCost = costAmount;
                }

                parsedSkills.add(new String[]{
                        name, currentTier, "", "", expCost, steamCost, aetherCost, prereq, description.toString().trim()
                });
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            for (String[] skill : parsedSkills) {
                writer.println(String.join(",", escapeForCsv(skill)));
            }
        }

        System.out.println("Parsing complete. Output saved to: " + outputPath);
    }

    private static String[] escapeForCsv(String[] fields) {
        return Arrays.stream(fields)
                .map(field -> field.contains(",") ? "\"" + field.replace("\"", "\"\"") + "\"" : field)
                .toArray(String[]::new);
    }
}
