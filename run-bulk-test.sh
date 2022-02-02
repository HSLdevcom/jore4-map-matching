#!/usr/bin/env bash

mvn -Pprod -DDB_URL=jdbc:postgresql://localhost:6433/jore4mapmatching?stringtype=unspecified -DDB_USERNAME=mapmatching -DDB_PASSWORD=password clean spring-boot:run
