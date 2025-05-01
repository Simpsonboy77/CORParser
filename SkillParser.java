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

        Pattern sectionPattern = Pattern.compile("(?i)^(Combat|Guile|Arcane|Divine|Academic|Crafting & Labor|Crafting and Labor)\\s+(Passives|Feats|Talents):\\s*(Novice|Apprentice|Journeyman|Master|Grand Master)?");
        Pattern skillHeaderPattern = Pattern.compile("^\\s*([\\w'’ \\-/]+):\\s*\\((\\d+)\\s*Exp\\)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
        Pattern costPattern = Pattern.compile("\\((?:To use|Cost):\\s*(\\d+)\\s*(Steam|Aether)\\)", Pattern.CASE_INSENSITIVE);
        Pattern prereqPattern = Pattern.compile("\\(Pre[-\\s]?req:\\s*(.*?)\\)", Pattern.CASE_INSENSITIVE);

        String[] lines = text.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Detect class/type/tier from section headers
            Matcher sectionMatcher = sectionPattern.matcher(line);
            if (sectionMatcher.find()) {
                currentClass = sectionMatcher.group(1);
                currentType = capitalize(sectionMatcher.group(2));
                if (sectionMatcher.group(3) != null) {
                    currentTier = capitalize(sectionMatcher.group(3));
                }
                continue;
            }

            // Detect skill header
            Matcher skillMatcher = skillHeaderPattern.matcher(line);
            if (skillMatcher.find()) {
                String name = skillMatcher.group(1).trim();
                String expCost = skillMatcher.group(2).trim();
                String remainder = skillMatcher.group(3).trim();

                String steamCost = "", aetherCost = "", prereq = "";

                // Match cost and prereq from the full line
                Matcher costMatch = costPattern.matcher(line);
                if (costMatch.find()) {
                    if ("Steam".equalsIgnoreCase(costMatch.group(2))) steamCost = costMatch.group(1);
                    else if ("Aether".equalsIgnoreCase(costMatch.group(2))) aetherCost = costMatch.group(1);
                }

                Matcher prereqMatch = prereqPattern.matcher(line);
                if (prereqMatch.find()) {
                    prereq = prereqMatch.group(1).trim();
                }

                // Clean up description
                String description = line
                        .replaceFirst(skillHeaderPattern.pattern(), "")
                        .replaceAll(costPattern.pattern(), "")
                        .replaceAll(prereqPattern.pattern(), "")
                        .trim();

                // Accumulate multi-line description
                int j = i + 1;
                while (j < lines.length && lines[j].trim().length() > 1) {
                    description += " " + lines[j].trim();
                    j++;
                }
                i = j;

                parsedSkills.add(new String[]{
                        name, currentTier, currentClass, currentType,
                        expCost, steamCost, aetherCost, prereq, description.trim()
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
                .map(field -> {
                    if (field == null) return "";
                    return field.contains(",") ? "\"" + field.replace("\"", "\"\"") + "\"" : field;
                })
                .toArray(String[]::new);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
