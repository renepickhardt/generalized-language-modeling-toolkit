ulimit -v 20000000
nice mvn exec:java -Dexec.mainClass="de.typology.executables.KneserNeyBuilder" -Dfile.encoding=UTF-8
