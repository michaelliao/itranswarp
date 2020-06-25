#!/bin/bash

# check java version:
JAVA_VERSION=`java -version 2>&1 | awk 'NR==1{ gsub(/"/,""); print $3 }'`

if [[ $JAVA_VERSION == 11* ]]; then
  echo "Java version ok: $JAVA_VERSION"
else
  echo "Java version is expected as 11 but $JAVA_VERSION. try:"
  echo "export JAVA_HOME=\`/usr/libexec/java_home -v 11\`"
  exit 1
fi

# maven build:
./mvnw -DskipTests clean package

cp target/itranswarp.jar release/

# compress resources:
cd src/main/resources
tar --exclude ".*" -czvf ../../../release/resources.tar.gz favicon.ico robots.txt static/
cd ../../..

echo "DONE"
