 grep "KSS: 19" Complete.log | wc -l
 grep "KSS: 18" Complete.log | wc -l
 grep "KSS: 17" Complete.log | wc -l
 grep "KSS: 16" Complete.log | wc -l
 grep "KSS: 15" Complete.log | wc -l
 grep "KSS: 14" Complete.log | wc -l
 grep "KSS: 13" Complete.log | wc -l
 grep "KSS: 12" Complete.log | wc -l
 grep "KSS: 11" Complete.log | wc -l
 grep "KSS: 10" Complete.log | wc -l
 grep "KSS: 9" Complete.log | wc -l
 grep "KSS: 8" Complete.log | wc -l
 grep "KSS: 7" Complete.log | wc -l
 grep "KSS: 6" Complete.log | wc -l
 grep "KSS: 5" Complete.log | wc -l
 grep "KSS: 4" Complete.log | wc -l
 grep "KSS: 3" Complete.log | wc -l
 grep "KSS: 2" Complete.log | wc -l
 grep "KSS: 1" Complete.log | wc -l


#grep "KSS" Complete.log | grep "PREFIXLENGHT: 3"

echo "no matches:"
grep "NOTHING" Complete.log | wc -l
echo "matches:"
grep "HIT" Complete.log | wc -l

echo "matches on rank 1 any prefix"
grep "HIT" Complete.log | grep "RANK: 1" | wc -l

echo "matches on rank 2 any prefix"
grep "HIT" Complete.log | grep "RANK: 2" | wc -l

echo "matches on rank 3 any prefix"
grep "HIT" Complete.log | grep "RANK: 3" | wc -l

echo "matches on rank 4 any prefix"
grep "HIT" Complete.log | grep "RANK: 4" | wc -l

echo "matches on rank 5 any prefix"
grep "HIT" Complete.log | grep "RANK: 5" | wc -l


echo "total number of hits with prefix 0:"
HIT=`grep "HIT" Complete.log | grep "PREFIXLENGHT: 0" | wc -l`
echo $HIT
echo "total number of NO HITS with prefix 0:"
NO=`grep "NOTHING" Complete.log | grep "PREFIXLENGTH: 0" | wc -l`
echo $NO
SUM=$((HIT + NO))
HIT=$((HIT * 1000))
frac=$((HIT / SUM))
echo $frac "Promille hits"

echo "matches with on rank 1 and prefix 0:"
grep "HIT" Complete.log | grep "RANK: 1" | grep "PREFIXLENGHT: 0" | wc -l

echo "matches with on rank 2 and prefix 0:"
grep "HIT" Complete.log | grep "RANK: 2" | grep "PREFIXLENGHT: 0" | wc -l

echo "matches with on rank 3 and prefix 0:"
grep "HIT" Complete.log | grep "RANK: 3" | grep "PREFIXLENGHT: 0" | wc -l

echo "matches with on rank 4 and prefix 0:"
grep "HIT" Complete.log | grep "RANK: 4" | grep "PREFIXLENGHT: 0" | wc -l

echo "matches with on rank 5 and prefix 0:"
grep "HIT" Complete.log | grep "RANK: 5" | grep "PREFIXLENGHT: 0" | wc -l




echo "total number of hits with prefix 1:"
grep "HIT" Complete.log | grep "PREFIXLENGHT: 1" | wc -l
echo "total number of NO HITS with prefix 1:"
grep "NOTHING" Complete.log | grep "PREFIXLENGTH: 1" | wc -l


echo "matches with on rank 1 and prefix 1:"
grep "HIT" Complete.log | grep "RANK: 1" | grep "PREFIXLENGHT: 1" | wc -l

echo "matches with on rank 2 and prefix 1:"
grep "HIT" Complete.log | grep "RANK: 2" | grep "PREFIXLENGHT: 1" | wc -l

echo "matches with on rank 3 and prefix 1:"
grep "HIT" Complete.log | grep "RANK: 3" | grep "PREFIXLENGHT: 1" | wc -l

echo "matches with on rank 4 and prefix 1:"
grep "HIT" Complete.log | grep "RANK: 4" | grep "PREFIXLENGHT: 1" | wc -l

echo "matches with on rank 5 and prefix 1:"
grep "HIT" Complete.log | grep "RANK: 5" | grep "PREFIXLENGHT: 1" | wc -l





echo "total number of hits with prefix 2:"
grep "HIT" Complete.log | grep "PREFIXLENGHT: 2" | wc -l
echo "total number of NO HITS with prefix 2:"
grep "NOTHING" Complete.log | grep "PREFIXLENGTH: 2" | wc -l


echo "matches with on rank 1 and prefix 2:"
grep "HIT" Complete.log | grep "RANK: 1" | grep "PREFIXLENGHT: 2" | wc -l

echo "matches with on rank 2 and prefix 2:"
grep "HIT" Complete.log | grep "RANK: 2" | grep "PREFIXLENGHT: 2" | wc -l

echo "matches with on rank 3 and prefix 2:"
grep "HIT" Complete.log | grep "RANK: 3" | grep "PREFIXLENGHT: 2" | wc -l

echo "matches with on rank 4 and prefix 2:"
grep "HIT" Complete.log | grep "RANK: 4" | grep "PREFIXLENGHT: 2" | wc -l

echo "matches with on rank 5 and prefix 2:"
grep "HIT" Complete.log | grep "RANK: 5" | grep "PREFIXLENGHT: 2" | wc -l




echo "total number of hits with prefix 3:"
grep "HIT" Complete.log | grep "PREFIXLENGHT: 3" | wc -l
echo "total number of NO HITS with prefix 3:"
grep "NOTHING" Complete.log | grep "PREFIXLENGTH: 3" | wc -l


echo "matches with on rank 1 and prefix 3:"
grep "HIT" Complete.log | grep "RANK: 1" | grep "PREFIXLENGHT: 3" | wc -l

echo "matches with on rank 2 and prefix 3:"
grep "HIT" Complete.log | grep "RANK: 2" | grep "PREFIXLENGHT: 3" | wc -l

echo "matches with on rank 3 and prefix 3:"
grep "HIT" Complete.log | grep "RANK: 3" | grep "PREFIXLENGHT: 3" | wc -l

echo "matches with on rank 4 and prefix 3:"
grep "HIT" Complete.log | grep "RANK: 4" | grep "PREFIXLENGHT: 3" | wc -l

echo "matches with on rank 5 and prefix 3:"
grep "HIT" Complete.log | grep "RANK: 5" | grep "PREFIXLENGHT: 3" | wc -l



echo "total number of hits with prefix 0:"
HIT=`grep "HIT" Complete.log | grep "PREFIXLENGHT: 0" | wc -l`
echo $HIT
echo "total number of NO HITS with prefix 0:"
NO=`grep "NOTHING" Complete.log | grep "PREFIXLENGTH: 0" | wc -l`
echo $NO
SUM=$((HIT + NO))
HIT=$((HIT * 1000))
frac=$((HIT / SUM))
echo $frac "Promille hits"

echo "total number of hits with prefix 1:"
HIT=`grep "HIT" Complete.log | grep "PREFIXLENGHT: 1" | wc -l`
echo $HIT
echo "total number of NO HITS with prefix 1:"
NO=`grep "NOTHING" Complete.log | grep "PREFIXLENGTH: 1" | wc -l`
echo $NO
SUM=$((HIT + NO))
HIT=$((HIT * 1000))
frac=$((HIT / SUM))
echo $frac "Promille hits"

echo "total number of hits with prefix 2:"
HIT=`grep "HIT" Complete.log | grep "PREFIXLENGHT: 2" | wc -l`
echo $HIT
echo "total number of NO HITS with prefix 2:"
NO=`grep "NOTHING" Complete.log | grep "PREFIXLENGTH: 2" | wc -l`
echo $NO
SUM=$((HIT + NO))
HIT=$((HIT * 1000))
frac=$((HIT / SUM))
echo $frac "Promille hits"

echo "total number of hits with prefix 3:"
HIT=`grep "HIT" Complete.log | grep "PREFIXLENGHT: 3" | wc -l`
echo $HIT
echo "total number of NO HITS with prefix 3:"
NO=`grep "NOTHING" Complete.log | grep "PREFIXLENGTH: 3" | wc -l`
echo $NO
SUM=$((HIT + NO))
HIT=$((HIT * 1000))
frac=$((HIT / SUM))
echo $frac "Promille hits"
