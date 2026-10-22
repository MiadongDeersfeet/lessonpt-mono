#!/bin/bash
set -euo pipefail
if [[ ! "${PORT:-10000}" =~ ^[0-9]+$ ]] || (( PORT < 1024 || PORT > 65535 )); then
  echo "PORT must be between 1024 and 65535" >&2
  exit 1
fi
# Only substitute PORT; preserve nginx variables.
envsubst '${PORT}' < /app/nginx.conf.template > /tmp/nginx.conf
nginx -t -c /tmp/nginx.conf
java -jar /app/lessonpt.jar &
java_pid=$!
nginx -c /tmp/nginx.conf -g 'daemon off;' &
nginx_pid=$!
cleanup() {
  trap - TERM INT
  kill -QUIT "$nginx_pid" 2>/dev/null || true
  kill -TERM "$java_pid" 2>/dev/null || true
  wait "$nginx_pid" "$java_pid" 2>/dev/null || true
}
trap 'cleanup; exit 0' TERM INT
set +e
wait -n "$java_pid" "$nginx_pid"
status=$?
set -e
cleanup
# Either process exiting unexpectedly should restart the whole service.
if (( status == 0 )); then status=1; fi
exit "$status"
