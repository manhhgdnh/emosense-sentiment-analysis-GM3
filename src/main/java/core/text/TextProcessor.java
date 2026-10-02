package core.text;

import model.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextProcessor {

    // Token pattern keeps words, emojis, and punctuation that matters for scoring
    // IMPORTANT: We intentionally do NOT tokenize commas (",") or single dots (".") because they break n-gram matching ("a little, bit" should still match "a little bit")
    // Ellipsis "..." and emphatic punctuation "!!", "?!", etc are still captured
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(\\.\\.\\.)|([!?]+)|(\\p{So})|([\\p{L}\\p{N}_']+)"
    );

    public List<Token> process(String text) {
        List<Token> tokens = new ArrayList<>();
        if (text == null) return tokens;

        // Put spaces around emojis so they become separate tokens
        text = text.replaceAll("([\\p{So}])", " $1 ");

        // Keep basic punctuation tokens so the rule engine can post-process them
        String cleaned = text.replaceAll("[^\\p{L}\\p{N}_\\p{So}\\s.!?']", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) return tokens;

        Matcher m = TOKEN_PATTERN.matcher(cleaned);
        int idx = 0;
        while (m.find()) {
            String t = m.group();
            if (t == null || t.isEmpty()) continue;
            tokens.add(new Token(t, idx));
            idx++;
        }
        return tokens;
    }
}
