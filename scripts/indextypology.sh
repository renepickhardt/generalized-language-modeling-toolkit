dbUser="setME"
testName="enwiki55"

#dbPath="/mnt/vdb/typoeval/mysql/${testName}/" #server
dbPath /var/lib/mysql/typology/ #local machine

mysql -u ${dbUser} -e "create database ${testName}"

for file in "$@"

do
xpath=${file%/*} 
xbase=${file##*/}
xfext=${xbase##*.}
xpref=${xbase%.*}
echo $xpath;
echo $xbase;
echo $xfext;
echo $xpref;

#create tables and indices
mysql -u ${dbUser} $testName --local-infile=1 -e "create table ${xfext}${xpref} (source varchar(60),target varchar(60),score float) engine=myisam character set utf8 collate utf8_bin; create index ${xfext}${xpref}_ix on ${xfext}${xpref} (source(60), score desc, target(60));"

#disable indices
myisamchk --keys-used=0 -rq ${dbPath}${xfext}${xpref}

mysql -u ${dbUser} $testName --local-infile=1 -e "flush tables;"

#remove hash (#) from the score and save file to /dev/shm for faster processing
sed 's/#//g' $file > /dev/shm/tmp.typoedges

#import data
mysql -u ${dbUser} $testName --local-infile=1 -e "load data local infile '/dev/shm/tmp.typoedges' into table ${xfext}${xpref} fields terminated by '\t' enclosed by '' lines terminated by '\n' (source, target, score);"

#compress table / really necessary?
myisampack ${dbPath}${xfext}${xpref}

#enable index
myisamchk -rq ${dbPath}${xfext}${xpref} --sort_buffer=3G --sort-index --sort-records=1

#and flush index again.
mysql -u ${dbUser} $testName --local-infile=1 -e "flush tables;"

done

rm /dev/shm/tmp.typoedges
