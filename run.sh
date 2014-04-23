ulimit -v 20000000
nice mvn clean compile exec:java -Dexec.mainClass="de.typology.executables.KneserNeyBuilder" -Dexec.args="$@" -Dfile.encoding=UTF-8
