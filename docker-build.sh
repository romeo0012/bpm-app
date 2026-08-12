# Stáhněte certifikát
# openssl s_client -connect dev-mgmt.prg1paas.t-cloud.eu:443 -showcerts </dev/null | openssl x509 -outform PEM > dev-mgmt.prg1paas.t-cloud.eu.crt
# openssl s_client -showcerts -connect dev-mgmt.prg1paas.t-cloud.eu:443 < /dev/null | awk '/BEGIN CERTIFICATE/,/END CERTIFICATE/ { print }' > dev-mgmt.prg1paas.t-cloud.eu-chain.pem
# Import do truststore
# keytool -import -alias dev-mgmt -file dev-mgmt.prg1paas.t-cloud.eu.crt -keystore truststore.jks -storepass BezpecneHeslo.123!
# keytool -import -alias dev-mgmt-chain -file dev-mgmt.prg1paas.t-cloud.eu-chain.pem -keystore truststore.jks -storepass BezpecneHeslo.123!
# Spusťte s truststore

export DB_URL="jdbc:postgresql://10.100.3.23:5432/camunda"
export DB_USERNAME="webadmin"
export DB_PASSWORD="SH1HhZR7uf"
export KEYCLOAK_ISSUER_URI="https://dev-mgmt.prg1paas.t-cloud.eu/realms/camunda"
export KEYCLOAK_ADMIN_URL="https://dev-mgmt.prg1paas.t-cloud.eu/admin/realms/camunda"
export KEYCLOAK_CLIENT_ID="camunda"
export KEYCLOAK_CLIENT_SECRET="Q0bqK7FJ5eTZcPsHxipgGZukQv2OvB4A"
export CAMUNDA_IDENTITY_CLIENT_ID="camunda-identity-service"
export CAMUNDA_IDENTITY_CLIENT_SECRET="B6wXVJAf6U4Wc5crYXXlladXM7lXQQnV"
export JAVAX_NET_SSL_TRUSTSTORE="truststore.jks"
export JAVAX_NET_SSL_TRUSTSTOREPASSWORD="BezpecneHeslo.123!"
export CAMUNDA_ADMIN_GROUP="camunda-admin"

mvn clean package -DskipTests
java -Djavax.net.ssl.trustStore=$JAVAX_NET_SSL_TRUSTSTORE -Djavax.net.ssl.trustStorePassword=$JAVAX_NET_SSL_TRUSTSTOREPASSWORD -jar target/bpm-app.jar

# docker system prune -a
TOKEN=$1
TASK=$(basename $(pwd))
REPO="ghcr.io/romeo0012"
TAG=$(echo $TASK:latest | tr '[:upper:]' '[:lower:]')
docker build -t $TAG .
docker tag $TAG $REPO/$TAG
echo $TOKEN | docker login ghcr.io -u romeo0012 --password-stdin
docker push $REPO/$TAG