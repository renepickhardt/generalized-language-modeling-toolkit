#AUTOR: Martin Koerner, Rene Pickhardt
#need to fix the bug at KSS that KSS: 1 also findes KSS: 10
#Purpose to aggregate data from logfiles
#run with ./eval.sh logFileName

#prefix length up to PFL
PFL=5
#precision at k up to PAK
PAK=6

#storage directory for res.*.log files
LOGDIR="/home/martin/logs/"
#storage directory for return files
RETURNDIR="/home/martin/shells/"

#modify parameters
if [[ ${#trainedOnDS} != 0 ]]
	then trainedOnDS="(${trainedOnDS//,/|})"
	else trainedOnDS=.*
fi

if [[ ${#trainedOnLang} != 0 ]]
	then trainedOnLang="(${trainedOnLang//,/|})"
	else trainedOnLang=.*
fi

if [[ ${#testedOnDS} != 0 ]]
	then testedOnDS="(${testedOnDS//,/|})"
	else testedOnDS=.*
fi

if [[ ${#testedOnLang} != 0 ]]
	then testedOnLang="(${testedOnLang//,/|})"
	else testedOnLang=.* 
fi

if [[ ${#typ} != 0 ]]
	then typ="(${typ//,/|})"
	else typ=.*
fi

if [[ ${#weighted} != 0 ]]
	then weighted="(${weighted//,/|})"
	else weighted=.* 
fi

if [[ ${#modelParameter} != 0 ]]
	then modelParameter="(${modelParameter//,/|})"
	else modelParameter=.*
fi

if [[ ${#sam} != 0 ]]
	then sam="(${sam//,/|})"
	else sam=.*
fi

if [[ ${#split} != 0 ]]
	then split="(${split//,/|})"
	else split=.*
fi

if [[ ${#joinlength} != 0 ]]
	then joinlength="(${joinlength//,/|})"
	else joinlength=.*
fi

if [[ ${#nQ} != 0 ]]
	then nQ="(${nQ//,/|})"
	else nQ=.* 
fi

#set return file name
NAME=trainedOn-$trainedOnDS-$trainedOnLang-testedOn-$testedOnDS-$testedOnLang-$typ-$weighted-modelParameter$modelParameter-sam$sam-split$split-joinlength$joinlength-nQ$nQ.log

#PAK using fixed pfl
PAKFPFL () {
PREFIX="pakfpfl$pfl."
RETURN=$RETURNDIR$PREFIX$NAME
#reset old output file
echo -n "" | tee $RETURN
echo "retrieves Precision with fixed pfl=$pfl"
for((  k = 1 ;  k <= PAK;  k++  ))
do
# write prefixes
echo -n $k" " | tee -a $RETURN
for FILE in ${HITS[@]}
do
CNT=`grep "Precision at k=$k with pfl=$pfl" $FILE`
CNT=${CNT[0]//Precision at k=$k with pfl=$pfl: /}
echo -n $CNT" " | tee -a $RETURN
done
echo "" | tee -a $RETURN
done
}

#PAK using fixed k
PAKFK () {
PREFIX="pakfk$k."
RETURN=$RETURNDIR$PREFIX$NAME
#reset old output file
echo -n "" | tee $RETURN
echo "retrieves Precision with fixed k=$k"
for((  pfl = 0 ;  pfl <= PFL;  pfl++  ))
do
# write prefixes
echo -n $pfl" " | tee -a $RETURN
for FILE in ${HITS[@]}
do
CNT=`grep "Precision at k=$k with pfl=$pfl" $FILE`
CNT=${CNT[0]//Precision at k=$k with pfl=$pfl: /}
echo -n $CNT" " | tee -a $RETURN
done
echo "" | tee -a $RETURN
done
}

MRR () {
PREFIX="mrr."
RETURN=$RETURNDIR$PREFIX$NAME
#reset old output file
echo -n "" | tee $RETURN
echo "retrieves MRR with max pfl=$PFL"
for((  pfl = 0 ;  pfl <= PFL;  pfl++  ))
do
# write prefixes
echo -n $pfl" " | tee -a $RETURN
for FILE in ${HITS[@]}
do
CNT=`grep "MRR with pfl=$pfl" $FILE`
CNT=${CNT[0]//MRR with pfl=$pfl: /}
echo -n $CNT" " | tee -a $RETURN
done
echo "" | tee -a $RETURN
done
}

KSS () {
PREFIX="kss."
RETURN=$RETURNDIR$PREFIX$NAME
#reset old output file
echo -n "" | tee $RETURN
echo "retrieves KSS"
for((  k = 1 ;  k <= PAK;  k++  ))
do
# write prefixes
echo -n $k" " | tee -a $RETURN
for FILE in ${HITS[@]}
do
CNT=`grep "^KSS at k=$k" $FILE`
CNT=${CNT[0]//KSS at k=$k: /}
echo -n $CNT" " | tee -a $RETURN
done
echo "" | tee -a $RETURN
done
}

NKSS () {
PREFIX="nkss."
RETURN=$RETURNDIR$PREFIX$NAME
#reset old output file
echo -n "" | tee $RETURN
echo "retrieves NKSS"
for((  k = 1 ;  k <= PAK;  k++  ))
do
# write prefixes
echo -n $k" " | tee -a $RETURN
for FILE in ${HITS[@]}
do
CNT=`grep "NKSS at k=$k" $FILE`
CNT=${CNT[0]//NKSS at k=$k: /}
echo -n $CNT" " | tee -a $RETURN
done
echo "" | tee -a $RETURN
done
}

cd $LOGDIR

FILES=()
#add .log-files to FILES
for FILE in *.log
do
	FILES+=($FILE)
done

echo "matching files"
FILECNT=0
for FILE in ${FILES[@]}
do
	if [[ $FILE =~ $NAME ]]
		then 
			HITS+=($FILE)
			echo "match at $FILE"
			FILECNT+=1
			
		else
			echo " fail at $FILE"
	fi
done

#exit if no files were matched
if [[ $FILECNT == 0 ]]
	then
		echo "no matches found"
		echo $NAME
		exit
fi

#either k or pfl have to be defined when using PAK
if [[ ${#k} == 0 && ${#pfl} == 0 && "$metrics" == *PAK* ]]
	then echo "either set value for k or pfl for using PAK"
fi

if [[ ${#k} != 0 && ${#pfl} != 0 && "$metrics" == *PAK* ]]
	then echo "either set value for k or pfl for using PAK"
fi

#matching methods
if [[ ${#k} != 0 && ${#pfl} == 0 && "$metrics" == *PAK* ]]
	then PAKFK
fi

if [[ ${#k} == 0 && ${#pfl} != 0 && "$metrics" == *PAK* ]]
	then PAKFPFL
fi

if [[ "$metrics" == *MRR* ]]
	then MRR
fi

if [[ "$metrics" == *[^N]KSS* || "$metrics" == KSS* ]]
	then KSS
fi

if [[ "$metrics" == *NKSS* ]]
	then NKSS
fi

