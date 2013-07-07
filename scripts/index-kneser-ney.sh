dbUser="importer"
modelLength=5
topK=5

# call with e.g.:
# ./index-kneser-ney.sh out/wiki/test/ kneser-ney
# or
# ./index-kneser-ney.sh out/wiki/test/ mod-kneser-ney

echo $1
echo $2
inputPath=${1%/}
dbLang=${inputPath##*/}
inputPath=${inputPath%/*}
dbDataSet=${inputPath##*/}
dbType=${2//-/_}
echo "inputpath: "$inputPath
echo "lang: "$dbLang
echo "dbDataSet: "$dbDataSet
echo "dbType: "$dbType



buildResultTable () {
  xpath=${file%/*}
  xbase=${file##*/}
  xfext=${xbase##*.}
  xpref=${xbase%.*}
  tableName=$xfext"_"$xpref
  echo "tableName: "$tableName
  echo "xpath: "$xpath
  echo "xbase: "$xbase
  echo "xfext: "$xfext
  echo "xpref: "$xpref

  indexQuery="create table "$tableName" ("
  indexSuffix="("
  importQuery="load data local infile '"$file"' into table "$tableName" fields terminated by '\t' enclosed by '' lines terminated by '\n' ("
  sequenceLengthMinusOne=`expr ${#sequenceBinary} - 1`
  for (( sequencePointer=0; sequencePointer<$sequenceLengthMinusOne; sequencePointer++ )); do
    currentBit=${sequenceBinary:$sequencePointer:1}
    if [ $currentBit -eq 1 ]; then
      indexQuery=$indexQuery"source"$sequencePointer" varchar(60), "
      indexSuffix=$indexSuffix"source"$sequencePointer", "
      importQuery=$importQuery"source"$sequencePointer", "
    fi
  done
  indexQuery=$indexQuery"target varchar(60), score float) engine=myisam character set utf8 collate utf8_bin;"
  if [ $sequence -ne 1 ]; then
    indexQuery=$indexQuery" create index "$tableName"_0_ix on "$tableName$indexSuffix"score desc);"
  fi

  for (( i=1 ; i <= $topK ; i++ )); do
    if [ $sequence -eq 1 ]; then
      indexQuery=$indexQuery" create index "$tableName"_"$i"_ix on "$tableName" (target("$i"), score desc);"
    else
      indexQuery=$indexQuery" create index "$tableName"_"$i"_ix on "$tableName" "$indexSuffix"target("$i"));"
    fi
  done
  importQuery=$importQuery"target, score);"
}

buildDiscountTable () {
  xpath=${file%/*}
  xbase=${file##*/}
  xfext=${xbase##*.}
  xpref=${xbase%.*}
  tableName=$xfext"_"$xpref
  echo "tableName: "$tableName
  echo "xpath: "$xpath
  echo "xbase: "$xbase
  echo "xfext: "$xfext
  echo "xpref: "$xpref

  indexQuery="create table "$tableName" ("
  indexSuffix="("
  importQuery="load data local infile '"$file"' into table "$tableName" fields terminated by '\t' enclosed by '' lines terminated by '\n' ("
  sequenceLengthMinusOne=`expr ${#sequenceBinary} - 1`
  for (( sequencePointer=0; sequencePointer<=$sequenceLengthMinusOne; sequencePointer++ )); do
    currentBit=${sequenceBinary:$sequencePointer:1}
    if [ $currentBit -eq 1 ]; then
      indexQuery=$indexQuery"source"$sequencePointer" varchar(60), "
      indexSuffix=$indexSuffix"source"$sequencePointer", "
      importQuery=$importQuery"source"$sequencePointer", "
    fi
  done
  indexQuery=$indexQuery"score float) engine=myisam character set utf8 collate utf8_bin;"
  importQuery=$importQuery"score);"

  # remove ", " from indexSuffix
  indexSuffix=${indexSuffix%?}
  indexSuffix=${indexSuffix%?}
  indexSuffix=$indexSuffix")"
  indexQuery=$indexQuery" create index "$tableName" on "$tableName$indexSuffix";"
  echo $indexQuery
}

buildIndices () {

  #create tables and indices
  mysql -u ${dbUser} $dbName --local-infile=1 -e "$indexQuery"

  #disable indices
  myisamchk --keys-used=0 -rq ${dbPath}${tableName}
  mysql -u ${dbUser} $dbName --local-infile=1 -e "flush tables;"

  #import data
  mysql -u ${dbUser} $dbName --local-infile=1 -e "$importQuery"

  #compress table / really necessary?
  myisampack ${dbPath}${tableName}

  #enable index
  myisamchk -rq ${dbPath}${tableName} --tmpdir="/mnt/vdb/tmp" --sort_buffer=3G #--sort-index --sort-records=1

  #and flush index again
  mysql -u ${dbUser} $dbName --local-infile=1 -e "flush tables;"
}

buildDatabase () {
echo "dbName: "$dbName
#dbPath="/mnt/vdb/typoeval/mysql/${dbName}/" #server
dbPath=/var/lib/mysql/${dbName}/ #local machine
echo "create database ${dbName};"
mysql -u ${dbUser} -e "drop database \`${dbName}\`;"
mysql -u ${dbUser} -e "create database \`${dbName}\`;"
}

#dbName=$dbDataSet"_"$dbLang"_"$dbType"_high"
#buildDatabase
#twoPowerModelLength=2**$modelLength
#
#for (( sequence=1 ; sequence < $twoPowerModelLength ; sequence++ )); do
#  if [[ $sequence%2 -eq 0 ]]; then
#    continue
#  fi
#
#  echo $sequence
#  sequenceBinary=`echo "obase=2;$sequence" | bc`
#  echo $sequenceBinary
#
#  path=$1"/kneser-ney-high/"$sequenceBinary"/*"
#  for file in $path; do
#    buildResultTable
#    buildIndices
#  done
#done
#
#dbName=$dbDataSet"_"$dbLang"_"$dbType"_low"
#buildDatabase
#
#modelLengthMinusOne=`expr $modelLength - 1`
#twoPowerModelLengthMinusOne=2**$modelLengthMinusOne
#for (( sequence=1 ; sequence < $twoPowerModelLengthMinusOne ; sequence++ )); do
#  if [[ $sequence%2 -eq 0 ]]; then
#    continue
#  fi
#
#  echo $sequence
#  sequenceBinary=`echo "obase=2;$sequence" | bc`
#  echo $sequenceBinary
#
#  path=$1"/kneser-ney-low/"$sequenceBinary"/*"
#  for file in $path; do
#    buildResultTable
#    buildIndices
#  done
#done

dbName=$dbDataSet"_"$dbLang"_"$dbType"_high_discount"
buildDatabase

modelLengthMinusOne=`expr $modelLength - 1`
twoPowerModelLengthMinusOne=2**$modelLengthMinusOne
for (( sequence=1 ; sequence < $twoPowerModelLengthMinusOne ; sequence++ )); do
  if [[ $sequence%2 -eq 0 ]]; then
    continue
  fi

  echo $sequence
  sequenceBinary=`echo "obase=2;$sequence" | bc`
  echo $sequenceBinary

  path=$1"/kneser-ney-high-discount/"$sequenceBinary"/*"
  for file in $path; do
    buildDiscountTable
    buildIndices
  done
done

dbName=$dbDataSet"_"$dbLang"_"$dbType"_low_discount"
buildDatabase
modelLengthMinusTwo=`expr $modelLength - 2`
twoPowerModelLengthMinusTwo=2**$modelLengthMinusTwo
for (( sequence=1 ; sequence < $twoPowerModelLengthMinusTwo ; sequence++ )); do
  if [[ $sequence%2 -eq 0 ]]; then
    continue
  fi

  echo $sequence
  sequenceBinary=`echo "obase=2;$sequence" | bc`
  echo $sequenceBinary

  path=$1"/kneser-ney-low-discount/"$sequenceBinary"/*"
  for file in $path; do
    buildDiscountTable
    buildIndices
  done
done
