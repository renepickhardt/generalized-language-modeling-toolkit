# Generalized Language Modeling Toolkit (GLMTK)
#
# Copyright (C) 2014-2015 Lukas Schmelzeisen, Rene Pickhardt
#
# GLMTK is free software: you can redistribute it and/or modify it under the
# terms of the GNU General Public License as published by the Free Software
# Foundation, either version 3 of the License, or (at your option) any later
# version.
#
# GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
# A PARTICULAR PURPOSE. See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along with
# GLMTK. If not, see <http://www.gnu.org/licenses/>.
#
# See the AUTHORS file for contributors.

#!/usr/bin/env bash

set -u # trap uses of unset variables
set -e # exit on unchecked failure

# get script location
GLMTK_DIR=`readlink -f $BASH_SOURCE | xargs dirname`

CONFIG_FILE="$GLMTK_DIR/glmtk.conf"
JAR_FILE="$GLMTK_DIR/target/glmtk-0.0.1-SNAPSHOT-jar-with-dependencies.jar"

if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "Config file missing: Could not open '$CONFIG_FILE'."
    echo "Did you copy 'glmtk.config.sample' to 'glmtk.conf' in the installation directory '$GLMTK_DIR'?"
    exit
fi

if [[ ! -f "$JAR_FILE" ]]; then
    echo "Jar file missing: Could not open '$JAR_FILE'."
    echo "Did you execute './build.sh' in the installation directory '$GLMTK_DIR'?"
    exit
fi

# Calculate jvm memory
DEFAULT_JVM_MEMORY=4096
JVM_MEMORY=`grep -oP "^\s+jvm:\s+\K\d+" "$CONFIG_FILE"`
if [[ -z "$JVM_MEMORY" ]]; then
    JVM_MEMORY=$DEFAULT_JVM_MEMORY
fi

# Variable named MAVEN_OPTS, because maven listens to this environmen variable
MAVEN_OPTS="-Xmx${JVM_MEMORY}m -javaagent:$GLMTK_DIR/lib/classmexer/classmexer/0.03/classmexer-0.03.jar"

IS_TTY_STDERR="false"
if [[ -t 2 ]] ; then
    IS_TTY_STDERR="true"
fi

# Setup java library path
if [[ -z "${JAVA_HOME:-}" ]] ; then
    echo "Environment variable JAVA_HOME not set. Are you sure you have installed java run time environment and java developer toolkit. Consult a search engine if you are not sure how to set JAVA_HOME"
    exit
fi;
LIBRARY_PATH="${GLMTK_DIR}/lib"

# TODO: ulimit unlimited?
ulimit -v 20000000
nice java $MAVEN_OPTS -Dglmtk.dir="$GLMTK_DIR" -Dglmtk.isttyStderr="$IS_TTY_STDERR" -Dfile.encoding="UTF-8" -jar -Djava.library.path="${LIBRARY_PATH}" "$JAR_FILE" "$@"
