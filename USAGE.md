# Setup

After cloning the repo compile all java files with `./build.sh`.

Then run `./install.sh` to create symlink in `/usr/local/bin` to the important shell scripts.

# Usage of GLMTK utilities

Then the following executables should be available:

*Flags not mentioned in this document are probably not interesting at the
moment.*

## `glmtk`

Used to retrieve occurence counts from a training corpus.

    glmtk training.txt -e MKN GLM -n 5

The first argument is the path to the training corpus. In the example that file
would be `training.txt`. Output is written to a new directory which is `.glmtk`
appended to the training file. So in the example output is written to directory
`training.txt.glmtk`.

The `-e` argument specifies the language models that we should learn occurence
counts for. Can take an arbitrary number of arguments. In the example this would
be Modified Kneser-Ney Smoothing and the Generalized Language Model.
There are more available see `glmtk --help`.
*Note that AFAIK this flag is redundant at the moment, and we always learn all
occurence counts, but let's better be on the safe side.*

The `-n` flag specifies the lenght of the longest n-gram the program will count.

## `glmtk-exp-setup`

Splits a given text file into training and testing datasets as described
in the "Experimental Setup" of Lukas' thesis.

    glmtk-exp-setup oanc.txt -p 0.8 -b 5 -n 5 -N 40000

The first argument is the path to the corpus that should be split into datasets.
In the example this would be `oanc.txt`. This should be a plain text file
containing sentences of words. *Note that this file has to be in the format you
later want to learn counts from, for example you might want to separate
punctuation from words or similiar things.*
*Note that scripts that prepare certain corpora for glmtk analysis are not yet
public, these scripts are pretty specific for each corpus and Lukas'
experimental setup. But the output files of them live in the VM.*

Output goes into a new directory which is `.expsetup` appended to the corpus
name. So in the example output is written to directory `oanc.txt.expsetup`.
Inside that directory a file named `corpus` is created which is a mirror of
the original input data.

*In the VM these datasets are kept at `/data/langmodels/datasets/`.*

The `-p` argument specifies the probability with which a sentences goes into
the training. In the example a random 80% of sentences go into the training
dataset. The other sentecences go into heldout. The training data lives
in file `training`, heldout data lives in file `heldout`.

The `-b` argument gives the number of times we want to split out training corpus
in half. For the example value of 5 we create 5 differently sized corpora will
be created, where each corpus is twice as large as the next. They are created
by keeping only each second, fourth, eight, ... sentence in the original
training. A bigger training set thus contains all sentences of a smaller one.
he corresponding output files are named `training-1`, `training-2`, ...
*Note that `training-1` is an exact copy of `training`.*
You probably want to learn occurence counts from exactly these files using
`glmtk`.

The `-n` argument specifies the maximum length of the testing n-grams. The
example values of `5` means that 1-,2-,3-,4-, and 5-gram testing sequences are
generated. Lower length testing sequences are derived form longer length ones
by removing words at the beginning.

The `-N` argument gives the number of testings sequences per n-gram length that
should be generated. In the example this are 40.000 testing sequences. Testing
sequences are randomly selected from the heldout dataset in a way that they only
contain words which also occur in the smallest training set. The file
`heldout.nounk` is the heldout data filtered for words that only occur in the
smallest training set. The actual testings sequences are in files named
`ngram-1`, `ngram-2`, `ngram-3`, ... which contain the corresponding n-grams.
*Note that in the VM there often times exist files like `ngram-3-5k` these are
hand created the first 5k sequences from the `ngram-3` file.*

## `glmtk-exp-argmaxcompare`

Will perform words prediction on testing files for given algorithms
and language models and track a variety of metrics.

    glmtk-exp-argmaxcompare training-1.glmtk -q ngram-4 ngram-5 \
        -d training-1-argmaxcompare -o training-1-argmaxcompare-output \
        -a SMPL TA NRA -e MKN GLM -k 5 -c

The first argument is the directory in which learned occurence counts are
stored that were learned using `glmtk`.

Word prediction is performed for the testing sequences files given to argument
`-q`. Multiple can be specified. The file is expected to contain newline
delilmited testing sequences. Testing sequences should consists
of multiple words. All but the last word are interpreted as the history for
which a word should be predicted, while the last word gives the expected word.

The `-d` option gives the directory to which the kept metrics will be written.
*In the VM that name of these output directories was
`<TRAINING-FILE>-argmaxcompare-<NUMBER-OF-TESTING-SEQUENCES>-c`.*
In this directory a number of files are created. You probably want to analyse
these with the `statistics-*` scripts. The format of these files are
space-delimited numbers. Exacly one number is written for each each testing
sequences. So the 4th number is source of performing word prediction on the 4th
line in the testing file. The name of these output files is
`<TESTING-FILE>-<ALGORITHM>-<LANGUAGE-MODEL>-<METRIC>`.

There are a number of metric files:
- Files ending in a number: `-1`, `-2`, ...:
  These track the time used per query to perform word prediction, while
  predicting as many words in the number.
- Files ending with `-numRandomAccesses` or `-numSortedAccesses`:
  These keep track of how many data structure accesses were performed during
  the calculation of each query. Note that NRA uses only Sorted Access.
  These metrics are not being tracked for the SMPL argmax.
- Files ending with `-nkss`:
  These give the normalized keystroke savings for each query. At the moment NKSS
  is calculated by looking if the expected word occurs as the top-1 prediction.
- Files ending with `-time-nkss`:
  These give the time used to calculate the NKSS of a query.

The `-o` option gives the directoy to which the results of each query are
written. *In the VM this output dir is named like the `-d` output dir but
with `-output at the end. These files are probably uninteresting. However you
should still specify that argument as the script sometimes doesn't like it when
you don't.* These files give the calcualted k prediction along with
probabilities for each top-k join query. For top-1 queries we additionaly query
with increasing prefixes. *The time necessary for these prefix queries is
tracked nowhere. But one is probably only interested in NKSS.*

The `-a` argument gives the algorithms which should be used for word prediction.
Can be specified multiple times. Possible values are `TA`, `NRA`, and `SMPL`.
*The help also lists `BEAM` but that is not implemented`.*

The `-e` argument gives the language models which should be used for word
prediction. Only `MKN` and `GLM` are possible.

The `-k` argument gives the number up to which k predictions should be
calculated for. So for example a value of 5 means that we perform word
prediction which gives 1, 2, 3, 4, 5 predictions.

The `-c` argument specifies that no argmax query cache should be created prior
to execution. *Always set this flag as I'm not sure if query caches work
without errors, and this one gives more flexibility.* All completion tries
that are loaded for word prediction are materialized to disk in the directory
`<TRAINING-FILE>.glmtk/counts/tries`. All these files are loaded into memory
for GLM but only some for MKN. *I have to look for the script I wrote which
lists all necessary tries of MKN.*

## `glmtk-exp-estimatortime`

Performs probability calculation of test sequences and keeps track of a variety
of metrics. *I will probably implement Entropy/Perplexity tracking here.*

    glmtk-exp-estimatortime training-1.glmtk -q ngram4 ngram-5
      -q ngram-4 ngram-5 -e MKN GLM

The first argument is the directory in which learned occurence counts are
stored that were learned using `glmtk`.

Probability calculation is performed for the testing sequences files given to
argument `-q`. Multiple can be specified. The file is expected to contain
newline delilmited testing sequences. Testing sequences should consists
of multiple words. Computed conditional probabilities with interpreting all
but the last word as the history and the last word as the argument (probability
event?).

The `-d` option gives the directory to which the kept metrics will be written.
*In the VM that name of these output directories was
`<TRAINING-FILE>-estimatortime`.*
In this directory a number of files are created. You probably want to analyse
these with the `statistics-*` scripts. The format of these files are
space-delimited numbers. Exacly one number is written for each each testing
sequences. So the 4th number is source of performing probability calculation on
the 4th line in the testing file. The name of these output files is
`<TESTING-FILE>-<LANGUAGE-MODEL>-<METRIC>`.

There are a number of metric files:
- Files without a special suffix:
  These track the time used per query to calculate the probability.
- Files ending in `-numWeights`:
  These keep the track of weights used for probability calculation per query.
- Files ending in `-timeRemaining` or `-timeWeights`:
  These track the time used to calculate the weights and the time reamining
  to calculate probabilities using weights.

The `-e` argument gives the lanugage models which hsould be used for
probability calculation. A wide variety are available, see `--help`.

# Usage of statistics utilities

## `statistics`

Will compute simple statistical characteristics for all given files.

Just go into a directory which only contains file which contain white-space
delimited numbers and call it with

statistics <file1> <file2> ...

Most of the time you will just do `statistics *`.

Print a pretty table with a bunch of statistics:
- `N`: The sample size
- `μ`: The mean
- `σ`: The standard deviation
- `σ²`: The variance
- `σ/μ`: The coefficient of variation
- `min`, `max`: The smallest/largest measured value

Optionally the first argument can be something like `-5`, so the total call
would be `statistics -5 *`. This removes 5% of the largest outliers in the
sample. For fairness we would throw away the smallest 2.5% of values and the
largest 2.5% of values. *You will probably note huge differences when using
this option.*

## `statistics-div`

Used to "divide one file value through another".

    statistics-div NRA:TA ngram-5-NRA-Weighted-Sum-Modified-Kneser-Ney-1

The "nominator file" is the given file. For the "denominator file" we search
for the expression left of the `:` in the given pattern and replace it in the
file by the expression on the right. Divides each value form the nominator file
through its corresponding value in the denominator file and writes all ratios
to an output file, which you can then analyse with `statistics`.

In the example `ngram-5-NRA-Weighted-Sum-Modified-Kneser-Ney-1` is divided
through `ngram-5-TA-Weighted-Sum-Modified-Kneser-Ney-1`, and output is written
to `div-ngram-5-NRA:TA-Weighted-Sum-Modified-Kneser-Ney-1`.

**For all following scripts you need to install `numpy` and `matplotlib`!**

## `statistics-boxplot`

Prints the robust boxplot statistics for a given file.
Also prints the TeX PGFplots code used to output this data as a boxplot.

## `statistics-histogram`

Creates a histogram of all values in a given file.

    statistics-histogram ngram-5-TA-Weighted-Sum-Modified-Kneser-Ney-1 100

The first argument is the file with the numbers. The second argument is the
numbers of bins in the histogram.

A optional third argument can be specified which removes that many outliers.
For example
`statistics-histogram ngram-5-TA-Weighted-Sum-Modified-Kneser-Ney-1 100 5` would
remove 5% of outliers like in `statistics`.

## `statistics-histogram-log`

Like the above but with a logarithmic x-axis.

## `statistics-histogram-div`

*I don't even know anymore.*
