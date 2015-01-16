#!/usr/bin/env bash

set -u # trap uses of unset variables
set -e # exit on unchecked failure

if [[ $1 == "clean" ]]
then
    mvn clean
else
    mvn compile assembly:single
fi
