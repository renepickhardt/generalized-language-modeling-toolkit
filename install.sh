#!/usr/bin/env bash

set -u # trap uses of unset variables
set -e # exit on unchecked failure

path=`pwd`
install_dir="/usr/local/bin/"

for script in `find . -maxdepth 1 -executable -type f -name "glmtk*"`
do
	script=${script:2}
    if [ -h "${install_dir}/${script}" ]
    then
        echo "Found ${script} installed, skipping..."
    else
	    echo "installing ${script}..."
	    sudo ln -s "${path}/${script}" "${install_dir}/${script}"
    fi
done
