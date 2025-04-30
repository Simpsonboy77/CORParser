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
        String currentClass = null;
        String currentType = null;

        // Match section headers like "Guile Passives: Novice"
        Pattern sectionPattern = Pattern.compile("(?i)^(Combat|Guile|Arcane|Divine|Academic|Crafting & Labor)\\s+(Passives|Feats|Talents):\\s*(Novice|Apprentice|Journeyman|Master|Grand Master)?");

        // Match skill format
        Pattern skillPattern = Pattern.compile(
                "^\\s*([\\w'’ \\-/]+):\\s*" +                                      // Name
                        "\\(?\\s*(\\d+)\\s*(?i:Exp|EXP)\\)?\\s*" +                         // EXP
                        "(?:\\(?\\s*(?:To use|Cost|To Use)\\s*:\\s*(\\d+)\\s*(Steam|Aether)\\s*\\)?)?\\s*" + // Cost
                        "(?:\\(?Pre-req|Pre req\\s*:?\\s*(.*?)\\)?)?\\s*" +                        // Prereq
                        "(.*?)$",                                                         // Initial description
                Pattern.CASE_INSENSITIVE
        );

        String[] lines = text.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Update current section (Class, Type, Tier)
            Matcher sectionMatcher = sectionPattern.matcher(line);
            if (sectionMatcher.find()) {
                currentClass = sectionMatcher.group(1);
                currentType = capitalize(sectionMatcher.group(2));
                if (sectionMatcher.group(3) != null) {
                    currentTier = capitalize(sectionMatcher.group(3));
                }
                continue;
            }

            // Detect skills
            Matcher skillMatcher = skillPattern.matcher(line);
            if (skillMatcher.find()) {
                String name = skillMatcher.group(1).trim();
                String expCost = skillMatcher.group(2);
                String costAmount = skillMatcher.group(3);
                String costType = skillMatcher.group(4);
                String prereq = skillMatcher.group(5) != null ? skillMatcher.group(5) : "";
                StringBuilder description = new StringBuilder(skillMatcher.group(6).trim());

                // Accumulate description until a line contains only one character
                int j = i + 1;
                while (j < lines.length && lines[j].trim().length() > 1) {
                    description.append(" ").append(lines[j].trim());
                    j++;
                }
                i = j; // skip ahead

                String steamCost = "";
                String aetherCost = "";
                if ("Steam".equalsIgnoreCase(costType)) {
                    steamCost = costAmount;
                } else if ("Aether".equalsIgnoreCase(costType)) {
                    aetherCost = costAmount;
                }

                parsedSkills.add(new String[]{
                        name, currentTier, currentClass, currentType, expCost, steamCost, aetherCost, prereq, description.toString().trim()
                });
            }
        }

        // Output CSV
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            for (String[] skill : parsedSkills) {
                writer.println(String.join(",", escapeForCsv(skill)));
            }
        }

        System.out.println("Parsing complete. Output saved to: " + outputPath);
    }

    private static String[] escapeForCsv(String[] fields) {
        return Arrays.stream(fields)
                .map(field -> {
                    if (field == null) return "";
                    return field.contains(",") ? "\"" + field.replace("\"", "\"\"") + "\"" : field;
                })
                .toArray(String[]::new);
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
