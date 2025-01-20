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

wait_for_prod_database_to_be_ready() {
  while ! pg_isready -h localhost -p 19000; do
    echo "waiting for pre-populated database to spin up"
    sleep 2;
  done
}

wait_for_test_database_to_be_ready() {
  while ! pg_isready -h localhost -p 20000; do
    echo "waiting for test database to spin up"
    sleep 2;
  done
}

start() {
  start_prod_database
  wait_for_prod_database_to_be_ready
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatching
}

start_deps() {
  # Pre-populated database (same data as in production) is used in integration
  # tests.
  start_prod_database

  start_dev_database
  start_test_database
}

run() {
  wait_for_test_database_to_be_ready
  wait_for_prod_database_to_be_ready
  cd spring-boot && mvn spring-boot:run
}

run_tests() {
  wait_for_test_database_to_be_ready
  wait_for_prod_database_to_be_ready
  cd spring-boot && mvn clean verify
}

generate_jooq() {
  wait_for_test_database_to_be_ready
  cd spring-boot && mvn clean process-resources
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

  start:deps
    Start all databases (development, test, pre-populated) to be used while
    developing the application.

  run
    Run the application locally via Maven (using 'dev' profile). All the
    databases need to be already running.

  test
    Run JUnit tests via Maven using 'dev' profile.

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

COMMAND=${1:-}

if [[ -z $COMMAND ]]; then
  print_usage
  exit 1
fi

case $COMMAND in
start)
  start
  ;;

start:deps)
  start_deps
  ;;

run)
  run
  ;;

test)
  run_tests
  ;;

generate:jooq)
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
  echo "Unknown command: '${COMMAND}'"
  print_usage
  exit 1
  ;;
esac
