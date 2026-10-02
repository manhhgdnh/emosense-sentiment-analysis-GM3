package interfaces;

import model.AnalysisResult;

public interface  IEmotionAnalyzer {
    AnalysisResult analyze(String text);
    
}
