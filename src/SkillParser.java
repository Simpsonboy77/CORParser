import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;

public class SkillParser {

    public enum tierEnum {
        None,
        Races,
        CulturalPowers {
            public String toString() {
                return "Cultural Powers";
            }
        },
        Novice,
        Apprentice,
        Journeyman,
        Master,
        GrandMaster {
            public String toString(){
                return "Grand Master";
            }
        }
    }

    public String currentTier = "";
    public String currentArchetype = "";
    public String currentFocus = "";
    public String currentType = "";
    public String name = "";
    String inputPdf = "CoR Rules Guide 2025 4.6.2025-1.pdf";
    String outputPath = "CoR_Skills_Parsed.csv";
    long startTime = 0;

    String[] lines;

    String expCost, steamCost, aetherCost, prereq, multipurchase, description = "";

    Pattern tierPattern;
    Pattern sectionPattern;
    Pattern subClassPattern;
    //Pattern skillHeaderPattern = Pattern.compile("^(?:\\s|•)*([\\w'’ \\-/]+):\\s*\\((\\d+)\\s*Exp\\)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    Pattern skillNamePattern;
    Pattern expPattern;
    Pattern costPattern;
    Pattern prereqPattern;
    Pattern multipurchasePattern;
    Pattern titlePattern;

    List<String[]> parsedSkills;
    List<String[]> parsedTitles;

    public SkillParser(String inputPdf, String outoutCSV){
        inputPdf = inputPdf;
        outputPath = outoutCSV;
        startTime = System.currentTimeMillis();


    }

    void digestPDF(){
        String text = "";
        try{
            PDDocument document = Loader.loadPDF(new File(inputPdf));
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(document);
            document.close();
        }
        catch (Exception e) {
            System.err.println("Fix yo paths");
            System.exit(1);
        }
        lines = text.split("\\r?\\n");
    }
    void initPatterns()
    {
        parsedSkills = new ArrayList<>();
        parsedTitles = new ArrayList<>();
        parsedSkills.add(new String[]{"Name", "Tier", "Class", "Subclass", "Type", "EXP Cost", "Steam Cost", "Aether Cost", "Prerequisites", "Multi Purchase", "Description"});

        String classRegex = "(Combat|Guile|Arcane|Divine|Academic|Crafting & Labor|Crafting and Labor)";

        //matches Class: subclass into 2 capturing groups, must be done in master and gm only
        String subclassRegex = classRegex +": "+ "(\\w*)";

        String typeRegex = "(Spells|Passives|Feats|Talents|Runes and rituals)";
        String tierRegex = "(";
        for (tierEnum e : tierEnum.values()) tierRegex += "|" + e.toString();
        tierRegex = tierRegex + ")";

         tierPattern = Pattern.compile("Chapter 1\\d: " + tierRegex + "\\s?$", Pattern.CASE_INSENSITIVE);

        sectionPattern = Pattern.compile("^"+ classRegex + "\\s+"+ typeRegex +":\\s*"+tierRegex, Pattern.CASE_INSENSITIVE);
        subClassPattern = Pattern.compile(subclassRegex,Pattern.CASE_INSENSITIVE);
        //skillHeaderPattern = Pattern.compile("^(?:\\s|•)*([\\w'’ \\-/]+):\\s*\\((\\d+)\\s*Exp\\)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
        skillNamePattern = Pattern.compile("•?(.*): \\(", Pattern.CASE_INSENSITIVE);
        expPattern = Pattern.compile(".*\\((\\d).*xp\\)", Pattern.CASE_INSENSITIVE);
        costPattern = Pattern.compile("\\((?:To use|Cost):\\s*(\\d+)\\s*(Steam|Aether)\\)", Pattern.CASE_INSENSITIVE);
        prereqPattern = Pattern.compile("\\(Pre[-\\s]?req:\\s*(.*?)\\)", Pattern.CASE_INSENSITIVE);
        multipurchasePattern = Pattern.compile("\\(Multi[-\\s]?purchase\\)", Pattern.CASE_INSENSITIVE);
        titlePattern = Pattern.compile("Title: " + tierRegex + "( \\w*)");
    }


    public static void main(String[] args) throws IOException {


        SkillParser parser = new SkillParser("CoR Rules Guide 2025 4.6.2025-1.pdf", "CoR_Skills_Parsed.csv");

        parser.digestPDF();

        parser.initPatterns();


        parser.digestLines();

        parser.writeCSV();
    }

    private void writeCSV() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(outputPath));
            for (String[] skill : parsedSkills) {
                writer.println(String.join(",", escapeForCsv(skill)));
            }
        }
        catch (Exception e) {
            ;
        }


        System.out.println("Parsing complete. Output saved to: " + outputPath);
        System.out.println(parsedSkills.size() + " skills parsed");
        System.out.println("Parser ran in "+ (System.currentTimeMillis() - startTime) + " milliseconds.");
    }

    private void digestLines() {

        boolean parseSubClasses = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            String nextTier = SkillParser.patternMatch(tierPattern,line);
            if (!nextTier.isEmpty()){
                currentTier = nextTier;
                System.out.println(line);
                System.out.println(parsedSkills.size() + " skills parsed");
                try {
                    tierEnum e = tierEnum.valueOf(currentTier);
                    if (e == tierEnum.Master || e == tierEnum.GrandMaster)
                        parseSubClasses = true;
                }
                catch (Exception e) {
                }

            }

            //fast loop
            if (currentTier.isEmpty())
                continue;

            // Detect class/type/tier from section headers
            Matcher sectionMatcher = sectionPattern.matcher(line);
            if (sectionMatcher.find()) {

                currentArchetype = sectionMatcher.group(1);
                currentType = capitalize(sectionMatcher.group(2));
                continue;
            }
            else if(parseSubClasses){
                //check if we are in master tier or higher
                Matcher subclassMatcher = subClassPattern.matcher(line);
                if (subclassMatcher.find()) {
                    currentArchetype = subclassMatcher.group(1);
                    currentFocus = subclassMatcher.group(2);
                }
            }

            //detect titles
            Matcher titleMatcher = titlePattern.matcher(line);

            if(titleMatcher.find()){
                //can double check that tier matches current tier

                name = titleMatcher.group(2);
            }

            // Detect skill header
            Matcher skillNameMatcher = skillNamePattern.matcher(line);
            if (skillNameMatcher.find()) {
                name = skillNameMatcher.group(1).trim();

                String steamCost = "", aetherCost = "", multipurchase = "";

                String expCost = patternMatch(expPattern, line);
                String prereq = patternMatch(prereqPattern, line);



                // Match cost and prereq from the full line
                Matcher costMatch = costPattern.matcher(line);
                if (costMatch.find()) {
                    if ("Steam".equalsIgnoreCase(costMatch.group(2))) steamCost = costMatch.group(1);
                    else if ("Aether".equalsIgnoreCase(costMatch.group(2))) aetherCost = costMatch.group(1);
                }

                Matcher multipurchaseMatch = multipurchasePattern.matcher(line);
                if (multipurchaseMatch.find()) {
                    multipurchase = "1";
                }

                // Clean up description
                int lastParen = line.lastIndexOf(')');
                String description = (lastParen >= 0 && lastParen < line.length() - 1)
                        ? line.substring(lastParen + 1).trim()
                        : "";

                // Accumulate multi-line description
                int j = i + 1;
                while (j < lines.length && lines[j].trim().length() > 1) {
                    sectionMatcher = sectionPattern.matcher(lines[j].trim());
                    skillNameMatcher = skillNamePattern.matcher(lines[j].trim());
                    if (sectionMatcher.find() || skillNameMatcher.find()){
                        j--;
                        break;
                    }
                    description += " " + lines[j].trim();
                    j++;

                    if (lines[j].trim().contains("•")){
                        j--;
                        break;
                    }
                }

                i = j;


            }
            parsedSkills.add(new String[]{
                    name, currentTier, currentArchetype, currentFocus, currentType,
                    expCost, steamCost, aetherCost, prereq, multipurchase, description.trim()
            });
        }
    }

    private void addEntryToCSV(){

    }
    private static String[] escapeForCsv(String[] fields) {
        return Arrays.stream(fields)
                .map(field -> {
                    if (field == null) return "";
                    return field.contains(",") ? "\"" + field.replace("\"", "\"\"") + "\"" : field;
                })
                .toArray(String[]::new);
    }
    private static String patternMatch(Pattern p, String input ){
        Matcher matcher = p.matcher(input);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }


}
