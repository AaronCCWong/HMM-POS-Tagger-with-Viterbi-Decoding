package viterbi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Viterbi {

    Integer MAX_SUFFIX_LENGTH;

    BigramModel model;
    SuffixTree upperCaseTree;
    SuffixTree lowerCaseTree;
    List<String> tags;
    Integer numTags;

    public Viterbi(BigramModel bigramModel, SuffixTree upperCaseSuffixTree, SuffixTree lowerCaseSuffixTree, Integer maxSuffixLength) {
        model = bigramModel;
        upperCaseTree = upperCaseSuffixTree;
        lowerCaseTree = lowerCaseSuffixTree;
        MAX_SUFFIX_LENGTH = maxSuffixLength;

        tags = model.getTags();
        numTags = tags.size();
    }

    public List<String> run(List<String> sentence) {
        Integer sentenceLength = sentence.size();
        Matrix<Double> ppMatrix = new Matrix<Double>(numTags, sentenceLength);
        Matrix<Integer> backpointer = new Matrix<Integer>(numTags, sentenceLength);

        for (int state = 0; state < numTags; state++) {
            Integer timeStep = 0;
            Double prob = model.getStartProbability(tags.get(state));
            String word = sentence.get(timeStep);
            Double emissionProb = getEmissionProbability(state, word);
            ppMatrix.set(state, timeStep, prob * emissionProb);
            backpointer.set(state, timeStep, -1);
        }

        for (int timeStep = 1; timeStep < sentenceLength; timeStep++) {
            String word = sentence.get(timeStep);
            for (int state = 0; state < numTags; state++) {
                Double maxProb = 0.0;
                Integer maxPrevState = 0;
                for (int prevState = 0; prevState < numTags; prevState++) {
                    if (model.getTagTransitionCount(tags.get(prevState), tags.get(state)) > 0) {
                        Double transitionProb = model.getTransitionProbability(tags.get(prevState), tags.get(state));
                        Double prevProb = ppMatrix.get(prevState, timeStep - 1);
                        if (maxProb < transitionProb * prevProb) {
                            maxProb = transitionProb * prevProb;
                            maxPrevState = prevState;
                        }
                    }
                }

                Double emissionProb = getEmissionProbability(state, word);
                ppMatrix.set(state, timeStep, maxProb * emissionProb);
                backpointer.set(state, timeStep, maxPrevState);
            }
        }

        return getWordTags(ppMatrix, backpointer, sentenceLength);
    }

    private List<String> getWordTags(Matrix<Double> ppMatrix, Matrix<Integer> backpointer, Integer sentenceLength) {
        Integer bestPathPointer = 0;
        for (int state = 1; state < numTags; state++) {
            Integer timeStep = sentenceLength - 1;
            if (ppMatrix.get(state, timeStep) > ppMatrix.get(bestPathPointer, timeStep)) {
                bestPathPointer = state;
            }
        }

        int[] bestPath = new int[sentenceLength];
        bestPath[sentenceLength - 1] = bestPathPointer;
        for (int timeStep = sentenceLength - 2; timeStep >= 0; timeStep--) {
            Integer nextTimeStep = timeStep + 1;
            bestPath[timeStep] = backpointer.get(bestPath[nextTimeStep], nextTimeStep);
        }

        List<String> wordTags = new ArrayList<>();
        for (int timeStep = 0; timeStep < sentenceLength; timeStep++) {
            wordTags.add(tags.get(bestPath[timeStep]));
        }

        return wordTags;
    }

    private Double getEmissionProbability(Integer state, String word) {
        if (model.getWordCount(word) > 0) {
            return model.getEmissionProbability(tags.get(state), word);
        }

        Map<Integer, Double> stateProbs = getSuffixStats(word);
        return stateProbs.get(state);
    }

    private Map<Integer, Double> getSuffixStats(String word) {
        Integer numTags = tags.size();

        Map<Integer, Double> stateProbs = new HashMap<>();
        for (int state = 0; state < numTags; state++) {
            String tag = tags.get(state);
            Integer suffixLength = Math.min(MAX_SUFFIX_LENGTH, word.length());
            String suffix = word.substring(word.length() - suffixLength);

            SuffixStats stats;
            if (Character.isUpperCase(word.charAt(0))) {
                stats = new SuffixStats(upperCaseTree, suffix, tag);
            } else {
                stats = new SuffixStats(lowerCaseTree, suffix, tag);
            }

            Double probWordIsTag = 0.0; // handles case when tag never occurs in the suffix tree
            if (stats.tagProb > 0.0) {
                probWordIsTag = stats.tagSuffixProb * stats.suffixProb / stats.tagProb;
            }

            stateProbs.put(state, probWordIsTag);
        }

        Map.Entry<Integer, Double> maxEntry = null;

        for (Map.Entry<Integer, Double> entry : stateProbs.entrySet()) {
            if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
                maxEntry = entry;
            }
        }

        return stateProbs;
    }

}
