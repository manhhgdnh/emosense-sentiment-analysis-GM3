package app;

import api.ApiAnalyzer;

import core.lexicon.Lexicon;
import core.rule.RuleBasedAnalyzer;

import io.ErrorLog;
import io.JsonExporter;
import io.EmotionChartExporter;
import io.ExplanationPrinter;

import model.AnalysisResult;
import model.EmotionBucket;

import java.util.*;

public class EmoSenseApp {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        JsonExporter exporter = new JsonExporter();

        // API Analyzer 
        ApiAnalyzer apiAnalyzer = new ApiAnalyzer();

        // Rule-Based Analyzer
        Lexicon lexicon = new Lexicon();
        lexicon.loadEmotionsFromFile("src/main/resources/lexicons/emotion_lexicon_v1.txt");
        lexicon.loadMarkersFromFile("src/main/resources/lexicons/marker_lexicon_v1.txt");

        RuleBasedAnalyzer ruleAnalyzer = new RuleBasedAnalyzer(lexicon);

        System.out.println("   EMOTIONAL SENSATION   ");

        while (true) {
            System.out.println("\n   MENU   ");
            System.out.println("1. Enter your feeling today! ");
            System.out.println("2. Feedback wrong result ");
            System.out.println("3. Quit ");
            System.out.print("Your choice : ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (Exception e) {
                System.out.println("Please choose again! ");
                continue;
            }

            if (choice == 3) {
                System.out.println("I hope you have a good today and a good tomorrow and a good life ^^ ");
                break;
            }

            if (choice == 1) {
                System.out.println("Tell me your feeling please : ");
                String text = scanner.nextLine();

                // Depending on API
                AnalysisResult apiResult = null;
                if (apiAnalyzer.isConfigured()) {
                    System.out.println("\n API Analyzer ");
                    apiResult = apiAnalyzer.analyze(text);
                    System.out.println(apiResult.toString());
                } else {
                    System.out.println("\n API Analyzer skipped (set GEMINI_API_KEY to enable it).");
                }

                // Depending on RULE-BASED
                System.out.println("\n Rule Based Analyzer ");
                AnalysisResult ruleResult = ruleAnalyzer.analyze(text);
                System.out.println(ruleResult.toString());

                ExplanationPrinter.printScoreExplanation(ruleResult, ruleAnalyzer.getParams());

                if (ruleResult.getBucketPercents() != null) {
                    System.out.println("Emotion% (Rule): " + formatBucketPercents(
                            ruleResult.getBucketPercents(),
                            ruleResult.getDominantBucket()
                    ));

                    try {
                        EmotionChartExporter.exportPiePNG(
                                ruleResult.getBucketPercents(),
                                ruleResult.getDominantBucket(),
                                "Emotion Distribution (Rule-Based)",
                                "results/figures/emotion_pie_rule.png"
                        );
                        System.out.println("Chart saved: results/figures/emotion_pie_rule.png");
                    } catch (Exception ex) {
                        System.out.println("Chart cannot be exported : " + ex.getMessage());
                    }
                }

                // Export 
                System.out.print("\n Do you want to export JSON ? (n / api / rule / both) : ");
                String save = scanner.nextLine();

                if (save.equalsIgnoreCase("api")) {
                    if (apiResult != null) exporter.save(apiResult, text, "results/json/output_api.json");
                } else if (save.equalsIgnoreCase("rule")) {
                    exporter.save(ruleResult, text, "results/json/output_rule.json");
                } else if (save.equalsIgnoreCase("both")) {
                    if (apiResult != null) exporter.save(apiResult, text, "results/json/output_api.json");
                    exporter.save(ruleResult, text, "results/json/output_rule.json");
                }
            }

            else if (choice == 2) {
                System.out.println("Enter the sentence incorrect:");
                String input = scanner.nextLine();


                System.out.println("Enter your score (example : 15.0): ");
                double score = 0;
                try {
                    score = Double.parseDouble(scanner.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("Score is not correct, defaults to 0");
                }

                System.out.println("\n Then tell us your feedback please, it helps us a lot and then we can try better next time! ");
                String comment = scanner.nextLine();

                ErrorLog error = new ErrorLog(input, comment, score);
                error.saveToFile();
            }
        }

        scanner.close();
    }

    // Helpers 

    private static String formatBucketPercents(Map<EmotionBucket, Double> pct, EmotionBucket dominant) {

        // If dominant is NEUTRAL: show only neutral
        if (dominant == EmotionBucket.NEUTRAL) {
            return "{NEUTRAL=100.0%}";
        }

        // Otherwise : hide NEUTRAL and hide zero emotion buckets
        List<Map.Entry<EmotionBucket, Double>> items = new ArrayList<>();
        for (Map.Entry<EmotionBucket, Double> e : pct.entrySet()) {
            if (e.getKey() == EmotionBucket.NEUTRAL) continue;
            if (e.getValue() == null || e.getValue() <= 1e-10) continue;
            items.add(e);
        }

        // Sort by descending percent
        items.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        if (items.isEmpty()) {
            return "{NEUTRAL=100.0%}";
        }

        // Print the percentage 
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < items.size(); i++) {
            EmotionBucket b = items.get(i).getKey();
            double v = items.get(i).getValue();
            if (i > 0) sb.append(", ");
            sb.append(b.name()).append("=").append(String.format(Locale.ROOT, "%.1f", v)).append("%");
        }
        sb.append("}");
        return sb.toString();
    }


}
