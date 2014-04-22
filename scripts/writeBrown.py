#!/usr/bin/env python

from nltk.corpus import brown

# Plain
f = open('brownPlain.txt', 'w')
for sentence in brown.sents():
    first = True
    for word in sentence:
        if first:
            first = False
        else:
            f.write(' ')
        f.write(word)
    f.write('\n')
f.close()

# Tagged
f = open('brownTagged.txt', 'w')
for sentence in brown.tagged_sents():
    first = True
    for (word,tag) in sentence:
        if first:
            first = False
        else:
            f.write(' ')
        f.write('%s/%s' % (word,tag))
    f.write('\n')
f.close()
