ulimit -v 20000000

DEFAULT_MEMORY=4096

MEMORY=`grep -oP "^memory\s*=\s*\K\d+" config.txt`
if [[ -z $MEMORY ]]; then
    MEMORY=$DEFAULT_MEMORY
fi

MAVEN_OPTS="-Xmx${MEMORY}m"

nice mvn clean compile exec:java -Dexec.args="$@" -Dfile.encoding=UTF-8
