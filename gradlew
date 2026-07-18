#!/usr/bin/env sh
set -e
DIRNAME="$(cd "$(dirname "$0")" && pwd)"
if [ -x "$DIRNAME/gradle/wrapper/gradle-wrapper.jar" ]; then
  exec java -jar "$DIRNAME/gradle/wrapper/gradle-wrapper.jar" "$@"
fi

if [ -x "$DIRNAME/gradle/wrapper/gradle-9.1.1/bin/gradle" ]; then
  exec "$DIRNAME/gradle/wrapper/gradle-9.1.1/bin/gradle" "$@"
fi

exec java -jar "$DIRNAME/gradle/wrapper/gradle-wrapper.jar" "$@"
