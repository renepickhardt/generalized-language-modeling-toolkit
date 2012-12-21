 grep "KSS: 19" Complet.log | wc -l
 grep "KSS: 18" Complet.log | wc -l
 grep "KSS: 17" Complet.log | wc -l
 grep "KSS: 16" Complet.log | wc -l
 grep "KSS: 15" Complet.log | wc -l
 grep "KSS: 14" Complet.log | wc -l
 grep "KSS: 13" Complet.log | wc -l
 grep "KSS: 12" Complet.log | wc -l
 grep "KSS: 11" Complet.log | wc -l
 grep "KSS: 10" Complet.log | wc -l
 grep "KSS: 9" Complet.log | wc -l
 grep "KSS: 8" Complet.log | wc -l
 grep "KSS: 7" Complet.log | wc -l
 grep "KSS: 6" Complet.log | wc -l
 grep "KSS: 5" Complet.log | wc -l
 grep "KSS: 4" Complet.log | wc -l
 grep "KSS: 3" Complet.log | wc -l
 grep "KSS: 2" Complet.log | wc -l
 grep "KSS: 1" Complet.log | wc -l


#grep "KSS" Complet.log | grep "PREFIXLENGHT: 3"

echo "no matches:"
grep "NOTHING" Complet.log | wc -l
echo "matches:"
grep "HIT" Complet.log | wc -l

echo "matches on rank 1 any prefix"
grep "HIT" Complet.log | grep "RANK: 1" | wc -l

echo "matches on rank 2 any prefix"
grep "HIT" Complet.log | grep "RANK: 2" | wc -l

echo "matches on rank 3 any prefix"
grep "HIT" Complet.log | grep "RANK: 3" | wc -l

echo "matches on rank 4 any prefix"
grep "HIT" Complet.log | grep "RANK: 4" | wc -l

echo "matches on rank 5 any prefix"
grep "HIT" Complet.log | grep "RANK: 5" | wc -l


echo "total number of hits with prefix 0:"
HIT=`grep "HIT" Complet.log | grep "PREFIXLENGHT: 0" | wc -l`
echo $HIT
echo "total number of NO HITS with prefix 0:"
NO=`grep "NOTHING" Complet.log | grep "PREFIXLENGTH: 0" | wc -l`
echo $NO
SUM=$((HIT + NO))
HIT=$((HIT * 1000))
frac=$((HIT / SUM))
echo $frac "Promille hits"

echo "matches with on rank 1 and prefix 0:"
grep "HIT" Complet.log | grep "RANK: 1" | grep "PREFIXLENGHT: 0" | wc -l

echo "matches with on rank 2 and prefix 0:"
grep "HIT" Complet.log | grep "RANK: 2" | grep "PREFIXLENGHT: 0" | wc -l

echo "matches with on rank 3 and prefix 0:"
grep "HIT" Complet.log | grep "RANK: 3" | grep "PREFIXLENGHT: 0" | wc -l

echo "matches with on rank 4 and prefix 0:"
grep "HIT" Complet.log | grep "RANK: 4" | grep "PREFIXLENGHT: 0" | wc -l

echo "matches with on rank 5 and prefix 0:"
grep "HIT" Complet.log | grep "RANK: 5" | grep "PREFIXLENGHT: 0" | wc -l




echo "total number of hits with prefix 1:"
grep "HIT" Complet.log | grep "PREFIXLENGHT: 1" | wc -l
echo "total number of NO HITS with prefix 1:"
grep "NOTHING" Complet.log | grep "PREFIXLENGTH: 1" | wc -l


echo "matches with on rank 1 and prefix 1:"
grep "HIT" Complet.log | grep "RANK: 1" | grep "PREFIXLENGHT: 1" | wc -l

echo "matches with on rank 2 and prefix 1:"
grep "HIT" Complet.log | grep "RANK: 2" | grep "PREFIXLENGHT: 1" | wc -l

echo "matches with on rank 3 and prefix 1:"
grep "HIT" Complet.log | grep "RANK: 3" | grep "PREFIXLENGHT: 1" | wc -l

echo "matches with on rank 4 and prefix 1:"
grep "HIT" Complet.log | grep "RANK: 4" | grep "PREFIXLENGHT: 1" | wc -l

echo "matches with on rank 5 and prefix 1:"
grep "HIT" Complet.log | grep "RANK: 5" | grep "PREFIXLENGHT: 1" | wc -l





echo "total number of hits with prefix 2:"
grep "HIT" Complet.log | grep "PREFIXLENGHT: 2" | wc -l
echo "total number of NO HITS with prefix 2:"
grep "NOTHING" Complet.log | grep "PREFIXLENGTH: 2" | wc -l


echo "matches with on rank 1 and prefix 2:"
grep "HIT" Complet.log | grep "RANK: 1" | grep "PREFIXLENGHT: 2" | wc -l

echo "matches with on rank 2 and prefix 2:"
grep "HIT" Complet.log | grep "RANK: 2" | grep "PREFIXLENGHT: 2" | wc -l

echo "matches with on rank 3 and prefix 2:"
grep "HIT" Complet.log | grep "RANK: 3" | grep "PREFIXLENGHT: 2" | wc -l

echo "matches with on rank 4 and prefix 2:"
grep "HIT" Complet.log | grep "RANK: 4" | grep "PREFIXLENGHT: 2" | wc -l

echo "matches with on rank 5 and prefix 2:"
grep "HIT" Complet.log | grep "RANK: 5" | grep "PREFIXLENGHT: 2" | wc -l




echo "total number of hits with prefix 3:"
grep "HIT" Complet.log | grep "PREFIXLENGHT: 3" | wc -l
echo "total number of NO HITS with prefix 3:"
grep "NOTHING" Complet.log | grep "PREFIXLENGTH: 3" | wc -l


echo "matches with on rank 1 and prefix 3:"
grep "HIT" Complet.log | grep "RANK: 1" | grep "PREFIXLENGHT: 3" | wc -l

echo "matches with on rank 2 and prefix 3:"
grep "HIT" Complet.log | grep "RANK: 2" | grep "PREFIXLENGHT: 3" | wc -l

echo "matches with on rank 3 and prefix 3:"
grep "HIT" Complet.log | grep "RANK: 3" | grep "PREFIXLENGHT: 3" | wc -l

echo "matches with on rank 4 and prefix 3:"
grep "HIT" Complet.log | grep "RANK: 4" | grep "PREFIXLENGHT: 3" | wc -l

echo "matches with on rank 5 and prefix 3:"
grep "HIT" Complet.log | grep "RANK: 5" | grep "PREFIXLENGHT: 3" | wc -l



echo "total number of hits with prefix 0:"
HIT=`grep "HIT" Complet.log | grep "PREFIXLENGHT: 0" | wc -l`
echo $HIT
echo "total number of NO HITS with prefix 0:"
NO=`grep "NOTHING" Complet.log | grep "PREFIXLENGTH: 0" | wc -l`
echo $NO
SUM=$((HIT + NO))
HIT=$((HIT * 1000))
frac=$((HIT / SUM))
echo $frac "Promille hits"

echo "total number of hits with prefix 1:"
HIT=`grep "HIT" Complet.log | grep "PREFIXLENGHT: 1" | wc -l`
echo $HIT
echo "total number of NO HITS with prefix 1:"
NO=`grep "NOTHING" Complet.log | grep "PREFIXLENGTH: 1" | wc -l`
echo $NO
SUM=$((HIT + NO))
HIT=$((HIT * 1000))
frac=$((HIT / SUM))
echo $frac "Promille hits"

echo "total number of hits with prefix 2:"
HIT=`grep "HIT" Complet.log | grep "PREFIXLENGHT: 2" | wc -l`
echo $HIT
echo "total number of NO HITS with prefix 2:"
NO=`grep "NOTHING" Complet.log | grep "PREFIXLENGTH: 2" | wc -l`
echo $NO
SUM=$((HIT + NO))
HIT=$((HIT * 1000))
frac=$((HIT / SUM))
echo $frac "Promille hits"

echo "total number of hits with prefix 3:"
HIT=`grep "HIT" Complet.log | grep "PREFIXLENGHT: 3" | wc -l`
echo $HIT
echo "total number of NO HITS with prefix 3:"
NO=`grep "NOTHING" Complet.log | grep "PREFIXLENGTH: 3" | wc -l`
echo $NO
SUM=$((HIT + NO))
HIT=$((HIT * 1000))
frac=$((HIT / SUM))
echo $frac "Promille hits"
