#!/usr/bin/env bash

set -eu

# we assume here that all secrets are already read to environment variables

# if DB_URL was not set, build it from parts
if [ -z "${DB_URL+x}" ]; then
  export DB_PORT=${DB_PORT:-"5432"}
  export DB_URL="jdbc:postgresql://${DB_HOSTNAME}:${DB_PORT}/${DB_DATABASE}?stringtype=unspecified"
fi
