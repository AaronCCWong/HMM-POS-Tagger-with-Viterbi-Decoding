# Viterbi POS Tagger

Tested using Java 11.

To run from the command line, make sure you are in the root directory `viterbi`. This is the directory that contains `src` and `WSJ_POS_CORPUS_FOR_STUDENTS`.
Run

```bash
javac src/viterbi/*.java
```

Then to train and evaluate

```bash
java -cp src viterbi.WSJPOSTagger WSJ_POS_CORPUS_FOR_STUDENTS/WSJ_02-21.pos TEST_FILE MAX_SUFFIX_LENGTH MAX_WORD_FREQUENCY
```

where `TEST_FILE` is the file with sentences that you want to tag, `MAX_SUFFIX_LENGTH` is the maximum suffix length to use for the suffix tree and
`MAX_WORD_FREQUENCY` is the maximum word frequency as found in the training set of the words to use for the suffix tree.