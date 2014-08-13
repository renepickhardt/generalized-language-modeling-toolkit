#!/usr/bin/env bash

# get script location
GLMTK_DIR=`readlink -f $BASH_SOURCE | xargs dirname`

DEFAULT_MAIN_MEMORY=4096

# Calculate main memory
MAIN_MEMORY=`grep -oP "^mainMemory\s*=\s*\K\d+" $GLMTK_DIR/config.ini`
if [[ -z $MAIN_MEMORY ]]; then
    MAIN_MEMORY=$DEFAULT_MAIN_MEMORY
fi

# Variable named MAVEN_OPTS, because maven listens to this environmen variable
MAVEN_OPTS="-Xmx${MAIN_MEMORY}m" # -javaagent:$GLMTK_DIR/lib/classmexer.jar

ulimit -v 20000000
nice java $MAVEN_OPTS -Dglmtk.dir="$GLMTK_DIR" -Dfile.encoding="UTF-8" -jar $GLMTK_DIR/target/glmtk-0.0.1-SNAPSHOT-jar-with-dependencies.jar $@
