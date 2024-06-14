#!/bin/bash
cd sim-modem-api
./gradlew clean
./gradlew bootJar
cp app/build/libs/*.jar ../server.jar
cd ..
