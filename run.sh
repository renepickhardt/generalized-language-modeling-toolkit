ulimit -v 20000000
export MAVEN_OPTS="-Xmx6144m"
nice mvn clean compile exec:java -Dexec.args="$@" -Dfile.encoding=UTF-8
