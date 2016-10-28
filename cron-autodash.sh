#!/bin/bash
# -*- mode:shell-script; coding:utf-8; -*-
#
# Created: <Thu Oct 27 17:20:04 2016>
# Last Updated: <2016-October-27 17:54:15>
#

org=$1

# the directory of this script
pushd `dirname $0` > /dev/null
SCRIPTPATH=`pwd`
popd > /dev/null

propsfile=${SCRIPTPATH}/autodash-${org}.properties

if [[ ! -f ${propsfile} ]]; then
    echo "Not running the Snapper. The properties file does not exist."
else
    export DISPLAY=:0

    /usr/bin/java -classpath "${SCRIPTPATH}/target/lib/*:${SCRIPTPATH}/target/autodash-1.0-SNAPSHOT.jar" com.dinochiesa.autodash.DashboardSnapper -P ${propsfile} -v

fi
