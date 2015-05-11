# Components

The requirements are separated by the toolkit components.
Each component is briefly introduced to put the requirements into context.

Assumptions are marked as _italic_, discussion is marked as **bold**, questions are in comment boxes.

## Toolkit core

### Corpus statistics

The toolkit should be able to print out statistics of the corpus.
Corpus statistics could include

* total number of tokens
* 10 most frequent tokens
* number and percentage of uni-/bi-/n-grams seen

The statistics should be hold in a object with a `toString` method and the  toolkit could provide a `printStatistics` method for convencience.

## Language Model
Language models assign a probability to a sequence of words.

There are unigram models, n-gram models, factored models and others.
The toolkit supports a various range of language models.

## Estimator
Estimators offer to calculate probabilities of a sequence of words.
Not all estimators are wrapped into a language model, _that has more functionality_.

**Estimator sounds like internal usage, I would suggest** `LangModel`.

### Comparable
Estimators should be comparable to others. This includes the well-established entropy as well as application use cases, e.g. to compare them to other language models that are incorrect (not a probability function) where entropy isn't compatible.

* tests that check for properties of a probability function (e.g. sums to 1)
* **any ideas for an application use case evaluation?**
* **doesn't the results depend on the corpus and we would need to reduce this to few use cases for both evaluation types?**

### Custom estimators
Users can implement their own estimators, in order to create a new language model.

* slim interface or
* abstract class to reduce overrides and provide ``super`` calls

### Avoid misapplication
When using an estimator, it should be very hard for users to make common miscalculations, e.g. due to assumptions like

* P("w1 w2") == P("w2" | "w1") <=> c("_ _") == c("w1 _")
* or c("w1") == c("w1 _")

**We need a document that explains the differences, to point at in the JavaDoc.**

These problem arise from the low problem awareness, that is also a result of the complex notation.
We need to point out these differences in the API.

This is a major problem when a user wants to create a new language model and uses the `count` methods falsely.

* API should clearly separate the notations, e.g. by using different endpoints, to increase the problem awareness

## Unknown component

### Smoothing

> Is this language model, estimator or toolkit specific?

## Use cases
The new API has to fulfill the above requirements and has to be handy according to some use cases. 

### 1. Calculate frequency of a n-gram
A common use case is to calculate the frequency `c("w1 w2")` of a n-gram.

### 2. Calculate probability of a n-gram
Another common use case is to calculate the probability `p("w1 w2") = c("w1 w2") / c("_ _")` of a n-gram.

### 3. Calculate probability of a sequence
A user has to be able to calculate the conditional probability `p("w2" | "w1") = c("w1 w2") / c("w1 _")` of a sequence.

### 4. Use a different estimator
A user wants to use a different estimator for his calculations.

### 5. Statistics
A user wants to get and/or print statistics of the corpus.
See [the statistics requirement](#corpus-statistics) above.

### 6. Implement own (correct) estimators
Users want to implement own estimators that are valid, in order to compare its entropy.
In case an estimator isn't valid, a comparison via application use cases should still be possible.

### 7. Predict the next word in a sequence
A user wants to be able to predict the next **words**, given a sequence.

> Isn't the probability of the next word important? And do we want to limit this prediction to a single word?

### 8. Automated caching mechanisms
The toolkit should use caching mechanisms (in-memory structures and persistent analysis data) automatically, according to the system resources available.

### 9. Change caching behavior
In case of very small or very large corpses, a user wants to have finer control above caching and persistence mechanisms.
For example, a user may want to disable any persistence processes for large corpses with few queries.

### 10. Set smoothing
A user wants to enable smoothing of unseen words/sequences.

>This has an effect on the statistic?

## API use case examples

    GlmToolkit t = new GlmToolkit("/tmp/corpus.txt");
    LangModel m = t.getModel();
    
    // change estimator [use case #4]
    m = t.getModel(LangModel.GLM);
    m = new MyModel(t); // or maybe a more symmetric variant
    
    // change caching behavior [use case #9]
    m.setCaching(CachingMode.NONE);
    
    // set smoothing [use case #10]
    m.setSmoothing(true);
    
    // get statistics [use case #5]
    CorpusStatistics s = t.getStatistics();
    LOG.info("statistics", s);
    
    /**
     * API Version 1: (updated) current API
     */
    // calculate n-gram frequency [use case #1]
    c = m.count("america the brave"); // c(ngram)
    
    // calculate n-gram probability [use case #2]
    p = m.probability("america the brave"); // p(ngram)
    
    // calculate conditional sequence probability [use case #3]
    pSequence = m.probability("brave", "america the"); // probability(ngram, history)
    
    /**
     * API Version 2: chaining
     */
    c = m.count("america the brave");
    p = m.probability("america the brave");
    pSequence = m.withHistory("america the").probability("brave"); m.withHistory(history).probability([ngram])
    
    /**
     * API Version 3: different endpoints
     */
    c = m.count("america the brave");
    p = m.probability("america the brave");
    pSequence = m.condProbability("america the", "brave"); // m.condProbability(history, ngram)
    
    /**
     * common to all API versions
     */
    // predict next words [use case #7]
    List<Word> nextWords = m.nextWords("america the", 10);
    sysout("Top 10 next words are " + List.toString(nextWords));
    Word nextWord = m.nextWord("america the");
    sysout("Next word is " + nextWord + " with a chance of " + nextWord.getProbability() + ".");

## open questions / topics
* Estimators are used to implement smoothing methods
* how to handle unknown words (in testing)?
* what about the top-k join API (it would be really great if this part of the code base becomes reusable)
* it should be possible to handle argmax queries like: what is the most probable word given the following history
* I am pretty sure the api for conditional probabilities should look like `m.condProbability(ngram, history)` instead of `m.condProbability(history, ngram)`
* what about ARPA format?
* what about general backoff methods, and general interpolated methods (averaging / combining language model API)
 
