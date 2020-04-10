#!/bin/bash

# set JDK 11, only works on Mac:
export JAVA_HOME=`/usr/libexec/java_home -v 11`

# maven build:
./mvnw -DskipTests clean package

cp target/itranswarp.jar release/

# compress resources:
cd src/main/resources
tar --exclude ".*" -czvf ../../../release/resources.tar.gz favicon.ico robots.txt static/
cd ../../..

echo "DONE"
