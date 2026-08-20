# Stáhnout certifikát
# openssl s_client -connect dev-mgmt.prg1paas.t-cloud.eu:443 -showcerts </dev/null | openssl x509 -outform PEM > dev-mgmt.prg1paas.t-cloud.eu.crt
# openssl s_client -showcerts -connect dev-mgmt.prg1paas.t-cloud.eu:443 < /dev/null | awk '/BEGIN CERTIFICATE/,/END CERTIFICATE/ { print }' > dev-mgmt.prg1paas.t-cloud.eu-chain.pem

# Import do truststore
# keytool -import -alias dev-mgmt -file dev-mgmt.prg1paas.t-cloud.eu.crt -keystore truststore.jks -storepass $JAVAX_NET_SSL_TRUSTSTOREPASSWORD
# keytool -import -alias dev-mgmt-chain -file dev-mgmt.prg1paas.t-cloud.eu-chain.pem -keystore truststore.jks -storepass $JAVAX_NET_SSL_TRUSTSTOREPASSWORD

# mvn clean verify
# java -Djavax.net.ssl.trustStore=$JAVAX_NET_SSL_TRUSTSTORE -Djavax.net.ssl.trustStorePassword=$JAVAX_NET_SSL_TRUSTSTOREPASSWORD -jar target/bpm-app.jar
# java -jar target/bpm-app.jar

TASK=$(basename $(pwd))
REPO="ghcr.io/romeo0012"
TAG=$(echo $TASK:latest | tr '[:upper:]' '[:lower:]')
echo "$REPO / $TAG"
docker build -t $TAG .
docker tag $TAG $REPO/$TAG
echo $GITHUB_TOKEN | docker login ghcr.io -u romeo0012 --password-stdin
docker push $REPO/$TAG