#!/usr/bin/env bash

set -u # trap uses of unset variables
set -e # exit on unchecked failure

path=`pwd`

for script in `find . -maxdepth 1 -executable -type f -name "glmtk*"`
do
	script=${script:2}
	echo "installing ${script}..."
	sudo ln -s "${path}/${script}" "/usr/local/bin/${script}"
done
