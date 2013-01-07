dbUser="importer"
testName="hybridtypology"

#dbPath="/mnt/vdb/typoeval/mysql/${testName}/" #server
dbPath=/var/lib/mysql/${testName}/ #local machine

#mysql -u ${dbUser} -e "create database ${testName};"


for (( i = 2 ;  i <= 5;  i++  ))
do
path="$1$i/*"
for file in $path
do


done;
done;
