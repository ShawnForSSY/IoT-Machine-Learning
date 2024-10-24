#!/bin/bash

# Compile and run only SVMGridSearch
javac -cp "lib/*" -d bin src/SVMGridSearch.java
java -cp "bin:lib/*" SVMGridSearch
