#!/bin/sh

echo "\n"
curl --request GET \
  --url https://localhost:4430/topic1 \
  --header 'cache-control: no-cache' \
  --header 'offset: -1' \
  --header 'postman-token: 414c3f26-8155-1c58-e4d1-4edf4f9993e7' \
  --insecure
echo "\n"
