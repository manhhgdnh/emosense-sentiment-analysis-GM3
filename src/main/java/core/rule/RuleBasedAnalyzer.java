package core.rule;

import core.params.SystemParams;

import core.lexicon.Lexicon;
import core.text.TextProcessor;

import interfaces.IEmotionAnalyzer;

import model.AnalysisResult;
import model.Token;

import java.util.List;

public class RuleBasedAnalyzer implements IEmotionAnalyzer {

    private final Lexicon lexicon;
    private final TextProcessor processor;
    private final RuleEngine engine;
    private final SystemParams params;

    public RuleBasedAnalyzer(Lexicon lexicon) {
        this.lexicon = lexicon;
        this.processor = new TextProcessor();
        this.engine = new RuleEngine();
        this.params = new SystemParams();
    }

    @Override
    public AnalysisResult analyze(String text) {
        List<Token> tokens = processor.process(text);
        return engine.evaluate(tokens, lexicon, params);
    }

    public SystemParams getParams() {
        return params;
    }
}
