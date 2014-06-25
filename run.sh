#!/bin/bash

# cd into script location
cd `readlink -f $BASH_SOURCE | xargs dirname`

DEFAULT_MAIN_MEMORY=6096

# Calculate main memory
MAIN_MEMORY=`grep -oP "^mainMemory\s*=\s*\K\d+" config.ini`
if [[ -z $MAIN_MEMORY ]]; then
    MAIN_MEMORY=$DEFAULT_MAIN_MEMORY
fi

# Read arguments
MAIN_CLASS=$1
shift
ARGS=$@

MAVEN_OPTS="-Xmx${MAIN_MEMORY}m"
ulimit -v 20000000
# Need to use eval since ARGS is an array and " will not work
eval "nice mvn clean compile exec:java -Dexec.mainClass=\"$MAIN_CLASS\" -Dexec.args=\"$ARGS\" -Dfile.encoding=\"UTF-8\""
