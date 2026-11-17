#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"

rm -rf out
mkdir -p out

javac -d out src/*.java test/*.java
java -ea -cp out LabTestRunner
