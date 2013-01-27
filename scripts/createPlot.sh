#AUTOR: Martin Koerner, Rene Pickhardt
#Purpose to aggregate data from result logfiles

#run with: trainedOnDS=X trainedOnLang=X testedOnDS=X testedOnLang=X typ=X weighted=X modelParameter=X sam=X split=X joinlength=X nQ=X metrics=X ./createPlot.sh

#X: values seperated by commas
#leaving out a parameter --> wildcard

#e.g.:
#trainedOnDS=wiki trainedOnLang=de testedOnDS=wiki testedOnLang=de typ=typolgy,lm weighted=no modelParameter=2,5 sam=0 split=95 joinlength=10 nQ=100000 metrics=KSS ./createPlot.sh

#trainedOnDS=wiki trainedOnLang=de testedOnDS=wiki testedOnLang=de typ=typolgy,lm weighted=no modelParameter=2,5 sam=0 split=95 joinlength=10 nQ=100000 fixedk=2 metrics=NKSSFIXEDK ./createPlot.sh

#prefix length up to PFL
PFL=5
#precision at k up to PAK
PAK=5

#storage directory for res.*.log files
#LOGDIR="/media/07d76f7e-d27d-441b-b2ae-ea25d79bc3fa/typology/results/"
LOGDIR="/home/martin/results/"
#storage directory for return files
#RETURNDIR="/media/07d76f7e-d27d-441b-b2ae-ea25d79bc3fa/typology/plots/"
RETURNDIR="/home/martin/plots/"

#format for PARAMETERS:
#	0		1	2		3	4		5	6	7	8				9	10		11		12		
#"res.trainedOn-$trainedOnDS-$trainedOnLang-testedOn-$testedOnDS-$testedOnLang-$typ-$weighted-modelParameter$modelParameter-sam$sam-split$split-joinlength$joinlength-nQ$nQ.log"

#modifying parameters
PARAMETERS=(false false false false false false false false false false false false)
PARAMETERSLENGTH=${#PARAMETERS[*]}

#format parameters and set PARAMETERS for labelling 
if [[ ${#trainedOnDS} != 0 ]]
	then	trainedOnDS="(${trainedOnDS//,/|})"		
	else	trainedOnDS=.*
fi
NUMOFPAR=(`echo $trainedOnDS | tr '|' ' '`)
		if [[ ${#NUMOFPAR[@]} != 1 ]]
			then 	PARAMETERS[0]=true
				PARAMETERS[1]=true
		fi	


if [[ ${#trainedOnLang} != 0 ]]
	then	trainedOnLang="(${trainedOnLang//,/|})"
		
	else	trainedOnLang=.*
fi
NUMOFPAR=(`echo $trainedOnLang | tr '|' ' '`)
		if [[ ${#NUMOFPAR[@]} != 1 ]]
			then	PARAMETERS[0]=true
				PARAMETERS[2]=true
		fi

if [[ ${#testedOnDS} != 0 ]]
	then	testedOnDS="(${testedOnDS//,/|})"
	else	testedOnDS=.*
fi
NUMOFPAR=(`echo $testedOnDS | tr '|' ' '`)
		if [[ ${#NUMOFPAR[@]} != 1 ]]
			then	PARAMETERS[3]=true
				PARAMETERS[4]=true
		fi

if [[ ${#testedOnLang} != 0 ]]
	then	testedOnLang="(${testedOnLang//,/|})"
	else	testedOnLang=.* 
fi
NUMOFPAR=(`echo $testedOnLang | tr '|' ' '`)
		if [[ ${#NUMOFPAR[@]} != 1 ]]
			then	PARAMETERS[3]=true
				PARAMETERS[5]=true
		fi

if [[ ${#typ} != 0 ]]
	then	typ="(${typ//,/|})"
		
	else	typ=.*
fi
NUMOFPAR=(`echo $typ | tr '|' ' '`)
		if [[ ${#NUMOFPAR[@]} != 1 ]]
			then	PARAMETERS[6]=true
		fi

if [[ ${#weighted} != 0 ]]
	then	weighted="(${weighted//,/|})"		
	else	weighted=.* 
fi
NUMOFPAR=(`echo $weighted | tr '|' ' '`)
		if [[ ${#NUMOFPAR[@]} != 1 ]]
			then	PARAMETERS[7]=true
		fi

if [[ ${#modelParameter} != 0 ]]
	then	modelParameter="(${modelParameter//,/|})"
	else	modelParameter=.*
fi
NUMOFPAR=(`echo $modelParameter | tr '|' ' '`)
		if [[ ${#NUMOFPAR[@]} != 1 ]]
			then	PARAMETERS[8]=true
		fi

if [[ ${#sam} != 0 ]]
	then	sam="(${sam//,/|})"
	else	sam=.*
fi
NUMOFPAR=(`echo $sam | tr '|' ' '`)
		if [[ ${#NUMOFPAR[@]} != 1 ]]
			then	PARAMETERS[9]=true
		fi

if [[ ${#split} != 0 ]]
	then	split="(${split//,/|})"
	else	split=.*
fi
NUMOFPAR=(`echo $split | tr '|' ' '`)
		if [[ ${#NUMOFPAR[@]} != 1 ]]
			then	PARAMETERS[10]=true
		fi

if [[ ${#joinlength} != 0 ]]
	then	joinlength="(${joinlength//,/|})"
	else	joinlength=.*
fi
NUMOFPAR=(`echo $joinlength | tr '|' ' '`)
		if [[ ${#NUMOFPAR[@]} != 1 ]]
			then	PARAMETERS[11]=true
		fi

if [[ ${#nQ} != 0 ]]
	then	nQ="(${nQ//,/|})"
	else	nQ=.* 
fi
NUMOFPAR=(`echo $nQ | tr '|' ' '`)
		if [[ ${#NUMOFPAR[@]} != 1 ]]
			then	PARAMETERS[12]=true
		fi

#set regular expression for filtering files and return file name
REGEX="res.trainedOn-$trainedOnDS-$trainedOnLang-testedOn-$testedOnDS-$testedOnLang-$typ-$weighted-modelParameter$modelParameter-sam$sam-split$split-joinlength$joinlength-nQ$nQ.log"
NAME=${REGEX//"res."/}
NAME=${NAME//|/-}
NAME=${NAME//(/}
NAME=${NAME//)/}
NAME=${NAME//".log"/}
NAME=${NAME//".*"/"(all)"}


#format for .plot-file
PLOT () {
echo "file count: $FILECNT"
echo "reset" | tee "$RETURN.plot"
echo "source='./$NAME$SUFFIX'" | tee -a "$RETURN.plot"
echo "outputName='$NAME$SUFFIX.ps'" | tee -a "$RETURN.plot"
echo "" | tee -a "$RETURN.plot"

echo "################ label ################" | tee -a "$RETURN.plot"

echo "#change key position and size" | tee -a "$RETURN.plot"
echo "set key out vert center top" | tee -a "$RETURN.plot"
echo "set key box" | tee -a "$RETURN.plot"
#key font only for viewing "all"
#echo "set key font ',10'" | tee -a "$RETURN.plot"

echo "set xlabel '$XLABEL'" | tee -a "$RETURN.plot"
echo "set ylabel '$YLABEL'" | tee -a "$RETURN.plot"
echo "set title '$TITLE'" | tee -a "$RETURN.plot"

echo "set style data histogram" | tee -a "$RETURN.plot"
echo "set style histogram cluster gap 1" | tee -a "$RETURN.plot"
echo "set style fill pattern border" | tee -a "$RETURN.plot"
echo "set boxwidth 0.8" | tee -a "$RETURN.plot"

echo -n "plot " | tee -a "$RETURN.plot"

for((  i = 0 ;  i < FILECNT;  i++  ))
do
	let COLUMN=i+2	
	if [[ "$metrics" == *NKSS* && ${#fixedK} != 0 || "$metrics" == *MRR* && ${#fixedPFL} != 0 || "$metrics" == *PAK* && ${#fixedK} != 0 && ${#fixedPFL} != 0 ]]
		then 
		echo -n "	source using $COLUMN:xtic(1) title '${LABELS[$i]// modelParameter?/}' lt -1" | tee -a "$RETURN.plot"
		else 
		echo -n "	source using $COLUMN:xtic(1) title '${LABELS[$i]}' lt -1" | tee -a "$RETURN.plot"
	fi
	if [[ $i -le $FILECNT-2 ]]
		then	echo ", \\" | tee -a "$RETURN.plot"
		else	echo "" | tee -a "$RETURN.plot"
	fi
done
echo "" | tee -a "$RETURN.plot"
echo "set output outputName" | tee -a "$RETURN.plot"
echo "set terminal postscript" | tee -a "$RETURN.plot"
echo "replot" | tee -a "$RETURN.plot"

echo "plotted: "$NAME #user information only
echo "pause -1 \"Hit return to continue\"" | tee -a "$RETURN.plot"
cd $RETURNDIR
gnuplot $RETURN.plot
}

#PAK using fixed pfl
PAKFPFL () {
SUFFIX=".pakfpfl$fixedPFL"
RETURN=$RETURNDIR$NAME$SUFFIX
TITLE="precision at k using fixed prefix length=$fixedPFL"
XLABEL="k"
YLABEL="precision at k"
#reset old output file
echo -n "" | tee $RETURN
echo "retrieves Precision with fixed pfl=$fixedPFL"
for((  k = 1 ;  k <= PAK;  k++  ))
do
	# write prefixes
	echo -n $k" " | tee -a $RETURN
	for FILE in ${HITS[@]}
	do
		CNT=`grep "Precision at k=$k with pfl=$fixedPFL" $FILE`
		CNT=${CNT[0]//Precision at k=$k with pfl=$fixedPFL: /}
		echo -n $CNT" " | tee -a $RETURN
	done
	echo "" | tee -a $RETURN
done

PLOT
}

#PAK using fixed k
PAKFK () {
SUFFIX=".pakfk$fixedK"
RETURN=$RETURNDIR$NAME$SUFFIX
TITLE="precision at k using fixed k=$fixedK"
XLABEL="prefix length"
YLABEL="precision at k=$fixedK"
#reset old output file
echo -n "" | tee $RETURN
echo "retrieves Precision with fixed k=$fixedK"
for((  pfl = 0 ;  pfl <= PFL;  pfl++  ))
do
	# write prefixes
	echo -n $pfl" " | tee -a $RETURN
	for FILE in ${HITS[@]}
	do
		CNT=`grep "Precision at k=$fixedK with pfl=$pfl" $FILE`
		CNT=${CNT[0]//Precision at k=$fixedK with pfl=$pfl: /}
		echo -n $CNT" " | tee -a $RETURN
	done
	echo "" | tee -a $RETURN
done

PLOT
}

PAKFKFPFL () {
TEMPPFL="fpfl$fixedPFL"
SUFFIX=".pakfk$fixedKf$TEMPPFL"
RETURN=$RETURNDIR$NAME$SUFFIX
TITLE="precision at k using fixed k=$fixedK and fixed prefix length=$fixedPFL"
XLABEL="model length"
YLABEL="precision at k"
#reset old output file
echo -n "" | tee $RETURN
echo "retrieves PAKFKFPFL"
for((  modelLength = 2 ;  modelLength <= 5;  modelLength++  ))
do
tempLine="$modelLength "
for FILE in ${HITS[@]}
do
	TEMPFILE=${FILE/*modelParameter/}
	TEMPFILE=${TEMPFILE/-*/}
	# write prefixes
	echo $TEMPFILE
	if [[ $TEMPFILE == $modelLength ]]
	then
		CNT=`grep "Precision at k=$fixedK with pfl=$fixedPFL" $FILE`
		CNT=${CNT[0]//Precision at k=$fixedK with pfl=$fixedPFL: /}
		tempLine=$tempLine$CNT" "
	#comment out the else branch for building correct collumns	
	else	tempLine=$tempLine"0.0 "
fi
done
echo $tempLine | tee -a $RETURN
done
PLOT
}

MRR () {
SUFFIX=".mrr"
RETURN=$RETURNDIR$NAME$SUFFIX
TITLE="mean reciprocal rank with max prefix length=$PFL"
XLABEL="prefix length"
YLABEL="mean reciprocal rank"
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

PLOT
}

MRRFPFL () {
SUFFIX=".mrrfpfl$fixedPFL"
RETURN=$RETURNDIR$NAME$SUFFIX
TITLE="mean reciprocal rank with fixed prefix length=$fixedPFL"
XLABEL="model length"
YLABEL="normalized keystroke savings"
#reset old output file
echo -n "" | tee $RETURN
echo "retrieves MRRFPFL"
for((  modelLength = 2 ;  modelLength <= 5;  modelLength++  ))
do
tempLine="$modelLength "
for FILE in ${HITS[@]}
do
	TEMPFILE=${FILE/*modelParameter/}
	TEMPFILE=${TEMPFILE/-*/}
	# write prefixes
	echo $TEMPFILE
	if [[ $TEMPFILE == $modelLength ]]
	then
		CNT=`grep "MRR with pfl=$fixedPFL" $FILE`
		CNT=${CNT[0]//MRR with pfl=$fixedPFL: /}
		tempLine=$tempLine$CNT" "
	#comment out the else branch for building correct collumns	
	else	tempLine=$tempLine"0.0 "
fi
done
echo $tempLine | tee -a $RETURN
done
PLOT
}

KSS () {
SUFFIX=".kss"
RETURN=$RETURNDIR$NAME$SUFFIX
TITLE="average keystroke savings"
XLABEL="k"
YLABEL="keystroke savings"
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

PLOT
}

NKSS () {
SUFFIX=".nkss"
RETURN=$RETURNDIR$NAME$SUFFIX
TITLE="average normalized keystroke savings"
XLABEL="k"
YLABEL="normalized keystroke savings"
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

PLOT
}

NKSSFIXEDK () {
SUFFIX=".nkssfk$fixedK"
RETURN=$RETURNDIR$NAME$SUFFIX
TITLE="average normalized keystroke savings using fixed k=$fixedK"
XLABEL="model length"
YLABEL="normalized keystroke savings"
#reset old output file
echo -n "" | tee $RETURN
echo "retrieves NKSS"
for((  modelLength = 2 ;  modelLength <= 5;  modelLength++  ))
do
tempLine="$modelLength "
for FILE in ${HITS[@]}
do
	TEMPFILE=${FILE/*modelParameter/}
	TEMPFILE=${TEMPFILE/-*/}
	# write prefixes
	echo $TEMPFILE
	if [[ $TEMPFILE == $modelLength ]]
	then
		CNT=`grep "NKSS at k=$fixedK" $FILE`
		CNT=${CNT[0]//NKSS at k=$fixedK: /}
		tempLine=$tempLine$CNT" "
	#comment out the else branch for building correct collumns	
	else	tempLine=$tempLine"0.0 "
fi
done
echo $tempLine | tee -a $RETURN
done
PLOT
}


FILES=()
#adding .log-files to FILES
for FILE in $LOGDIR*.log
do
	FILES+=($FILE)
done

echo "matching files"
FILECNT=0
LABELS=()
for FILE in ${FILES[@]}
do
	if [[ $FILE =~ $REGEX ]]
		then 
			echo "match at $FILE"
			HITS+=($FILE)
			let FILECNT+=1
			
			#some labeling
			LABEL=""
			FILESPLIT=(`echo ${FILE//.log/}  | tr '-' ' '`) 
			FILESPLIT[0]="trained:"
			FILESPLIT[3]="tested:"
			for((  k = 0 ;  k <= PARAMETERSLENGTH ;  k++  ))
			do	
				if [[ ${PARAMETERS[k]} = true ]]
					then 	LABEL="$LABEL${FILESPLIT[k]} "
				fi
			done
			LABEL=${LABEL%?}
			LABELS+=("$LABEL")
			echo "LABEL: ${LABEL%?}"

			
		else
			echo " fail at $FILE"
	fi
done


#exit if no files were matched
if [[ $FILECNT == 0 ]]
	then
		echo "no matches found"
		echo $REGEX
		exit
fi

#either k or pfl have to be defined when using PAK
if [[ ${#fixedK} == 0 && ${#fixedPFL} == 0 && "$metrics" == *PAK* ]]
	then echo "either set value for fixedK or fixedPFL for using PAK"
	exit
fi

#matching metrics
if [[ ${#fixedK} != 0 && ${#fixedPFL} != 0 && "$metrics" == *PAK* ]]
	then PAKFKFPFL
	exit
fi

if [[ ${#fixedK} != 0 && ${#fixedPFL} == 0 && "$metrics" == *PAK* ]]
	then PAKFK
fi

if [[ ${#fixedK} == 0 && ${#fixedPFL} != 0 && "$metrics" == *PAK* ]]
	then PAKFPFL
fi

if [[ "$metrics" == *MRR* && ${#fixedPFL} != 0 ]]
	then MRRFPFL
fi

if [[ "$metrics" == *MRR* && ${#fixedPFL} == 0 ]]
	then MRR
fi

if [[ "$metrics" == *[^N]KSS* || "$metrics" == KSS* ]]
	then KSS
fi

if [[ "$metrics" == *NKSS* && ${#fixedK} != 0 ]]
	then NKSSFIXEDK
fi

if [[ "$metrics" == *NKSS* && ${#fixedK} == 0 ]]
	then NKSS
fi
