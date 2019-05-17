package viterbi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        Viterbi viterbi = new Viterbi(model, upperCaseTree, lowerCaseTree, MAX_SUFFIX_LENGTH);

        List<String> currentSentence = new ArrayList<>();
        while (sc.hasNextLine()) {
            String word = sc.nextLine();

            if (word.isEmpty()) {
                sentenceTags.add(viterbi.run(currentSentence));
                sentences.add(currentSentence);
                currentSentence = new ArrayList<>();
            } else {
                currentSentence.add(word);
            }
        }

        if (currentSentence.size() > 0) {
            sentenceTags.add(viterbi.run(currentSentence));
            sentences.add(currentSentence);
        }

        sc.close();
        generateOutputFile(outputFilename, sentences, sentenceTags);
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

}
