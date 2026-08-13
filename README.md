SECRETS
-------

GITHUB_TOKEN=xxx
DB_URL="jdbc:postgresql://10.100.3.23:5432/camunda"
DB_USERNAME="webadmin"
DB_PASSWORD="xxx"
KEYCLOAK_ISSUER_URI="https://dev-mgmt.prg1paas.t-cloud.eu/realms/camunda"
KEYCLOAK_ADMIN_URL="https://dev-mgmt.prg1paas.t-cloud.eu/admin/realms/camunda"
KEYCLOAK_CLIENT_ID="camunda"
KEYCLOAK_CLIENT_SECRET="xxx"
CAMUNDA_IDENTITY_CLIENT_ID="camunda-identity-service"
CAMUNDA_IDENTITY_CLIENT_SECRET="xxx"
JAVAX_NET_SSL_TRUSTSTORE="truststore.jks"
JAVAX_NET_SSL_TRUSTSTOREPASSWORD="xxx"
CAMUNDA_ADMIN_GROUP="camunda-admin"


ELASTIC
-------

Největší indexy
curl -k -u elastic:$ELASTIC_PASSWORD 'https://localhost:9200/_cat/indices?v&s=store.size:desc'

Největší shard-y
curl -k -u elastic:$ELASTIC_PASSWORD 'https://localhost:9200/_cat/shards?v&s=store:desc'

Stav disk watermarků
curl -k -u elastic:$ELASTIC_PASSWORD 'https://localhost:9200/_cluster/settings?include_defaults=true&filter_path=**disk.watermark*'

Zkontroluj data stream:
curl -k -u elastic:$ELASTIC_PASSWORD 'https://localhost:9200/_data_stream/metricbeat-8.5.1?pretty'

Udělej rollover:
curl -k -u elastic:$ELASTIC_PASSWORD -X POST 'https://localhost:9200/metricbeat-8.5.1/_rollover?pretty'

Pak smaž starý backing index:
curl -k -u elastic:$EELASTIC_PASSWORDLPW -X DELETE 'https://localhost:9200/.ds-metricbeat-8.5.1-2026.04.09-000002'
Po rolloveru už tenhle index nebude write index a delete bude povolený.

Smazat konkrétní starý index přímo.
curl -k -u elastic:$ELASTIC_PASSWORD -X DELETE 'https://localhost:9200/.ds-metricbeat-8.5.1-2026.03.21-000001'
curl -k -u elastic:$EELASTIC_PASSWORDLPW -X DELETE 'https://localhost:9200/.ds-metricbeat-8.5.1-2026.04.09-000002'

