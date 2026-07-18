#!/usr/bin/env sh
set -e
DIRNAME="$(cd "$(dirname "$0")" && pwd)"
exec "$DIRNAME/gradle/wrapper/gradle-wrapper.jar" "$@"
