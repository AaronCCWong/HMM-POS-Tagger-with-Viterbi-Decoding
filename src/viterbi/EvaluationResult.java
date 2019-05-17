package viterbi;

import java.util.List;

public class EvaluationResult {

    List<List<String>> sentences;
    List<List<String>> sentenceTags;

    public EvaluationResult(List<List<String>> sentences, List<List<String>> sentenceTags) {
        this.sentences = sentences;
        this.sentenceTags = sentenceTags;
    }

}
