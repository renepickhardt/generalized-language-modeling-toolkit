ulimit -v 20000000
nice mvn exec:java -Dexec.mainClass="de.typology.$1" -Dfile.encoding=UTF-8
