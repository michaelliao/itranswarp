#!/usr/bin/env bash

cd "$(dirname "$0")"

docker run --rm --env-file env/native.env -p 2019:2019 $(docker images michaelliao/itranswarp -q)
