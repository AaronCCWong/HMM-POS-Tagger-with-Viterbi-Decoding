package viterbi;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SuffixTree {

    private Integer suffixCount;
    private Integer totalCount;
    private Integer totalTagCount;
    private Double theta;
    private Map<String, SuffixTree> nodes;
    private Map<String, Integer> tagCount;
    private Map<String, Integer> tagSuffixCount;

    public SuffixTree() {
        suffixCount = 0;
        totalCount = 0;
        totalTagCount = 0;
        nodes = new HashMap<>();
        tagCount = new HashMap<>();
        tagSuffixCount = new HashMap<>();
    }

    public Boolean hasSuffix(String suffix) {
        SuffixTree pointer = getSubtree(suffix);

        return pointer.getCount() > 0;
    }

    public Integer getTotalSuffixCount() {
        return totalCount;
    }

    public void setTheta(Double theta) {
        this.theta = theta;
    }

    public Integer getCount() {
        return suffixCount;
    }

    public void incrementCount() {
        suffixCount++;
    }

    public Integer getTagCount(String tag) {
        return tagCount.getOrDefault(tag, 0);
    }

    public void incrementTagCount(String tag) {
        Integer tCount = tagCount.getOrDefault(tag, 0) + 1;
        tagCount.put(tag, tCount);
    }

    public Integer getTagSuffixCount(String tag) {
        return tagSuffixCount.getOrDefault(tag, 0);
    }

    public void incrementTagSuffixCount(String tag) {
        Integer tsCount = tagSuffixCount.getOrDefault(tag, 0) + 1;
        tagSuffixCount.put(tag, tsCount);
    }

    public SuffixTree get(String letter) {
        return nodes.get(letter);
    }

    public void put(String key, SuffixTree val) {
        nodes.put(key, val);
    }

    public void addSuffix(String suffix, String tag) {
        incrementTagCount(tag);
        for (int i = 0; i < suffix.length(); i++) {
            SuffixTree pointer = getSubtree(suffix.substring(i));

            pointer.incrementTagSuffixCount(tag);
            pointer.incrementCount();
            totalCount++;
            totalTagCount++;
        }
    }

    public boolean containsKey(String key) {
        return nodes.containsKey(key);
    }

    public Double getTagSuffixProbability(String suffix, String tag) {
        Stack<Double> mles = new Stack<>();
        for (int i = 0; i < suffix.length(); i++) {
            SuffixTree pointer = getSubtree(suffix.substring(i));
            Integer suffixCount = pointer.getCount();
            Integer tagSuffixCount = pointer.getTagSuffixCount(tag);

            Double mle = tagSuffixCount / (double) suffixCount;
            mles.push(mle);
        }

        Double probability = 0.0;
        while (!mles.empty()) {
            probability = (mles.pop() + (theta * probability)) / (1 + theta);
        }

        return probability;
    }

    public Double getSuffixProbability(String suffix) {
        SuffixTree pointer = getSubtree(suffix);
        Integer suffixCount = pointer.getCount();

        return suffixCount / (double) totalCount;
    }

    public Double getTagProbability(String suffix, String tag) {
        Integer tagCount = getTagCount(tag);

        return tagCount / (double) totalTagCount;
    }

    private SuffixTree getSubtree(String suffix) {
        SuffixTree pointer = this;
        for (int i = 0; i < suffix.length(); i++) {
            String letter = suffix.substring(i);
            if (!pointer.containsKey(letter)) {
                pointer.put(letter, new SuffixTree());
            }
            pointer = pointer.get(letter);
        }

        return pointer;
    }

}
