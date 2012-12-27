 grep "KSS: 19" typo-4-7095.log | wc -l
 grep "KSS: 18" typo-4-7095.log | wc -l
 grep "KSS: 17" typo-4-7095.log | wc -l
 grep "KSS: 16" typo-4-7095.log | wc -l
 grep "KSS: 15" typo-4-7095.log | wc -l
 grep "KSS: 14" typo-4-7095.log | wc -l
 grep "KSS: 13" typo-4-7095.log | wc -l
 grep "KSS: 12" typo-4-7095.log | wc -l
 grep "KSS: 11" typo-4-7095.log | wc -l
 grep "KSS: 10" typo-4-7095.log | wc -l
 grep "KSS: 9" typo-4-7095.log | wc -l
 grep "KSS: 8" typo-4-7095.log | wc -l
 grep "KSS: 7" typo-4-7095.log | wc -l
 grep "KSS: 6" typo-4-7095.log | wc -l
 grep "KSS: 5" typo-4-7095.log | wc -l
 grep "KSS: 4" typo-4-7095.log | wc -l
 grep "KSS: 3" typo-4-7095.log | wc -l
 grep "KSS: 2" typo-4-7095.log | wc -l
 grep "KSS: 1" typo-4-7095.log | wc -l


#grep "KSS" typo-4-7095.log | grep "PREFIXLENGHT: 3"

echo "no matches:"
grep "NOTHING" typo-4-7095.log | wc -l
echo "matches:"
grep "HIT" typo-4-7095.log | wc -l

echo "matches on rank 1 any prefix"
grep "HIT" typo-4-7095.log | grep "RANK: 1" | wc -l

echo "matches on rank 2 any prefix"
grep "HIT" typo-4-7095.log | grep "RANK: 2" | wc -l

echo "matches on rank 3 any prefix"
grep "HIT" typo-4-7095.log | grep "RANK: 3" | wc -l

echo "matches on rank 4 any prefix"
grep "HIT" typo-4-7095.log | grep "RANK: 4" | wc -l

echo "matches on rank 5 any prefix"
grep "HIT" typo-4-7095.log | grep "RANK: 5" | wc -l


echo "total number of hits with prefix 0:"
HIT=`grep "HIT" typo-4-7095.log | grep "PREFIXLENGHT: 0" | wc -l`
echo $HIT
echo "total number of NO HITS with prefix 0:"
NO=`grep "NOTHING" typo-4-7095.log | grep "PREFIXLENGTH: 0" | wc -l`
echo $NO
SUM=$((HIT + NO))
HIT=$((HIT * 1000))
frac=$((HIT / SUM))
echo $frac "Promille hits"

echo "matches with on rank 1 and prefix 0:"
grep "HIT" typo-4-7095.log | grep "RANK: 1" | grep "PREFIXLENGHT: 0" | wc -l

echo "matches with on rank 2 and prefix 0:"
grep "HIT" typo-4-7095.log | grep "RANK: 2" | grep "PREFIXLENGHT: 0" | wc -l

echo "matches with on rank 3 and prefix 0:"
grep "HIT" typo-4-7095.log | grep "RANK: 3" | grep "PREFIXLENGHT: 0" | wc -l

echo "matches with on rank 4 and prefix 0:"
grep "HIT" typo-4-7095.log | grep "RANK: 4" | grep "PREFIXLENGHT: 0" | wc -l

echo "matches with on rank 5 and prefix 0:"
grep "HIT" typo-4-7095.log | grep "RANK: 5" | grep "PREFIXLENGHT: 0" | wc -l




echo "total number of hits with prefix 1:"
grep "HIT" typo-4-7095.log | grep "PREFIXLENGHT: 1" | wc -l
echo "total number of NO HITS with prefix 1:"
grep "NOTHING" typo-4-7095.log | grep "PREFIXLENGTH: 1" | wc -l


echo "matches with on rank 1 and prefix 1:"
grep "HIT" typo-4-7095.log | grep "RANK: 1" | grep "PREFIXLENGHT: 1" | wc -l

echo "matches with on rank 2 and prefix 1:"
grep "HIT" typo-4-7095.log | grep "RANK: 2" | grep "PREFIXLENGHT: 1" | wc -l

echo "matches with on rank 3 and prefix 1:"
grep "HIT" typo-4-7095.log | grep "RANK: 3" | grep "PREFIXLENGHT: 1" | wc -l

echo "matches with on rank 4 and prefix 1:"
grep "HIT" typo-4-7095.log | grep "RANK: 4" | grep "PREFIXLENGHT: 1" | wc -l

echo "matches with on rank 5 and prefix 1:"
grep "HIT" typo-4-7095.log | grep "RANK: 5" | grep "PREFIXLENGHT: 1" | wc -l





echo "total number of hits with prefix 2:"
grep "HIT" typo-4-7095.log | grep "PREFIXLENGHT: 2" | wc -l
echo "total number of NO HITS with prefix 2:"
grep "NOTHING" typo-4-7095.log | grep "PREFIXLENGTH: 2" | wc -l


echo "matches with on rank 1 and prefix 2:"
grep "HIT" typo-4-7095.log | grep "RANK: 1" | grep "PREFIXLENGHT: 2" | wc -l

echo "matches with on rank 2 and prefix 2:"
grep "HIT" typo-4-7095.log | grep "RANK: 2" | grep "PREFIXLENGHT: 2" | wc -l

echo "matches with on rank 3 and prefix 2:"
grep "HIT" typo-4-7095.log | grep "RANK: 3" | grep "PREFIXLENGHT: 2" | wc -l

echo "matches with on rank 4 and prefix 2:"
grep "HIT" typo-4-7095.log | grep "RANK: 4" | grep "PREFIXLENGHT: 2" | wc -l

echo "matches with on rank 5 and prefix 2:"
grep "HIT" typo-4-7095.log | grep "RANK: 5" | grep "PREFIXLENGHT: 2" | wc -l




echo "total number of hits with prefix 3:"
grep "HIT" typo-4-7095.log | grep "PREFIXLENGHT: 3" | wc -l
echo "total number of NO HITS with prefix 3:"
grep "NOTHING" typo-4-7095.log | grep "PREFIXLENGTH: 3" | wc -l


echo "matches with on rank 1 and prefix 3:"
grep "HIT" typo-4-7095.log | grep "RANK: 1" | grep "PREFIXLENGHT: 3" | wc -l

echo "matches with on rank 2 and prefix 3:"
grep "HIT" typo-4-7095.log | grep "RANK: 2" | grep "PREFIXLENGHT: 3" | wc -l

echo "matches with on rank 3 and prefix 3:"
grep "HIT" typo-4-7095.log | grep "RANK: 3" | grep "PREFIXLENGHT: 3" | wc -l

echo "matches with on rank 4 and prefix 3:"
grep "HIT" typo-4-7095.log | grep "RANK: 4" | grep "PREFIXLENGHT: 3" | wc -l

echo "matches with on rank 5 and prefix 3:"
grep "HIT" typo-4-7095.log | grep "RANK: 5" | grep "PREFIXLENGHT: 3" | wc -l



echo "total number of hits with prefix 0:"
HIT=`grep "HIT" typo-4-7095.log | grep "PREFIXLENGHT: 0" | wc -l`
echo $HIT
echo "total number of NO HITS with prefix 0:"
NO=`grep "NOTHING" typo-4-7095.log | grep "PREFIXLENGTH: 0" | wc -l`
echo $NO
SUM=$((HIT + NO))
HIT=$((HIT * 1000))
frac=$((HIT / SUM))
echo $frac "Promille hits"

echo "total number of hits with prefix 1:"
HIT=`grep "HIT" typo-4-7095.log | grep "PREFIXLENGHT: 1" | wc -l`
echo $HIT
echo "total number of NO HITS with prefix 1:"
NO=`grep "NOTHING" typo-4-7095.log | grep "PREFIXLENGTH: 1" | wc -l`
echo $NO
SUM=$((HIT + NO))
HIT=$((HIT * 1000))
frac=$((HIT / SUM))
echo $frac "Promille hits"

echo "total number of hits with prefix 2:"
HIT=`grep "HIT" typo-4-7095.log | grep "PREFIXLENGHT: 2" | wc -l`
echo $HIT
echo "total number of NO HITS with prefix 2:"
NO=`grep "NOTHING" typo-4-7095.log | grep "PREFIXLENGTH: 2" | wc -l`
echo $NO
SUM=$((HIT + NO))
HIT=$((HIT * 1000))
frac=$((HIT / SUM))
echo $frac "Promille hits"

echo "total number of hits with prefix 3:"
HIT=`grep "HIT" typo-4-7095.log | grep "PREFIXLENGHT: 3" | wc -l`
echo $HIT
echo "total number of NO HITS with prefix 3:"
NO=`grep "NOTHING" typo-4-7095.log | grep "PREFIXLENGTH: 3" | wc -l`
echo $NO
SUM=$((HIT + NO))
HIT=$((HIT * 1000))
frac=$((HIT / SUM))
echo $frac "Promille hits"
