#!/bin/bash

UWS_VERSION=4.2tmp


if [ "$UWS_VERSION" = "4.1" ]
then
# uws lib 4.1
wget -O uws-4.1.jar "http://cdsportal.u-strasbg.fr/uwstuto/download?file=library&version=4.1"
mvn install:install-file -Dfile=uws-4.1.jar -DgroupId=cds.uwslib -DartifactId=uwslib -Dversion=4.1 -Dpackaging=jar
rm uws-4.1.jar
fi

if [ "$UWS_VERSION" = "4.2tmp" ]
then
mvn install:install-file -Dfile=lib/uws_4.2tmp.jar -DgroupId=cds.uwslib -DartifactId=uwslib -Dversion=4.2tmp -Dpackaging=jar
fi
