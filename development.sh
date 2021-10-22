#!/bin/bash

set -euo pipefail

# allow running from any working directory
WD=$(dirname "$0")
cd "${WD}"

DOCKER_COMPOSE_CMD="docker-compose -f ./docker/docker-compose.dev.yml"

function start_dev_database {
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatchingdevdb
}

function start_test_database {
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatchingtestdb
}

function start {
  start_dev_database
  start_test_database
  while ! pg_isready -h localhost -p 18000
  do
    echo "waiting for database to spin up"
    sleep 2;
  done
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatching
}

function stop_all {
  $DOCKER_COMPOSE_CMD down
}

function generate_jooq() {
  cd ./spring-boot
  mvn clean process-resources
}

function usage {
  echo "
  Usage $0 <command>

  start:deps
    Start development and test database

  start
    Start databases and the map-matching service

  stop
    Stop all map-matching related Docker containers

  generate:jooq
    Generate jOOQ classes based on tables existing in the test database

  help
    Show this usage information
  "
}

case $1 in
start:deps)
  start_dev_database
  start_test_database
  ;;

start)
  start
  ;;

stop)
  stop_all
  ;;

generate:jooq)
  start_test_database
  while ! pg_isready -h localhost -p 20000
  do
    echo "waiting for db to spin up"
    sleep 2;
  done
  generate_jooq
  ;;

help)
  usage
  ;;

*)
  usage
  ;;
esac
