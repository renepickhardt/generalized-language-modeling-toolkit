#!/usr/bin/env bash

# get script location
GLMTK_DIR=`readlink -f $BASH_SOURCE | xargs dirname`

DEFAULT_MAIN_MEMORY=6096

# Calculate main memory
MAIN_MEMORY=`grep -oP "^mainMemory\s*=\s*\K\d+" $GLMTK_DIR/config.ini`
if [[ -z $MAIN_MEMORY ]]; then
    MAIN_MEMORY=$DEFAULT_MAIN_MEMORY
fi

MAVEN_OPTS="-Xmx${MAIN_MEMORY}m"
ulimit -v 20000000
nice java -Dglmtk.dir="$GLMTK_DIR" -Dfile.encoding="UTF-8" -jar $GLMTK_DIR/target/typology-0.0.1-SNAPSHOT-jar-with-dependencies.jar $@
