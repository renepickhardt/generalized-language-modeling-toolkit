#AUTOR: Rene Pickhardt
#Purpose to aggregate data from logfiles
#run with ./eval.sh logFileName

LOGFILE=$1
NUMQUERIES=`grep "MATCH:" $LOGFILE | wc -l`

#### calculate and output average key stroke savings
SUM=0
for((  i = 1 ;  i <= 20;  i++  ))
do
KSS=`grep "KSS: $i" $LOGFILE | wc -l`
echo "$i keystrokes saved $KSS times"
MUL=$((KSS * i))
SUM=$((SUM + MUL))
done
AVGKSS=`echo "$SUM / $NUMQUERIES " | bc -l`
echo "Average Key stroke savings: $AVGKSS "

#### Precision @ k for every prefix length
for((  i = 0 ;  i <= 10;  i++  ))
do
NUMQUERIES=`grep "PREFIXLENGTH: $i" $LOGFILE | wc -l`
echo "$NUMQUERIES queries of with prefix of length $i"
HIT=0
for((  j = 1 ;  j <= 10;  j++  ))
do
RES=`grep "PREFIXLENGTH: $i" $LOGFILE | grep "RANK: $j" | wc -l`
HIT=$((RES + HIT))
precisionAtK=`echo "$HIT / $NUMQUERIES " | bc -l`
echo "   Precision at k= $j: $precisionAtK"
done
done

#### MRR

for((  i = 0 ;  i <= 10;  i++  ))
do
NUMQUERIES=`grep "PREFIXLENGTH: $i" $LOGFILE | wc -l`
echo "$NUMQUERIES queries of with prefix of length $i"
HIT=0
for((  j = 1 ;  j <= 10;  j++  ))
do
CNT=`grep "PREFIXLENGTH: $i" $LOGFILE | grep "RANK: $j" | wc -l`
WRR=`echo "$CNT / $j " | bc -l`
HIT=`echo "$HIT + $WRR " | bc -l`
#HIT=$((HIT + WRR))
done
MRR=`echo "$HIT / $NUMQUERIES " | bc -l`
echo "   MRR = $MRR:"
done
