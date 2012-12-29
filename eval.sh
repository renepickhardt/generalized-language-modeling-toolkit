#AUTOR: Rene Pickhardt
#Purpose to aggregate data from logfiles
#run with ./eval.sh logFileName
LOGFILE=$1

#prefix length
PFL=5
#precision at k up to PAK
PAK=6
#maximal number of ranks that ar included in MRR every result ranking worse is set to 0
MRRMAXRANK=10

for LOGFILE in "$@"
do
NUMQUERIES=`grep "MATCH:" $LOGFILE | wc -l`

#### calculate and output average key stroke savings
echo "Calculate and output the average key stroke savings"
SUM=0
for((  i = 1 ;  i <= 20;  i++  ))
do
KSS=`grep "KSS: $i" $LOGFILE | wc -l`
echo "$i keystrokes saved $KSS times"
MUL=$((KSS * i))
SUM=$((SUM + MUL))
done
AVGKSS=`echo "$SUM / $NUMQUERIES " | bc -l`
echo "Average Key stroke savings: $AVGKSS " | tee "res.$LOGFILE"

#### Precision @ k for every prefix length
echo "precision @ k for every prefix with PFL = $PFL and k up to: $PAK"
for((  i = 0 ;  i <= PFL;  i++  ))
do
NUMQUERIES=`grep "PREFIXLENGTH: $i" $LOGFILE | wc -l`
echo "$NUMQUERIES queries of with prefix of length $i"
HIT=0
for((  j = 1 ;  j <= PAK;  j++  ))
do
RES=`grep "PREFIXLENGTH: $i" $LOGFILE | grep "RANK: $j" | wc -l`
HIT=$((RES + HIT))
precisionAtK=`echo "$HIT / $NUMQUERIES " | bc -l`
echo "   Precision at k = $j: $precisionAtK PFL = $i" | tee -a "res.$LOGFILE"
done
done

#### MRR
echo "TESTS for MRR with PFL = $PFL and RANKS up to $MRRMAXRANK"
for((  i = 0 ;  i <= PFL;  i++  ))
do
NUMQUERIES=`grep "PREFIXLENGTH: $i" $LOGFILE | wc -l`
#echo "$NUMQUERIES queries of with prefix of length $i"
HIT=0
for((  j = 1 ;  j <= MRRMAXRANK;  j++  ))
do
CNT=`grep "PREFIXLENGTH: $i" $LOGFILE | grep "RANK: $j" | wc -l`
WRR=`echo "$CNT / $j " | bc -l`
HIT=`echo "$HIT + $WRR " | bc -l`
#HIT=$((HIT + WRR))
done
MRR=`echo "$HIT / $NUMQUERIES " | bc -l`
echo "   MRR = $MRR: PFL = $i" | tee -a "res.$LOGFILE"
done

done

