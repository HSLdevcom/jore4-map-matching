#!/usr/bin/env bash

set -euo pipefail

# allow running from any working directory
WD=$(dirname "$0")
cd "${WD}"

DOCKER_COMPOSE_CMD="docker compose -f ./docker/docker-compose.yml"

start_prod_database() {
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatchingdb
}

start_dev_database() {
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatchingdevdb
}

start_test_database() {
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatchingtestdb
}

start_prod_database_blocking() {
  start_prod_database
  while ! pg_isready -h localhost -p 19000
  do
    echo "waiting for pre-populated database to spin up"
    sleep 2;
  done
}

start_test_database_blocking() {
  start_test_database
  while ! pg_isready -h localhost -p 20000
  do
    echo "waiting for test database to spin up"
    sleep 2;
  done
}

start() {
  start_prod_database_blocking
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatching
}

start_dev() {
  start_dev_database
  # pre-populated database (same data as in production) is used in integration tests
  start_prod_database_blocking
  start_test_database_blocking
  cd "${WD}/spring-boot" && mvn clean spring-boot:run
}

run_tests() {
  # pre-populated database (same data as in production) is used in integration tests
  start_prod_database_blocking
  start_test_database_blocking
  cd "${WD}/spring-boot" && mvn clean verify
}

generate_jooq() {
  cd ./spring-boot
  mvn clean process-resources
}

stop_all() {
  $DOCKER_COMPOSE_CMD stop
}

remove_all() {
  $DOCKER_COMPOSE_CMD down
}

print_usage() {
  echo "
  Usage: $(basename "$0") <command>

  start
    Start pre-populated database (same data as in production) and map-matching
    service in Docker containers.

  start:dev
    Start all databases (development, test, pre-populated) in Docker containers
    and the app locally via Maven (using 'dev' profile).

  start:devdeps
    Start all databases (development, test, pre-populated) to be used while
    developing the application.

  test
    Run JUnit tests via Maven using 'dev' profile. Start pre-populated & test
    database if not already up.

  generate:jooq
    Generate jOOQ classes using test database as dependency.

  stop
    Stop all map-matching related Docker containers.

  remove
    Stop and remove all map-matching related Docker containers.

  help
    Show this usage information.
  "
}

if [[ $# -eq 0 ]]; then
  print_usage
  exit 1
fi

case "$1" in
start)
  start
  ;;

start:dev)
  start_dev
  ;;

start:devdeps)
  start_dev_database
  start_test_database_blocking
  start_prod_database_blocking
  ;;

test)
  run_tests
  ;;

generate:jooq)
  start_test_database_blocking
  generate_jooq
  ;;

stop)
  stop_all
  ;;

remove)
  remove_all
  ;;

help)
  print_usage
  ;;

*)
  echo ""
  echo "Unknown command: '${1}'"
  print_usage
  exit 1
  ;;
esac
