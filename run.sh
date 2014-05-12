#!/bin/bash

loc=""
vars="$@"
if [ "$1" == "-no_out" ];
then
   loc=" > /dev/null"
   vars="${*:2}"
fi

sh -c "java -classpath lib/antlr-3.3-complete.jar:bin/:. Mini $vars $loc"

