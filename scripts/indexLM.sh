dbUser="importer"
testName="hybridngram"

#dbPath="/mnt/vdb/typoeval/mysql/${testName}/" #server
dbPath=/var/lib/mysql/${testName}/ #local machine

mysql -u ${dbUser} -e "create database ${testName};"

for (( i = 2 ;  i <= 5;  i++  ))
do
path="$1$i/*"
for file in $path

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
if [ $i -eq 2 ];
then 
mysql -u ${dbUser} $testName --local-infile=1 -e "create table ${xfext}${xpref} (source1 varchar(60),target varchar(60),score float) engine=myisam character set utf8 collate utf8_bin;
create index ${xfext}${xpref}_ix on ${xfext}${xpref} (source1(60), score desc);
create index ${xfext}${xpref}_2_ix on ${xfext}${xpref} (source1(60), target(2));
create index ${xfext}${xpref}_3_ix on ${xfext}${xpref} (source1(60), target(3));
create index ${xfext}${xpref}_4_ix on ${xfext}${xpref} (source1(60), target(4));
create index ${xfext}${xpref}_5_ix on ${xfext}${xpref} (source1(60), target(5));"
fi

if [ $i -eq 3 ];
then 
mysql -u ${dbUser} $testName --local-infile=1 -e "create table ${xfext}${xpref} (source1 varchar(60),source2 varchar(60),target varchar(60),score float) engine=myisam character set utf8 collate utf8_bin;
create index ${xfext}${xpref}_ix on ${xfext}${xpref} (source1(60), source2 varchar(60), score desc);
create index ${xfext}${xpref}_2_ix on ${xfext}${xpref} (source1(60), source2 varchar(60), target(2));
create index ${xfext}${xpref}_3_ix on ${xfext}${xpref} (source1(60), source2 varchar(60), target(3));
create index ${xfext}${xpref}_4_ix on ${xfext}${xpref} (source1(60), source2 varchar(60), target(4));
create index ${xfext}${xpref}_5_ix on ${xfext}${xpref} (source1(60), source2 varchar(60), target(5));"
fi

if [ $i -eq 4 ];
then 
mysql -u ${dbUser} $testName --local-infile=1 -e "create table ${xfext}${xpref} (source1 varchar(60),source2 varchar(60), source3 varchar(60), target varchar(60),score float) engine=myisam character set utf8 collate utf8_bin;
create index ${xfext}${xpref}_ix on ${xfext}${xpref} (source1(60), source2 varchar(60), source3 varchar(60), score desc);
create index ${xfext}${xpref}_2_ix on ${xfext}${xpref} (source1(60), source2 varchar(60), source3 varchar(60), target(2));
create index ${xfext}${xpref}_3_ix on ${xfext}${xpref} (source1(60), source2 varchar(60), source3 varchar(60), target(3));
create index ${xfext}${xpref}_4_ix on ${xfext}${xpref} (source1(60), source2 varchar(60), source3 varchar(60), target(4));
create index ${xfext}${xpref}_5_ix on ${xfext}${xpref} (source1(60), source2 varchar(60), source3 varchar(60), target(5));"
fi

if [ $i -eq 5 ];
then 
mysql -u ${dbUser} $testName --local-infile=1 -e "create table ${xfext}${xpref} (source1 varchar(60),source2 varchar(60), source3 varchar(60), source4 varchar(60), target varchar(60),score float) engine=myisam character set utf8 collate utf8_bin;
create index ${xfext}${xpref}_ix on ${xfext}${xpref} (source1(60), source2 varchar(60), source3 varchar(60),  source4 varchar(60), score desc);
create index ${xfext}${xpref}_2_ix on ${xfext}${xpref} (source1(60), source2 varchar(60), source3 varchar(60), source4 varchar(60), target(2));
create index ${xfext}${xpref}_3_ix on ${xfext}${xpref} (source1(60), source2 varchar(60), source3 varchar(60), source4 varchar(60), target(3));
create index ${xfext}${xpref}_4_ix on ${xfext}${xpref} (source1(60), source2 varchar(60), source3 varchar(60), source4 varchar(60), target(4));
create index ${xfext}${xpref}_5_ix on ${xfext}${xpref} (source1(60), source2 varchar(60), source3 varchar(60), source4 varchar(60), target(5));"

fi


#disable indices
myisamchk --keys-used=0 -rq ${dbPath}${xfext}${xpref}

mysql -u ${dbUser} $testName --local-infile=1 -e "flush tables;"

#remove hash (#) from the score and save file to /dev/shm for faster processing
sed 's/#//g' $file > /dev/shm/tmp.typoedges

#import data
if [ $i -eq 2 ];
then 
mysql -u ${dbUser} $testName --local-infile=1 -e "load data local infile '/dev/shm/tmp.typoedges' into table ${xfext}${xpref} fields terminated by '\t' enclosed by '' lines terminated by '\n' (source1, target, score);"
fi

if [ $i -eq 3 ];
then 
mysql -u ${dbUser} $testName --local-infile=1 -e "load data local infile '/dev/shm/tmp.typoedges' into table ${xfext}${xpref} fields terminated by '\t' enclosed by '' lines terminated by '\n' (source1, scource2, target, score);"
fi

if [ $i -eq 4 ];
then 
mysql -u ${dbUser} $testName --local-infile=1 -e "load data local infile '/dev/shm/tmp.typoedges' into table ${xfext}${xpref} fields terminated by '\t' enclosed by '' lines terminated by '\n' (source1, source2, source3, target, score);"
fi

if [ $i -eq 5 ];
then 
mysql -u ${dbUser} $testName --local-infile=1 -e "load data local infile '/dev/shm/tmp.typoedges' into table ${xfext}${xpref} fields terminated by '\t' enclosed by '' lines terminated by '\n' (source1, source2, source3, source4, target, score);"
fi


#compress table / really necessary?
myisampack ${dbPath}${xfext}${xpref}

#enable index
myisamchk -rq ${dbPath}${xfext}${xpref} --sort_buffer=3G #--sort-index --sort-records=1

#and flush index again.
mysql -u ${dbUser} $testName --local-infile=1 -e "flush tables;"

done;
done;
rm /dev/shm/tmp.typoedges
