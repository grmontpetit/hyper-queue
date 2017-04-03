#!/bin/sh

echo "\n"
curl --request GET \
  --url https://localhost:4430/topic1 \
  --header 'cache-control: no-cache' \
  --header 'postman-token: 370426b4-8fc2-0bf2-a706-bc5cb0b06cae' \
  --insecure
echo "\n"
