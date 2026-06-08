# Stáhněte certifikát
# openssl s_client -connect dev-mgmt.prg1paas.t-cloud.eu:443 -showcerts </dev/null | openssl x509 -outform PEM > tcloud.crt
# openssl s_client -showcerts -connect dev-mgmt.prg1paas.t-cloud.eu:443 < /dev/null | awk '/BEGIN CERTIFICATE/,/END CERTIFICATE/ { print }' > chain.pem
# Import do truststore
# keytool -import -alias tcloud -file tcloud.crt -keystore truststore.jks -storepass BezpecneHeslo.123!
# keytool -import -alias keycloak -file chain.pem -keystore truststore.jks -storepass BezpecneHeslo.123!
# Spusťte s truststore
# mvn clean package -DskipTests
# java -Djavax.net.ssl.trustStore=truststore.jks -Djavax.net.ssl.trustStorePassword=BezpecneHeslo.123! -jar target/bpm-app.jar
# docker system prune -a

TOKEN=$1
TASK=$(basename $(pwd))
REPO="ghcr.io/romeo0012"
TAG=$(echo $TASK:latest | tr '[:upper:]' '[:lower:]')
docker build -t $TAG .
docker tag $TAG $REPO/$TAG
echo $TOKEN | docker login ghcr.io -u romeo0012 --password-stdin
docker push $REPO/$TAG

# git add -A
# git commit --amend --no-edit
# git push --force-with-lease origin main