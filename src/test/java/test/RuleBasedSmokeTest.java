package test;

import core.lexicon.Lexicon;
import core.rule.RuleBasedAnalyzer;
import model.AnalysisResult;

public class RuleBasedSmokeTest {

    public static void main(String[] args) {
        Lexicon lexicon = new Lexicon();
        lexicon.loadEmotionsFromFile("src/main/resources/lexicons/emotion_lexicon_v1.txt");
        lexicon.loadMarkersFromFile("src/main/resources/lexicons/marker_lexicon_v1.txt");
        RuleBasedAnalyzer analyzer = new RuleBasedAnalyzer(lexicon);

        AnalysisResult happy = analyzer.analyze("I am very happy!");
        AnalysisResult sad = analyzer.analyze("I am deeply sad.");
        AnalysisResult negatedHappy = analyzer.analyze("I am not happy with this result.");
        AnalysisResult contrast = analyzer.analyze("The staff was friendly, but the food was absolutely disgusting.");

        require(happy.getScore20() > 10.0, "positive cue should score above the midpoint");
        require(sad.getScore20() < 10.0, "negative cue should score below the midpoint");
        require(negatedHappy.getScore20() < 10.0, "negation should reverse/dampen a positive cue");
        require(contrast.getScore20() < 10.0, "strong post-contrast negative cue should dominate this example");

        System.out.println("RuleBasedSmokeTest: OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
