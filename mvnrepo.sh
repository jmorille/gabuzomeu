#!/bin/bash
@echo off
echo ######################################
echo ### Begin updated local Repository ###
echo ######################################

echo ## Tomcat jdbc pool
mvn install:install-file -Dpackaging=jar  -DgroupId=org.javia.arity  -DartifactId=arity -Dversion=2.1.2 -Dfile=maven-repo/arity-2.1.2.jar



echo ### Enc updated local Repository   ###
echo ######################################
