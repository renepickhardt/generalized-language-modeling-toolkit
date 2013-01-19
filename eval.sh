#AUTOR: Rene Pickhardt
#need to fix the bug at KSS that KSS: 1 also findes KSS: 10
#Purpose to aggregate data from logfiles
#run with ./eval.sh logFileName
LOGFILE=$1

#prefix length
PFL=5
#precision at k up to PAK
PAK=5
#maximal number of ranks that ar included in MRR every result ranking worse is set to 0
MRRMAXRANK=10

for LOGFILE in "$@"
do
NUMQUERIES=`grep "MATCH:" $LOGFILE | wc -l`

#reselt res. file
echo -n "" | tee "res.$LOGFILE"

#### key stroke savings at k
echo "key stroke savings at k"
SUM=0

#change this if PAK!=5
#X for accessing arrays with less operations (-1...)
PARAMETERS= ( X 0 0 0 0 0 )
RESULT= ( X 0 0 0 0 0 )
#read lines and store values into PARAMETERS
(while read -r line
do 
#store results into RESULTS
	if [[ "$line" == *"MATCH"* ]]
	then
		for((  atk = 1 ;  atk <= PAK;  atk++  ))
		do
			RESULT[$atk]=$((RESULT[$atk]+PARAMETERS[$atk]))
			#echo "RESULT[$atk]:${RESULT[$atk]}"
		done
		PARAMETERS= ( X 0 0 0 0 0 0 )
	fi
	for((  atk = 1 ;  atk <= PAK;  atk++  ))
	do
		if [[ "$line" == "KSS AT $atk: "* ]]
		then
			PARAMETERS[$atk]=${line##*: }
			#echo "PARAMETERS[$atk]:${PARAMETERS[$atk]}"
		fi
	done
done

#store last results
for((  atk = 1 ;  atk <= PAK;  atk++  ))
do
	RESULT[${atk}]=$((RESULT[${atk}]+PARAMETERS[${atk}]))
	#echo "RESULT[${atk}]:${RESULT[${atk}]}"
done

#print results
echo "total queries: $NUMQUERIES"
for((  atk = 1 ;  atk <= PAK;  atk++  ))
do
	AVGKSS=`echo ${RESULT[$atk]} / $NUMQUERIES | bc -l`
	echo "KSS at k=$atk: $AVGKSS" | tee -a "res.$LOGFILE"
done
) < $LOGFILE


#### normalized key stroke savings at k
echo "normalized key stroke savings at k"
SUM=0

#change this if PAK!=5
#X for accessing arrays with less operations (-1...)
PARAMETERS= ( X 0 0 0 0 0 )
RESULT= ( X 0 0 0 0 0 )
#read lines and store values into PARAMETERS
(while read -r line
do 
#store results into RESULTS
	if [[ "$line" == *"MATCH"* ]]
	then
		for(( atk = 1 ;  atk <= PAK;  atk++ ))
		do
			RESULT[$atk]=`echo ${RESULT[$atk]} + ${PARAMETERS[$atk]} | bc -l`
			#echo "RESULT[$atk]:${RESULT[$atk]}"
		done
		PARAMETERS= ( X 0 0 0 0 0 )
	fi
	for((  atk = 1 ;  atk <= PAK;  atk++  ))
	do
		if [[ "$line" == "NKSS AT $atk: "* ]]
		then
			PARAMETERS[$atk]=${line##*: }
			#echo "PARAMETERS[$atk]:${PARAMETERS[$atk]}"
		fi
	done
done

#store last results
for((  atk = 1 ;  atk <= PAK;  atk++  ))
do
	RESULT[$atk]=`echo ${RESULT[$atk]} + ${PARAMETERS[$atk]} | bc -l`
	#echo "RESULT[$atk]:${RESULT[$atk]}"
done

#print results
echo "total queries: $NUMQUERIES"
for((  atk = 1 ;  atk <= PAK;  atk++  ))
do
	AVGKSS=`echo ${RESULT[$atk]} / $NUMQUERIES | bc -l`
	echo "NKSS at k=$atk: $AVGKSS" | tee -a "res.$LOGFILE"
done
) < $LOGFILE

#### MRR
echo "TESTS for MRR with PFL = $PFL and RANKS up to $MRRMAXRANK"
for((  i = 0 ;  i <= PFL;  i++  ))
do
NUMQUERIES=`grep "PREFIXLENGTH: $i " $LOGFILE | wc -l`
#echo "$NUMQUERIES queries of with prefix of length $i"
HIT=0
for((  j = 1 ;  j <= MRRMAXRANK;  j++  ))
do
CNT=`grep "PREFIXLENGTH: $i " $LOGFILE | grep "RANK: $j" | wc -l`
WRR=`echo "$CNT / $j " | bc -l`
HIT=`echo "$HIT + $WRR " | bc -l`
#HIT=$((HIT + WRR))
done
MRR=`echo "$HIT / $NUMQUERIES " | bc -l`
echo "MRR with pfl=$i: $MRR" | tee -a "res.$LOGFILE"
done

#### Precision @ k for every prefix length
echo "precision @ k for every prefix with PFL = $PFL and k up to: $PAK"
for((  i = 0 ;  i <= PFL;  i++  ))
do
NUMQUERIES=`grep "PREFIXLENGTH: $i " $LOGFILE | wc -l`
echo "$NUMQUERIES queries of with prefix of length $i"
HIT=0
for((  j = 1 ;  j <= PAK;  j++  ))
do
RES=`grep "PREFIXLENGTH: $i " $LOGFILE | grep "RANK: $j" | wc -l`
HIT=$((RES + HIT))
precisionAtK=`echo "$HIT / $NUMQUERIES " | bc -l`
echo "Precision at k=$j with pfl=$i: $precisionAtK" | tee -a "res.$LOGFILE"
done
done

done

