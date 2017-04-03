#!/bin/sh

echo "\n"
curl --request GET \
  --url https://localhost:4430/topic1/events \
  --header 'cache-control: no-cache' \
  --header 'postman-token: eb8a0dd1-d467-da13-8e64-87a4eabb42af' \
  --insecure
echo "\n"
