ulimit -v 20000000
mvn clean
mvn compile
nice mvn exec:java -Dexec.mainClass="de.typology.executables.KneserNeyBuilder" -Dfile.encoding=UTF-8
