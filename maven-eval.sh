#!/usr/bin/env bash

./mvnw initialize -q\
  org.codehaus.mojo:exec-maven-plugin:1.6.0:exec\
  -Dexec.executable=echo\
  -Dexec.args="$@"\
  --non-recursive