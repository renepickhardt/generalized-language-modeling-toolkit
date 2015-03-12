#!/usr/bin/env bash

set -u # trap uses of unset variables
set -e # exit on unchecked failure

function usage() {
    echo "Usage: $0 <jar-file> <group-id> <artiface-id> <version"
}

if [[ -z "${1:-}" ]] || [[ ! -f "$1" ]] || [[ -z "${2:-}" ]] || [[ -z "${3:-}" ]] || [[ -z "${4:-}" ]] ; then
    usage
    exit
fi

jarFile=$1
groupId=$2
artifactId=$3
version=$4

if [[ $jarFile == *"-sources.jar" ]] ; then
    mvn install:install-file -DlocalRepositoryPath=. -DcreateChecksum=true -Dpackaging=jar -Dfile=$jarFile -DgroupId=$groupId -DartifactId=$artifactId -Dversion=$version -Dclassifier=sources
else
    mvn install:install-file -DlocalRepositoryPath=. -DcreateChecksum=true -Dpackaging=jar -Dfile=$jarFile -DgroupId=$groupId -DartifactId=$artifactId -Dversion=$version
fi
