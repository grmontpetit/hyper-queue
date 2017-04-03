#!/bin/sh

echo "\n"
curl --request POST \
  --url https://localhost:4430/topic1 \
  --header 'cache-control: no-cache' \
  --header 'content-type: application/json' \
  --header 'postman-token: 063f348d-74d0-20bd-762c-72d1d00c4336' \
  --insecure \
  --data '{"id": "event1","value": "event1 value"}'
echo "\n"
