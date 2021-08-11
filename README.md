okdata-pipeline-jvm
===================

Collection of JVM pipeline components for [Origo dataplattform](https://oslokommune.github.io/dataplattform/).

## Components

- transformers
  - [json](doc/transformers/json.md)
  - [csv](doc/transformers/csv.md)

## Setup

1. Install [Serverless Framework](https://serverless.com/framework/docs/getting-started/)
2. Install Serverless plugins: `make init`

## Formatting code

Code is formatted using [black](https://pypi.org/project/black/): `make format`.

## Running tests

Tests are run using [tox](https://pypi.org/project/tox/): `make test`.

## Deploy

`make deploy` or `make deploy-prod`.
