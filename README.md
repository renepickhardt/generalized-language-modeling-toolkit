#Generalized Language Model Toolkit

The software can be used to compute a Generalized Language Model which is yet another mean to compute a [Language Model](http://en.wikipedia.org/wiki/Language_model) 

## Getting started
```
git clone git@github.com:renepickhardt/generalized-language-modeling-toolkit.git
sudo chmod a+x mvn.sh
```
You will need to install maven in order to build the project.
```
sudo apt-get install maven2
```

You need to copy config.sample.txt to config.txt and reed the instructions in config.sample.txt.
```
cp config.sample.txt config.txt
emacs config.txt
```

After you set all your directories in config.txt you can run the project
```
./mvn.sh
```

## Disk and Main memory requirements
Since Generalized language models can become very large the software is written to use the hard disk. In this sense you can theoretically run the programm with very little memory. Still we recommend 16 GB of main memory for the large english wikipedia data sets. 

We tried to avoid frequent disc hits. Still the programm will execute much faster if you store your data on a Solid State disk. 

## Download the test data sets
Please refere to http://glm.rene-pickhardt.de/data in order to download preprocessed and formatted data sets. 

If you whish to parse the data yourself (e.g. because you want to use a newer wikipedia dump) refer to https://github.com/mkrnr/lexer-parser

## Citing the paper
If this software or data is of any help to your research please be so fair and cite the [original publication](http://glm.rene-pickhardt/paper). 
You might want to use the following bibtex entry
```
@inproceedings{Pickhardt:2014:GLM, 
   author = {Pickhardt, Rene and Gottron, Thomas and Körner, Martin and  Wagner, Paul Georg and  Speicher, Till and  Staab, Steffen}, 
   title = {A Generalized Language Model as the Combination of Skipped n-grams and Modified Kneser Ney Smoothing}, 
   year = {2014}, 
   booktitle = {ACL'14: Proceedings of the 52nd Annual Meeting of the Association for Computational Linguistics}, 
 } 
```

## History
The Generalized Language models envolved from Paul Georg Wagner's and Till Speicher's Young Scientists project called [Typology](http://www.typology.de) which I advised in 2012.
The Typology project played around and evaluated an idea I had (inspired by Adam Schenker) of presenting text as a graph in which the edges would encode relationships (nowerdays known as skipped bi-grams). The Graph was used to produce an answer to the next word prediction problem applied to word suggestions in keyboards of modern smartphones.
From the convincing results I developed the theory of Generalized Language models. 
Most of the Code was written by my student assistent [Martin Körner](http://mkoerner.de/) who also created his [bachlor thesis](bachlor-thesis-martin-koerner.pdf) about the implementation of a preliminary vesion of the Generalized Language Models. This thesis is a nice reference if you want to get an understanding of modified kneser ney smoothing for standard language models. In terms of notation and building of generalized language models it is outdated.
