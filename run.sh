#!/bin/bash

#
# Run integrations locally or on Openshift
#

# Import environment variables
if [ -f ./env.sh ]; then
  . ./env.sh
fi

if [[ -d /opt/openshift ]]; then
  echo "run.sh - Running on Openshift"
  INTEGRATION_JAR=./integration.jar
  LOG_BASEDIR=.
  JAVA_CMD_OPTS="-Xmx350m"
  YAML_TEMPLATE=./application.yaml.template
  cd /opt/openshift
  mkdir ./certs
  cp -s /tmp/*.p12 ./certs
else
  echo "run.sh - Running locally"
  INTEGRATION_JAR=target/bundle/integration.jar
  LOG_BASEDIR=target
  JAVA_CMD_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,address=5000,suspend=n
  YAML_TEMPLATE=./target/bundle/application.yaml.template
  if [[ -z "$SKIP_MVN_BUILD" ]]; then
    mvn clean install -Prun
  fi
fi

# Cleanup tomcat folder from previous runs
rm -rf tomcat ; mkdir tomcat

# Install and start ngrok
# echo "Installing ngrok"
# curl -O https://bin.equinox.io/c/4VmDzA7iaHb/ngrok-stable-linux-amd64.zip
# unzip -o ngrok-stable-linux-amd64.zip
# chmod +x ngrok
# echo "Running ngrok from folder $PWD"
# ./ngrok authtoken $NGROK_TOKEN
# ./ngrok http --subdomain hubspot.symphonyoss --log=stdout 8186 > ngrok.log &

# echo "Checking that ngrok is running..."
# sleep 3
# ps auxwww | grep ngrok
# echo "ngrok logs..."
# cat ngrok.log

# Run the Spring Boot application
echo "Running Spring Boot app from folder $PWD:"
java -Dlog4j2.outputAllToConsole=true -Dlogs.basedir=$LOG_BASEDIR $JAVA_CMD_OPTS \
-jar $INTEGRATION_JAR \
--spring.profiles.active=$APP_ID \
--server.tomcat.basedir=$PWD/tomcat \
--server.address=0.0.0.0
