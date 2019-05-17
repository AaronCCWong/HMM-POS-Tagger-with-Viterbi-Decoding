package viterbi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SuffixTreeBuilder {

    private Integer MAX_WORD_FREQUENCY;
    private Integer MAX_SUFFIX_LENGTH;

    public SuffixTreeBuilder(Integer maxSuffixLength, Integer maxWordFreq) {
        MAX_SUFFIX_LENGTH = maxSuffixLength;
        MAX_WORD_FREQUENCY = maxWordFreq;
    }

    public SuffixTree buildUpperCaseTree(BigramModel model) {
        boolean upperCase = true;
        List<String> words = model.getWords(upperCase);

        return buildTree(model, words);
    }

    public SuffixTree buildLowerCaseTree(BigramModel model) {
        boolean upperCase = false;
        List<String> words = model.getWords(upperCase);

        return buildTree(model, words);
    }

    public SuffixTree buildTree(BigramModel model, List<String> words) {
        List<String> suffixWords = new ArrayList<>();
        for (String word : words) {
            if (model.getWordCount(word) < MAX_WORD_FREQUENCY) {
                suffixWords.add(word);
            }
        }

        List<String> tags = model.getTags();
        Map<String, Integer> suffixTagCount = new HashMap<>();
        for (String tag : tags) {
            for (String word : suffixWords) {
                Integer count = model.getWordTagCount(tag, word) + suffixTagCount.getOrDefault(tag, 0);
                suffixTagCount.put(tag, count);
            }
        }

        List<String> suffixTags = new ArrayList<>(suffixTagCount.keySet());
        Integer totalTags = 0;
        for (String tag : suffixTags) {
            totalTags += suffixTagCount.get(tag);
        }

        SuffixTree tree = new SuffixTree();
        for (String word : suffixWords) {
            List<String> wordTags = model.getTagsForWord(word);
            for (String tag : wordTags) {
                Integer suffixLength = Math.min(MAX_SUFFIX_LENGTH, word.length());
                tree.addSuffix(word.substring(word.length() - suffixLength), tag);
            }
        }

        Double theta = calculateTheta(suffixTags, suffixTagCount, totalTags);
        tree.setTheta(theta);

        return tree;
    }

    private Double calculateTheta(List<String> suffixTags, Map<String, Integer> suffixTagCount, final Integer totalTagCount) {
        List<Double> tagProbs = suffixTags.stream().map(tag -> suffixTagCount.get(tag) / (double) totalTagCount).collect(Collectors.toList());
        Double avg = tagProbs.stream().collect(Collectors.summingDouble(Double::doubleValue)) / (double) suffixTags.size();
        List<Double> squaredDiff = tagProbs.stream().map(prob -> Math.pow(prob - avg, 2)).collect(Collectors.toList());

        return squaredDiff.stream().collect(Collectors.summingDouble(Double::doubleValue)) / (double) (suffixTags.size() - 1);
    }

}
