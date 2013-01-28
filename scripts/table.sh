#AUTOR: Martin Koerner
#Purpose to put results into a LaTeX table

#run with: pfl=x k=x modelParameter=x ./table.sh

#e.g.:
#pfl=1 k=5 weight=no modelParameter=5 ./table.sh


#storage directory for res.*.log files
#LOGDIR="/var/lib/datasets/results/"
LOGDIR="/home/martin/results/"
#storage directory for return files
#RETURNDIR="/var/lib/datasets/plots/"
RETURNDIR="/home/martin/plots/"
STATS="stats.txt"
SECNKSS=1

if [[ ${#pfl} == 0 || ${#k} == 0 || ${#weight} == 0 || ${#modelParameter} == 0 ]]
	then echo "set values for k, pfl, modelParameter and weight"
	exit
fi

#temp:
#res.trainedOn-wiki-de-testedOn-wiki-de-lm-pic-modelParameter2-sam0-split95-joinlength10-nQ100000




#general declarations
PF1="res.trainedOn-"
PF2="-testedOn-"
PF3="-modelParameter"
PF4="-sam0-split95-joinlength10-nQ100000.log"
LANGS=(de en es fr it)

LM="-lm-"
TYPO="-typolgy-"
FILENAME="table-k$k-pfl$pfl-modelParameter$modelParameter.txt"
RETURN=$RETURNDIR$FILENAME

#reset result file
echo -n "" | tee "$RETURN"

echo "\begin{table*}[bth]" | tee -a "$RETURN"
echo "\begin{center}" | tee -a "$RETURN"
echo "\begin{tabular}{lllll}" | tee -a "$RETURN"
echo "Corpus & total words & unique words & MRR pfl=$pfl & Top $k Precision pfl=$pfl & NKSS@$SECNKSS & NKSS@$k \\\\" | tee -a "$RETURN"
echo "\hline" | tee -a "$RETURN"

CALC () {
#echo -n "( CNTTYPO / $CNTLM  - 1 ) * 100"

RESULT=`echo "($CNTTYPO/$CNTLM-1)*100" | bc -l`
echo -n " $RESULT" | awk '{ printf "%.1f", $0 }' | tee -a "$RETURN"
#echo -n " $RESULT" | tee -a "$RETURN"
}

PRINTLN () {
echo -n "$CORPUS & " | tee -a "$RETURN"
TOTALWORDS=`grep "$TYP1$LANG total" $STATS`
TOTALWORDS=${TOTALWORDS[0]//$TYP1$LANG total: /}
echo -n "$TOTALWORDS & " | tee -a "$RETURN"

UNIQUEWORDS=`grep "$TYP1$LANG unique" $STATS`
UNIQUEWORDS=${UNIQUEWORDS[0]//$TYP1$LANG unique: /}
echo -n "$UNIQUEWORDS & " | tee -a "$RETURN"

CNTTYPO=`grep "MRR with pfl=$pfl" $FILETYPO`
CNTTYPO=${CNTTYPO[0]//MRR with pfl=$pfl: /}
echo $CNTTYPO
CNTLM=`grep "MRR with pfl=$pfl" $FILELM`
CNTLM=${CNTLM[0]//MRR with pfl=$pfl: /}
echo $CNTLM
CALC
echo -n " & " | tee -a "$RETURN"
CNTTYPO=`grep "Precision at k=$k with pfl=$pfl" $FILETYPO`
CNTTYPO=${CNTTYPO[0]//Precision at k=$k with pfl=$pfl: /}
echo $CNTTYPO
CNTLM=`grep "Precision at k=$k with pfl=$pfl" $FILELM`
CNTLM=${CNTLM[0]//Precision at k=$k with pfl=$pfl: /}
echo $CNTLM
CALC
echo -n " & " | tee -a "$RETURN"


CNTTYPO=`grep "NKSS at k=$SECNKSS" $FILETYPO`
CNTTYPO=${CNTTYPO[0]//NKSS at k=$SECNKSS: /}
echo $CNTTYPO
CNTLM=`grep "NKSS at k=$SECNKSS" $FILELM`
CNTLM=${CNTLM[0]//NKSS at k=$SECNKSS: /}
echo $CNTLM
CALC
echo -n " & " | tee -a "$RETURN"

CNTTYPO=`grep "NKSS at k=$k" $FILETYPO`
CNTTYPO=${CNTTYPO[0]//NKSS at k=$k: /}
echo $CNTTYPO
CNTLM=`grep "NKSS at k=$k" $FILELM`
CNTLM=${CNTLM[0]//NKSS at k=$k: /}
echo $CNTLM
CALC

echo " \\\\" | tee -a "$RETURN"
}

#google
for LANG in ${LANGS[@]} 
do
if [[ $LANG != "it" ]]
then
TYP1="google-"
TYP2="wiki-"
CORPUS="$TYP1$TYP2$LANG"
FILETYPO=$LOGDIR$PF1$TYP1$LANG$PF2$TYP2$LANG$TYPO$weight$PF3$modelParameter$PF4
FILELM=$LOGDIR$PF1$TYP1$LANG$PF2$TYP2$LANG$LM$weight$PF3$modelParameter$PF4
echo $FILETYPO
echo $FILELM
PRINTLN
fi

#wiki

TYP1="wiki-"
TYP2="wiki-"
CORPUS="$TYP1$TYP2$LANG"
FILETYPO=$LOGDIR$PF1$TYP1$LANG$PF2$TYP2$LANG$TYPO$weight$PF3$modelParameter$PF4
FILELM=$LOGDIR$PF1$TYP1$LANG$PF2$TYP2$LANG$LM$weight$PF3$modelParameter$PF4
echo $FILETYPO
echo $FILELM
PRINTLN

#dgttm
TYP1="dgttm-"
TYP2="dgttm-"
CORPUS="$TYP1$TYP2$LANG"
FILETYPO=$LOGDIR$PF1$TYP1$LANG$PF2$TYP2$LANG$TYPO$weight$PF3$modelParameter$PF4
FILELM=$LOGDIR$PF1$TYP1$LANG$PF2$TYP2$LANG$LM$weight$PF3$modelParameter$PF4
echo $FILETYPO
echo $FILELM
PRINTLN


#enron
if [[ $LANG == "en" ]]
then
TYP1="enron-"
TYP2="enron-"
CORPUS="$TYP1$TYP2$LANG"
FILETYPO=$LOGDIR$PF1$TYP1$LANG$PF2$TYP2$LANG$TYPO$weight$PF3$modelParameter$PF4
FILELM=$LOGDIR$PF1$TYP1$LANG$PF2$TYP2$LANG$LM$weight$PF3$modelParameter$PF4
echo $FILETYPO
echo $FILELM
PRINTLN
fi

done

echo "\end{tabular}" | tee -a "$RETURN"
echo "\label{tab:corporaStats}" | tee -a "$RETURN"
echo "\caption{Statistics of our evaluation corpora}" | tee -a "$RETURN"
echo "\end{center}" | tee -a "$RETURN"
echo "\end{table*}" | tee -a "$RETURN"
