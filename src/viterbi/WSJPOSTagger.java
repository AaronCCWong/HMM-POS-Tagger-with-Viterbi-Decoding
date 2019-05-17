package viterbi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class WSJPOSTagger {

    private static Integer MAX_SUFFIX_LENGTH;
    private static Integer MAX_WORD_FREQUENCY;

    public static void main(String[] args) throws FileNotFoundException, IOException {
        if (args.length != 4) {
            String errMsg = "Expected [TRAINING_FILENAME] [TEST_WORDS_FILENAME] [MAX_SUFFIX_LENGTH] [MAX_WORD_FREQUENCY], got " + args.length + " args.";
            System.err.println(errMsg);
            System.exit(99);
        }

        String trainFilename = args[0];
        File trainFile = new File(trainFilename);
        BigramModel bigramModel = train(trainFile);

        MAX_SUFFIX_LENGTH = Integer.parseInt(args[2]);
        MAX_WORD_FREQUENCY = Integer.parseInt(args[3]);

        System.out.println("Using a maximum suffix length of " + MAX_SUFFIX_LENGTH);
        System.out.println("Using words with a maximum frequency of " + MAX_WORD_FREQUENCY + " to create suffix tree");

        SuffixTreeBuilder treeBuilder = new SuffixTreeBuilder(MAX_SUFFIX_LENGTH, MAX_WORD_FREQUENCY);
        SuffixTree upperCaseTree = treeBuilder.buildUpperCaseTree(bigramModel);
        SuffixTree lowerCaseTree = treeBuilder.buildLowerCaseTree(bigramModel);

        System.out.println("Training HMM model...");

        String testFilename = args[1];
        File testFile = new File(testFilename);
        String[] filenameParts = testFilename.split("/");
        String[] filenameAndExt = filenameParts[filenameParts.length - 1].split("\\.");
        String filename = filenameAndExt[0];
        String outputFilename = filename + ".pos";

        System.out.println("Finished training.");
        System.out.println("Evaluating...");

        evaluate(bigramModel, upperCaseTree, lowerCaseTree, testFile, outputFilename);

        System.out.println("Check the base directory for the output file.");
    }

    public static BigramModel train(File file) throws FileNotFoundException {
        BigramModel bigramModel = new BigramModel();
        Scanner sc = new Scanner(file);
        String prevTag = "";

        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.isEmpty()) {
                prevTag = "";
                continue;
            }

            String[] wordTag = line.split("\t");
            String word = wordTag[0];
            String tag = wordTag[1];

            bigramModel.incrementWordCount(word);
            bigramModel.incrementTagCount(tag);
            bigramModel.incrementTagWordCount(tag, word);
            if (prevTag != null && !prevTag.isEmpty()) {
                bigramModel.incrementTagTansitionCount(prevTag, tag);
            } else if (prevTag.isEmpty()) {
                bigramModel.incrementTagStartCount(tag);
            }
            prevTag = tag;
        }

        sc.close();

        return bigramModel;
    }

    public static void evaluate(BigramModel model, SuffixTree upperCaseTree, SuffixTree lowerCaseTree, File words, String outputFilename) throws FileNotFoundException, IOException {
        Scanner sc = new Scanner(words);
        List<List<String>> sentenceTags = new ArrayList<>();
        List<List<String>> sentences = new ArrayList<>();

        List<String> currentSentence = new ArrayList<>();
        Integer i = 0;
        while (sc.hasNextLine()) {
            String word = sc.nextLine();

            if (word.isEmpty()) {
                sentenceTags.add(viterbi(model, upperCaseTree, lowerCaseTree, currentSentence));
                sentences.add(currentSentence);
                currentSentence = new ArrayList<>();
            } else {
                currentSentence.add(word);
            }

            i++;
        }

        if (currentSentence.size() > 0) {
            sentenceTags.add(viterbi(model, upperCaseTree, lowerCaseTree, currentSentence));
            sentences.add(currentSentence);
        }

        sc.close();
        generateOutputFile(outputFilename, sentences, sentenceTags);
    }

    public static List<String> viterbi(BigramModel model, SuffixTree upperCaseTree, SuffixTree lowerCaseTree, List<String> sentence) {
        List<String> tags = model.getTags();
        Integer numTags = tags.size();
        Integer sentenceLength = sentence.size();
        Matrix<Double> ppMatrix = new Matrix<Double>(numTags, sentenceLength);
        Matrix<Integer> backpointer = new Matrix<Integer>(numTags, sentenceLength);

        for (int state = 0; state < numTags; state++) {
            Integer timeStep = 0;
            Double prob = model.getStartProbability(tags.get(state));
            String word = sentence.get(timeStep);
            Double emissionProb = getEmissionProbability(model, upperCaseTree, lowerCaseTree, tags, state, word);
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
                        Double transitionProb =  model.getTransitionProbability(tags.get(prevState), tags.get(state));
                        Double prevProb = ppMatrix.get(prevState, timeStep - 1);
                        if (maxProb < transitionProb * prevProb) {
                            maxProb = transitionProb * prevProb;
                            maxPrevState = prevState;
                        }
                    }
                }

                Double emissionProb = getEmissionProbability(model, upperCaseTree, lowerCaseTree, tags, state, word);
                ppMatrix.set(state, timeStep, maxProb * emissionProb);
                backpointer.set(state, timeStep, maxPrevState);
            }
        }

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

        List<String> taggedWords = new ArrayList<>();
        for (int timeStep = 0; timeStep < sentenceLength; timeStep++) {
            taggedWords.add(tags.get(bestPath[timeStep]));
        }

        return taggedWords;
    }

    public static void generateOutputFile(String filename, List<List<String>> sentences, List<List<String>> sentenceTags) throws IOException {
        FileWriter writer = new FileWriter(filename);

        Integer numSentences = sentences.size();
        for (int i = 0; i < numSentences; i++) {
            Integer sentenceLength = sentences.get(i).size();
            for (int j = 0; j < sentenceLength; j++) {
                writer.write(sentences.get(i).get(j) + "\t" + sentenceTags.get(i).get(j) + "\n");
            }
            writer.write("\n");
        }

        writer.close();
    }

    public static Double getEmissionProbability(BigramModel model, SuffixTree upperCaseTree, SuffixTree lowerCaseTree, List<String> tags, Integer state, String word) {
        if (model.getWordCount(word) > 0) {
            return model.getEmissionProbability(tags.get(state), word);
        }

        Map<Integer, Double> stateProbs = getSuffixTagStats(upperCaseTree, lowerCaseTree, tags, word);
        return stateProbs.get(state);
    }

    public static Map<Integer, Double> getSuffixTagStats(SuffixTree upperCaseTree, SuffixTree lowerCaseTree, List<String> tags, String word) {
        Integer numTags = tags.size();

        Map<Integer, Double> stateProbs = new HashMap<>();
        for (int state = 0; state < numTags; state++) {
            String tag = tags.get(state);
            Integer suffixLength = Math.min(MAX_SUFFIX_LENGTH, word.length());
            String suffix = word.substring(word.length() - suffixLength);

            Double tagSuffixProb = 0.0;
            Double suffixProb = 0.0;
            Double tagProb = 0.0;
            if (Character.isUpperCase(word.charAt(0))) {
                while (!upperCaseTree.hasSuffix(suffix) && suffix.length() > 0) {
                    suffix = suffix.substring(1);
                }
                tagSuffixProb = upperCaseTree.getTagSuffixProbability(suffix, tag);
                suffixProb = upperCaseTree.getSuffixProbability(suffix);
                tagProb = upperCaseTree.getTagProbability(suffix, tag);
            } else {
                while (!lowerCaseTree.hasSuffix(suffix) && suffix.length() > 0) {
                    suffix = suffix.substring(1);
                }
                tagSuffixProb = lowerCaseTree.getTagSuffixProbability(suffix, tag);
                suffixProb = lowerCaseTree.getSuffixProbability(suffix);
                tagProb = lowerCaseTree.getTagProbability(suffix, tag);
            }

            Double probWordIsTag = 0.0; // handles case when tag never occurs in the suffix tree
            if (tagProb > 0.0) {
                probWordIsTag = tagSuffixProb * suffixProb / tagProb;
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
