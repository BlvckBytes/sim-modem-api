#!/bin/bash
cd sim-modem-api
./gradlew bootJar
cp build/libs/*.jar ../server.jar
cd ..