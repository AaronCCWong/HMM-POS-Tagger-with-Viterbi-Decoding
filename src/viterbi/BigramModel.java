package viterbi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BigramModel {

    private Integer sentenceCount;
    private Integer totalTagCount;
    private Map<String, Integer> tagCount;
    private Map<String, Integer> tagStartCount;
    private Map<String, Map<String, Integer>> tagTransitionCount;
    private Map<String, Map<String, Integer>> tagWordCount;
    private Map<String, Integer> wordCount;
    private Map<String, Map<String, Integer>> wordTagCount;

    public BigramModel() {
        sentenceCount = 0;
        totalTagCount = 0;

        tagCount = new HashMap<>();
        tagStartCount = new HashMap<>();
        tagTransitionCount = new HashMap<>();
        tagWordCount = new HashMap<>();
        wordCount = new HashMap<>();
        wordTagCount = new HashMap<>();
    }

    public List<String> getWords(boolean upperCase) {
        List<String> words = new ArrayList<>(wordCount.keySet());

        if (upperCase) { // strings with first character capitalized
            words = words.stream().filter(word -> Character.isUpperCase(word.charAt(0))).collect(Collectors.toList());
        } else { // everything else
            words = words.stream().filter(word -> !Character.isUpperCase(word.charAt(0))).collect(Collectors.toList());
        }

        return words;
    }

    public Integer getWordCount(String word) {
        return wordCount.getOrDefault(word, 0);
    }

    public void incrementWordCount(String word) {
        Integer count = wordCount.getOrDefault(word, 0) + 1;
        wordCount.put(word, count);
    }

    public Integer getTagStartCount(String tag) {
        return tagStartCount.getOrDefault(tag, 0);
    }

    public List<String> getTagsForWord(String word) {
        return new ArrayList<>(wordTagCount.getOrDefault(word, new HashMap<>()).keySet());
    }

    public void incrementTagStartCount(String tag) {
        Integer count = tagStartCount.getOrDefault(tag, 0) + 1;
        tagStartCount.put(tag, count);
        sentenceCount++;
    }

    public List<String> getTags() {
        List<String> tags = new ArrayList<>(tagCount.keySet());

        return tags;
    }

    public Integer getTagCount(String tag) {
        return tagCount.getOrDefault(tag, 0);
    }

    public void incrementTagCount(String tag) {
        Integer count = tagCount.getOrDefault(tag, 0) + 1;
        tagCount.put(tag, count);
        totalTagCount++;
    }

    public Integer getTagWordCount(String tag, String word) {
        return tagWordCount.getOrDefault(tag, new HashMap<>()).getOrDefault(word, 0);
    }

    public Integer getWordTagCount(String tag, String word) {
        return wordTagCount.getOrDefault(word, new HashMap<>()).getOrDefault(tag, 0);
    }

    public void incrementTagWordCount(String tag, String word) {
        if (!tagWordCount.containsKey(tag)) {
            tagWordCount.put(tag, new HashMap<>());
        }

        Integer tWCount = tagWordCount.get(tag).getOrDefault(word, 0) + 1;
        tagWordCount.get(tag).put(word, tWCount);

        if (!wordTagCount.containsKey(word)) {
            wordTagCount.put(word, new HashMap<>());
        }

        Integer wTCount = wordTagCount.get(word).getOrDefault(tag, 0) + 1;
        wordTagCount.get(word).put(tag, wTCount);
    }

    public Integer getTagTransitionCount(String fromTag, String toTag) {
        return tagTransitionCount.getOrDefault(fromTag, new HashMap<>()).getOrDefault(toTag, 0);
    }

    public void incrementTagTansitionCount(String fromTag, String toTag) {
        if (!tagTransitionCount.containsKey(fromTag)) {
            tagTransitionCount.put(fromTag, new HashMap<>());
        }

        Integer count = tagTransitionCount.get(fromTag).getOrDefault(toTag, 0) + 1;
        tagTransitionCount.get(fromTag).put(toTag, count);
    }

    public Double getEmissionProbability(String tag, String word) {
        Integer tagAndWordCount = tagWordCount.get(tag).getOrDefault(word, 0);
        Integer tagOccurences = tagCount.get(tag);

        return tagAndWordCount / (double) tagOccurences;
    }

    public Double getTransitionProbability(String fromTag, String toTag) {
        Integer tagToTagCount = tagTransitionCount.get(fromTag).getOrDefault(toTag, 0);
        Integer tagOccurences = tagCount.get(fromTag);

        return tagToTagCount / (double) tagOccurences;
    }

    public Double getStartProbability(String tag) {
        Integer startCount = tagStartCount.getOrDefault(tag, 0);

        return startCount / (double) sentenceCount;
    }

}
