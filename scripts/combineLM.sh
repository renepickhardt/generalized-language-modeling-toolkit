# combine ngrams
cd $1
for n in 2 3 4 5 
do
cd ${n}/
for l in A B C D E F G H I J K L M N O P Q R S T U V W X Y Z
do
cat ${l}* > ../${l}.${n}n
rm ${l}*
mv ../${l}.${n}n .
done


for l in a b c d e f g h i j k l m n p q r s t u v w x y z
do
cat ${l}* > ../${l}.${n}n
rm ${l}*
mv ../${l}.${n}n .
done


mv other.${n}n ../
cat o* > ../o.${n}n
rm o*
mv ../o.${n}n .
mv ../other.${n}n .
cd ../
done
