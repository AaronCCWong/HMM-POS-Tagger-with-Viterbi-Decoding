package viterbi;

public class SuffixStats {

    Double tagSuffixProb = 0.0;
    Double suffixProb = 0.0;
    Double tagProb = 0.0;

    public SuffixStats(SuffixTree tree, String suffix, String tag) {
        while (!tree.hasSuffix(suffix) && suffix.length() > 0) {
            suffix = suffix.substring(1);
        }
        tagSuffixProb = tree.getTagSuffixProbability(suffix, tag);
        suffixProb = tree.getSuffixProbability(suffix);
        tagProb = tree.getTagProbability(suffix, tag);
    }

}
