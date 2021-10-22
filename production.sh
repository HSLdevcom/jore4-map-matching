#!/bin/bash

set -euo pipefail

# allow running from any working directory
WD=$(dirname "$0")
cd "${WD}"

DOCKER_COMPOSE_CMD="docker-compose -f ./docker/docker-compose.yml"

function start_database {
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatchingdb
}

function start {
  start_database
  while ! pg_isready -h localhost -p 19000
  do
    echo "waiting for database to spin up"
    sleep 2;
  done
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatching
}

function stop_all {
  $DOCKER_COMPOSE_CMD down
}

function usage {
  echo "
  Usage $0 <command>

  start
    Start database and the map-matching service

  stop
    Stop all map-matching related Docker containers

  help
    Show this usage information
  "
}

case $1 in
start)
  start
  ;;

stop)
  stop_all
  ;;

help)
  usage
  ;;

*)
  usage
  ;;
esac
