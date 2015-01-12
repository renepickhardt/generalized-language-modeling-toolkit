#!/usr/bin/env bash

# get script location
GLMTK_DIR=`readlink -f $BASH_SOURCE | xargs dirname`

CONFIG_FILE=$GLMTK_DIR/glmtk.conf
JAR_FILE=$GLMTK_DIR/target/glmtk-0.0.1-SNAPSHOT-jar-with-dependencies.jar

if [[ ! -f $CONFIG_FILE ]]; then
    echo "Config file missing: Could not open '$CONFIG_FILE'."
    echo "Dic you copy 'glmtk.config.sample' to 'glmtk.conf' in the installation directory '$GLMTK_DIR'?"
    exit
fi

if [[ ! -f $JAR_FILE ]]; then
    echo "Jar file missing: Could not open '$JAR_FILE'."
    echo "Did you execute './build.sh' in the installation directory '$GLMTK_DIR'?"
    exit
fi

# Calculate jvm memory
DEFAULT_JVM_MEMORY=4096
JVM_MEMORY=`grep -oP "^\s+jvm:\s+\K\d+" $CONFIG_FILE`
if [[ -z $JVM_MEMORY ]]; then
    JVM_MEMORY=$DEFAULT_JVM_MEMORY
fi

# Variable named MAVEN_OPTS, because maven listens to this environmen variable
MAVEN_OPTS="-Xmx${JVM_MEMORY}m -javaagent:$GLMTK_DIR/lib/classmexer.jar"

IS_TTY_STDERR="false"
if [[ -t 2 ]] ; then
    IS_TTY_STDERR="true"
fi

# TODO: ulimit unlimited?
ulimit -v 20000000
nice java $MAVEN_OPTS -Dglmtk.dir="$GLMTK_DIR" -Dglmtk.isttyStderr="$IS_TTY_STDERR" -Dfile.encoding="UTF-8" -jar $JAR_FILE "$@"
