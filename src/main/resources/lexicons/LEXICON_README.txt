LEXICON FILES 

Two separate files:
1) emotion_lexicon_v1.txt
   - FORMAT: phrase;EMO;valence;baseIntensity;label
   - valence in [-1,1], baseIntensity in [0,2] (RuleParams clamps anyway)
   - label in {JOY,SADNESS,ANGER,FEAR,DISGUST,SURPRISE}

2) marker_lexicon_v1.txt
   - FORMAT: phrase;TYPE;factor
   - TYPE in {NEG,INT,DIM,CON,ESC}

NOTE : I implemented n-gram matching, you can keep spaces.
