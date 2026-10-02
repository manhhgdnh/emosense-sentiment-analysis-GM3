package app;

import core.lexicon.Lexicon;
import core.rule.RuleBasedAnalyzer;
import io.EmotionChartExporter;
import io.JsonExporter;
import model.AnalysisResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

/**
 * Non-interactive demo used to reproduce the figures and sample outputs
 * included in the repository without requiring an external API key.
 */
public class RuleBasedDemo {

    private static final String[][] SAMPLES = {
        {"contrast-positive", "The movie was long and boring, but the ending was fantastic."},
        {"strong-negative", "The staff was friendly, but the food was absolutely disgusting."},
        {"escalation-positive", "I am not only happy, but also thrilled about this opportunity!"},
        {"escalation-negative", "She is not only sad, but also devastated and heartbroken."},
        {"mixed-emotion", "I am angry about the delay, but relieved that we are safe."},
        {"negation", "I am not happy with this result."},
        {"fear-intensifier", "I was shocked by the news, and now I am deeply worried."},
        {"positive", "I feel happy and proud today."}
    };

    public static void main(String[] args) throws Exception {
        Lexicon lexicon = new Lexicon();
        lexicon.loadEmotionsFromFile("src/main/resources/lexicons/emotion_lexicon_v1.txt");
        lexicon.loadMarkersFromFile("src/main/resources/lexicons/marker_lexicon_v1.txt");
        RuleBasedAnalyzer analyzer = new RuleBasedAnalyzer(lexicon);

        new File("results/data").mkdirs();
        new File("results/json").mkdirs();
        new File("results/figures").mkdirs();

        try (FileWriter csv = new FileWriter("results/data/sample_scores.csv")) {
            csv.write("id,text,score20,rawS,positivePercent,negativePercent,dominantBucket\n");

            for (int i = 0; i < SAMPLES.length; i++) {
                String id = SAMPLES[i][0];
                String text = SAMPLES[i][1];
                AnalysisResult result = analyzer.analyze(text);

                csv.write(csv(id) + "," + csv(text) + "," +
                        fmt(result.getScore20()) + "," + fmt(result.getRawS()) + "," +
                        fmt(result.getPositivePercent()) + "," + fmt(result.getNegativePercent()) + "," +
                        csv(result.getDominantBucket().name()) + "\n");

                System.out.printf(Locale.ROOT, "%-22s score=%5.2f dominant=%s%n",
                        id, result.getScore20(), result.getDominantBucket());

                if (i == 1) {
                    EmotionChartExporter.exportPiePNG(
                            result.getBucketPercents(),
                            result.getDominantBucket(),
                            "Emotion Distribution — Rule-Based Example",
                            "results/figures/emotion_pie_rule.png"
                    );
                    new JsonExporter().save(result, text, "results/json/sample_rule_analysis.json");
                }
            }
        }
    }

    private static String fmt(double x) {
        return String.format(Locale.ROOT, "%.4f", x);
    }

    private static String csv(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
