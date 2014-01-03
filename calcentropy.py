"""                                                                                                                                                    
author: rene pickhardt                                                                                                                                 
                                                                                                                                                       
give a list of files as arguments. The files contain test sequences                                                                                    
with probabilities according to a trained language model                                                                                               
                                                                                                                                                       
this code ist GPLv3                                                                                                                                    
"""
from math import log
import sys

def calc(arg):
    f=open(arg,"r")
    res = 0
    zero = 0;
    wc = 0;
    for l in f:
	fl = float(l.split("\t")[1])
	if (fl==0):
            zero = zero + 1
            continue
	res=res + log(fl,2)
	wc = wc + len(l.split(" "))
    print arg + "\t entropy: " +  str((res*-1)/wc) + "\tsequences with zeros: " + str(zero)



for arg in sys.argv:
    if (arg==sys.argv[0]):
	continue
    calc(arg)
