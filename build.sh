#!/usr/bin/env bash

if [[ $1 == "clean" ]]
then
    mvn clean
else
    mvn compile assembly:single
fi
