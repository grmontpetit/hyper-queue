#!/bin/sh

echo "\n"
curl --request GET \
  --url https://localhost:4430/topic1 \
  --header 'cache-control: no-cache' \
  --header 'id: 0' \
  --header 'postman-token: a2af3dea-d846-9d5f-bcb4-6bb2f7ab50ef' \
  --insecure
echo "\n"
