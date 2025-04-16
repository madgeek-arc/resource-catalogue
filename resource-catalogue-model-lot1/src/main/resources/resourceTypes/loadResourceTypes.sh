#!/bin/bash

verbose="--write-out \%\{http\_code\} --silent --output /dev/null"
UPLOADED=""
FAILED=""
token=""
function post_resourceType {
    data=`cat $1`
    response=$(curl -X POST --write-out %\{http_code\} --silent --output /dev/null --data "$data" --header "Content-Type:application/json" --header "Authorization: Bearer $token" http://$2:8080/resource-catalogue-service/resourceType/)
    if ((${response} >= 200 && ${response} < 300 )); then
        colors="\e[32m"
        UPLOADED="$1\n$UPLOADED"
    else
        colors="\e[31m"
        FAILED="$1\n$FAILED"
    fi
    echo -e "[${colors}${response}\e[0m] Resource posted --> $1"
}

read -p 'Authorization token: ' token

for resource in *.json; do
    post_resourceType ${resource} $1
done

wait
echo "FINISHED UPLOADING RESOURCE TYPES"
notify-send "Finished Uploading Resources" "Successful :\n\n$UPLOADED\n\nFailed :\n$FAILED\n"
