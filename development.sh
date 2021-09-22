#!/bin/bash

set -euo pipefail

# allow running from any working directory
WD=$(dirname "$0")
cd "${WD}"

DOCKER_COMPOSE_CMD="docker-compose -f ./docker/docker-compose.yml"

function start_dependencies {
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatchingtestdb
}

function start_all {
  $DOCKER_COMPOSE_CMD up --build -d 
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
    Start the dependencies the map-matching service needs

  start
    Start the dependencies and the map-matching service

  stop
    Stop all map-matching related containers

  generate:jooq
    Start the dependencies and generate JOOQ classes

  help
    Show this usage information
  "
}

case $1 in
start:deps)
  start_dependencies
  ;;

start)
  start_all
  ;;

stop)
  stop_all
  ;;

generate:jooq)
  start_dependencies
  while ! pg_isready -h localhost -p 18000
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
