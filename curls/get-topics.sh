#!/bin/sh
echo "\n"
curl --request GET \
  --url https://localhost:4430/ \
  --header 'cache-control: no-cache' \
  --header 'postman-token: da1ebca7-610e-3557-0706-7bbb1f03e09d' \
  --insecure
echo "\n"
