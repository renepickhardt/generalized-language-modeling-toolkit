#!/usr/bin/env bash

set -u # trap uses of unset variables
set -e # exit on unchecked failure

path=`pwd`
install_dir="/usr/local/bin/"

function install {
  dir=$1
  scripts=$2

  for script in `find "${dir}" -maxdepth 1 -executable -type f -name "${scripts}"`
  do
      script=`basename ${script}`
      if [ -h "${install_dir}/${script}" ]
      then
          echo "Found ${script} installed, skipping..."
      else
          echo "Installing ${script}..."
          sudo ln -s "${path}/${dir}/${script}" "${install_dir}/${script}"
      fi
  done
}

install "."       "glmtk*"
install "scripts" "statistics*"
