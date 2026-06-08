ELPW=j7jBeKaHu0cqMWdU

Největší indexy
curl -k -u elastic:$ELPW 'https://localhost:9200/_cat/indices?v&s=store.size:desc'

Největší shard-y
curl -k -u elastic:$ELPW 'https://localhost:9200/_cat/shards?v&s=store:desc'

Stav disk watermarků
curl -k -u elastic:$ELPW 'https://localhost:9200/_cluster/settings?include_defaults=true&filter_path=**disk.watermark*'

Zkontroluj data stream:
curl -k -u elastic:$ELPW 'https://localhost:9200/_data_stream/metricbeat-8.5.1?pretty'

Udělej rollover:
curl -k -u elastic:$ELPW -X POST 'https://localhost:9200/metricbeat-8.5.1/_rollover?pretty'

Pak smaž starý backing index:
curl -k -u elastic:$ELPW -X DELETE 'https://localhost:9200/.ds-metricbeat-8.5.1-2026.04.09-000002'

Po rolloveru už tenhle index nebude write index a delete bude povolený.

smazat konkrétní starý index přímo.
curl -k -u elastic:$ELPW -X DELETE 'https://localhost:9200/.ds-metricbeat-8.5.1-2026.03.21-000001'
curl -k -u elastic:$ELPW -X DELETE 'https://localhost:9200/.ds-metricbeat-8.5.1-2026.04.09-000002'
