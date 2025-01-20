#!/usr/bin/env bash

set -euo pipefail

# allow running from any working directory
WD=$(dirname "$0")
cd "${WD}"

# By default, the tip of the main branch of the jore4-docker-compose-bundle
# repository is used as the commit reference, which determines the version of
# the Docker Compose bundle to download. For debugging purposes, this default
# can be overridden by some other commit reference (e.g., commit SHA or its
# initial substring), which you can pass via the `BUNDLE_REF` environment
# variable.
DOCKER_COMPOSE_BUNDLE_REF=${BUNDLE_REF:-main}

# Define a Docker Compose project name to distinguish the Docker environment of
# this project from others.
export COMPOSE_PROJECT_NAME=jore4-mapmatching

DOCKER_COMPOSE_CMD="docker compose -f ./docker/docker-compose.yml -f ./docker/docker-compose.custom.yml"

# Download Docker Compose bundle from the "jore4-docker-compose-bundle"
# repository. GitHub CLI is required to be installed.
#
# A commit reference is read from global `DOCKER_COMPOSE_BUNDLE_REF` variable,
# which should be set based on the script execution arguments.
download_docker_compose_bundle() {
  local commit_ref="$DOCKER_COMPOSE_BUNDLE_REF"

  local repo_name="jore4-docker-compose-bundle"
  local repo_owner="HSLdevcom"

  # Check GitHub CLI availability.
  if ! command -v gh &> /dev/null; then
    echo "Please install the GitHub CLI (gh) on your machine."
    exit 1
  fi

  # Make sure the user is authenticated to GitHub.
  gh auth status || gh auth login

  echo "Using the commit reference '${commit_ref}' to fetch a Docker Compose bundle..."

  # First, try to find a commit on GitHub that matches the given reference.
  # This function exits with an error code if no matching commit is found.
  local commit_sha
  commit_sha=$(
    gh api \
      -H "Accept: application/vnd.github+json" \
      -H "X-GitHub-Api-Version: 2022-11-28" \
      "repos/${repo_owner}/${repo_name}/commits/${commit_ref}" \
      --jq '.sha'
  )

  echo "Commit with the following SHA digest was found: ${commit_sha}"

  local zip_file="/tmp/${repo_name}.zip"
  local unzip_target_dir_prefix="/tmp/${repo_owner}-${repo_name}"

  # Remove old temporary directories if any remain.
  rm -fr "$unzip_target_dir_prefix"-*

  echo "Downloading the JORE4 Docker Compose bundle..."

  # Download Docker Compose bundle from the jore4-docker-compose-bundle
  # repository as a ZIP file.
  gh api "repos/${repo_owner}/${repo_name}/zipball/${commit_sha}" > "$zip_file"

  # Extract ZIP file contents to a temporary directory.
  unzip -q "$zip_file" -d /tmp

  # Clean untracked files from `docker` directory even if they are git-ignored.
  git clean -fx ./docker

  echo "Copying JORE4 Docker Compose bundle files to ./docker directory..."

  # Copy files from the `docker-compose` directory of the ZIP file to your
  # local `docker` directory.
  mv "$unzip_target_dir_prefix"-*/docker-compose/* ./docker

  # Remove the temporary files and directories created above.
  rm -fr "$zip_file" "$unzip_target_dir_prefix"-*

  echo "Generating a release version file for the downloaded bundle..."

  # Create a release version file containing the SHA digest of the referenced
  # commit.
  echo "$commit_sha" > ./docker/RELEASE_VERSION.txt
}

LOGGED_IN=false

login() {
  if [ $LOGGED_IN != true ]; then
    echo "Log in to Azure..."
    az login
    LOGGED_IN=true
  fi
}

start_prod_database() {
  $DOCKER_COMPOSE_CMD up --build -d jore4-mapmatchingdb
}

start_dev_database() {
  $DOCKER_COMPOSE_CMD -f ./docker/docker-compose.dev.yml up --build -d jore4-mapmatchingdb-dev
}

start_test_database() {
  $DOCKER_COMPOSE_CMD -f ./docker/docker-compose.dev.yml up --build -d jore4-mapmatchingdb-test
}

wait_for_prod_database_to_be_ready() {
  while ! pg_isready -h localhost -p 6433; do
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
  mvn spring-boot:run
}

run_tests() {
  wait_for_test_database_to_be_ready
  wait_for_prod_database_to_be_ready
  mvn clean verify
}

download_digitransit_key() {
  login

  echo "Downloading Digitransit subscription key from Azure Key Vault..."

  # This custom configuration file is used in the Maven properties filtering
  # step and this file is ignored in git.
  local config_file
  config_file="./profiles/dev/config.$(whoami).properties"

  cat <<EOF >> "$config_file"
digitransit.subscription.key=$(
  az keyvault secret show \
    --name "hsl-jore4-digitransit-api-key" \
    --vault-name "hsl-jore4-dev-vault" \
    --query "value" \
    -o tsv
)
EOF
}

generate_jooq() {
  wait_for_test_database_to_be_ready
  mvn clean process-resources
}

stop_all() {
  docker compose --project-name "$COMPOSE_PROJECT_NAME" stop
}

remove_all() {
  docker compose --project-name "$COMPOSE_PROJECT_NAME" down
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

  digitransit:fetch
    Download Digitransit subscription key for the JORE4 account which is used in
    the UI while loading map tiles.

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
  download_docker_compose_bundle
  start
  ;;

start:deps)
  download_docker_compose_bundle
  start_deps
  ;;

run)
  run
  ;;

test)
  run_tests
  ;;

digitransit:fetch)
  download_digitransit_key
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
  echo "Unknown command: '${1}'"
  print_usage
  exit 1
  ;;
esac
