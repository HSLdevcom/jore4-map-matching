#!/bin/bash

set -euo pipefail

# allow running from any working directory
WD=$(dirname "$0")
cd "${WD}"

DOCKER_COMPOSE_CMD="docker-compose -f ./docker/docker-compose.yml"

function start_prod_database {
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatchingdb
}

function start_dev_database {
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatchingdevdb
}

function start_test_database {
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatchingtestdb
}

function start {
  start_prod_database
  while ! pg_isready -h localhost -p 19000
  do
    echo "waiting for database to spin up"
    sleep 2;
  done
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatching
}

function start_dev {
  start_dev_database
  start_test_database
  while ! pg_isready -h localhost -p 20000
  do
    echo "waiting for test database to spin up"
    sleep 2;
  done
  cd "${WD}/spring-boot" && mvn clean spring-boot:run
}

function run_tests {
  start_test_database
  while ! pg_isready -h localhost -p 20000
  do
    echo "waiting for test database to spin up"
    sleep 2;
  done
  cd "${WD}/spring-boot" && mvn clean verify
}

function stop_all {
  $DOCKER_COMPOSE_CMD stop
}

function remove_all {
  $DOCKER_COMPOSE_CMD down
}

function generate_jooq() {
  cd ./spring-boot
  mvn clean process-resources
}

function usage {
  echo "
  Usage $0 <command>

  start
    Start production database (pre-populated) and map-matching service in Docker container

  start:dev
    Start development & test database in Docker container and the app locally via Maven (using 'dev' profile)

  start:devdeps
    Start development and test database to be used while developing the application

  test
    Run JUnit tests via Maven using 'dev' profile. Start development & test database if not already up.

  stop
    Stop all map-matching related Docker containers

  remove
    Stop and remove all map-matching related Docker containers

  generate:jooq
    Generate jOOQ classes using test database as dependency

  help
    Show this usage information
  "
}

case $1 in
start)
  start
  ;;

start:dev)
  start_dev
  ;;

start:devdeps)
  start_dev_database
  start_test_database
  ;;

stop)
  stop_all
  ;;

remove)
  remove_all
  ;;

test)
  run_tests
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
