TOKEN=$1
TASK=$(basename $(pwd))
PROCESS=$(basename "$(dirname "$(pwd)")")
REPO="ghcr.io/romeo0012"
TAG=$(echo task-$PROCESS-$TASK:latest | tr '[:upper:]' '[:lower:]')
docker build -t $TAG .
docker tag $TAG $REPO/$TAG
echo $TOKEN | docker login ghcr.io -u romeo0012 --password-stdin
docker push $REPO/$TAG
export NODE_EXTRA_CA_CERTS=../../root-ca.pem