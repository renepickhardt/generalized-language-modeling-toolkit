dbUser="importer"
modelLength=5
topK=5

echo $1
inputPath=${1/\/glm*/}
dbLang=${inputPath##*/}
inputPath=${inputPath%/*}
dbType=${inputPath##*/}
echo $inputPath
echo $dbLang
echo $dbType

dbName=$dbType"_"$dbLang
echo $dbName


#dbPath="/mnt/vdb/typoeval/mysql/${dbName}/" #server
dbPath=/var/lib/mysql/${dbName}/ #local machine

mysql -u ${dbUser} -e "drop database ${dbName};"
mysql -u ${dbUser} -e "create database ${dbName};"

twoPowerModelLength=2**$modelLength

for (( sequence=1 ; sequence < $twoPowerModelLength ; sequence++ )); do
  if [[ $sequence%2 -eq 0 ]]; then
    continue
  fi

  echo $sequence
  sequenceBinary=`echo "obase=2;$sequence" | bc`
  echo $sequenceBinary

  path=$1$sequenceBinary"/*"
  for file in $path
    do
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
    done;
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
    done;

    importQuery=$importQuery"target, score);"

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
  done;
done;

