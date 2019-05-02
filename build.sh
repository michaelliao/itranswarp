#!/bin/bash

export JAVA_HOME=`/usr/libexec/java_home -v 11`

mvn -DskipTests clean package

cp target/itranswarp.jar release/

cd src/main/resources
tar --exclude ".*" -czvf ../../../release/resources.tar.gz favicon.ico robots.txt static/
cd ../../..
