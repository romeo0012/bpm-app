#!/bin/sh
set -eu

: "${TRUSTSTORE_PASSWORD:?TRUSTSTORE_PASSWORD is required}"

exec java \
  -Xmx128m \
  -Xms128m \
  -XX:MaxMetaspaceSize=128m \
  -Djavax.net.ssl.trustStore=/app/truststore.jks \
  -Djavax.net.ssl.trustStorePassword="$TRUSTSTORE_PASSWORD" \
  -Djavax.net.ssl.trustStoreType=JKS \
  -jar /app/app.jar
