#Typology
[Typology](http://www.typology.de) is data structure for large amounts of text in order to suggest users next words they are about to type.

Typology was first developed by Paul Georg Wagner, Till Speicher and Rene Pickhardt. [More information](http://www.rene-pickhardt.de/tag/typology)

The code is available under an AGPL licence. This means that you can use this code free of charge as long as your built stuff that is also open source. If you want to be closed you will have to buy a commercial licence.

##Content
This project contains the following functions (follow the links for further information):

+ [Parse](src/de/typology/parser) the following datasets into plain text:
  + Wikipedia XML
  + JRC-Aquis XML
  + Reuters XML
  + Enron Mail
+ [Build](src/de/typology/splitter) different favors of language models
  + ngrams
  + typo edges
  + general language models (1, 10, 11, 101, 111, 1001, 1010, 1011, ...)
+ [Kneser-Ney smoothing](src/de/typology/smoother) (work in progress)
+ [Store](src/de/typology/scripts) language models in a MySQL database and build indices
+ [Evaluate](src/de/typology/evaluation) language models
+ [Aggregate evaluation results](src/de/typology/scripts)
+ [Plot aggregated evaluation results](src/de/typology/scripts)

##Execution
Information about the execution can be found [here](src/de/typology/executables).


