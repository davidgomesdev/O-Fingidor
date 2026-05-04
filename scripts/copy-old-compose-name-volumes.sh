#!/bin/bash

set -ex

echo "Validating that the volumes already exist... (compose up then down is needed beforehand)"

docker volume inspect o-fingidor_db
docker volume inspect o-fingidor_postgres_data

echo "Copying data from source volume \"$1\" to destination volume \"$2\"..."
docker run --rm \
           -i \
           -t \
           -v pessoa-faladora_postgres_data:/from \
           -v o-fingidor_postgres_data:/to \
           alpine ash -c "cd /from ; cp -av . /to"
docker run --rm \
           -i \
           -t \
           -v pessoa-faladora_db:/from \
           -v o-fingidor_db:/to \
           alpine ash -c "cd /from ; cp -av . /to"
