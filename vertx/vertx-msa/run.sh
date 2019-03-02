#!/usr/bin/env bash
set -Eeuo pipefail
if [[ $(uname -s) == "Darwin" ]]; then
    shopt -s expand_aliases
    alias rm="grm"
    alias echo="gecho"
    alias dirname="gdirname"
    alias grep="ggrep"
    alias readlink="greadlink"
    alias tar="gtar"
    alias sed="gsed"
    alias sort="gsort"
    alias date="gdate"
fi
__TMP="/tmp/cdc" && readonly __TMP && mkdir -p $__TMP
__HOME="" && pushd "$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")" >/dev/null \
    && { __HOME=$PWD; popd >/dev/null; } && readonly __HOME

HTTP_PORT=""

__error() {
    echo "$@" 1>&2
    exit 1
}

sensor() {
    if [[ -n $HTTP_PORT ]]; then
        java -Dhazelcast.logging.type=slf4j \
            --add-modules java.se \
            --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
            --add-opens java.base/java.lang=ALL-UNNAMED \
            --add-opens java.base/java.nio=ALL-UNNAMED \
            --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
            --add-opens java.management/sun.management=ALL-UNNAMED \
            --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED \
            -jar ./vertx-msa-sensor/target/vertx-msa-sensor-0.0.1-SNAPSHOT-fat.jar
    else
        __error "HTTP port is required"
    fi
}

dashboard() {
    if [[ -n $HTTP_PORT ]]; then
        java -Dhazelcast.logging.type=slf4j \
            --add-modules java.se \
            --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
            --add-opens java.base/java.lang=ALL-UNNAMED \
            --add-opens java.base/java.nio=ALL-UNNAMED \
            --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
            --add-opens java.management/sun.management=ALL-UNNAMED \
            --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED \
            -jar ./vertx-msa-dashboard/target/vertx-msa-dashboard-0.0.1-SNAPSHOT-fat.jar
    else
        __error "HTTP port is required"
    fi
}

initdb() {
    psql template1 -f $__HOME/initdb.sql
}

store() {
    if [[ -n $HTTP_PORT ]]; then
        java -Dhazelcast.logging.type=slf4j \
            --add-modules java.se \
            --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
            --add-opens java.base/java.lang=ALL-UNNAMED \
            --add-opens java.base/java.nio=ALL-UNNAMED \
            --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
            --add-opens java.management/sun.management=ALL-UNNAMED \
            --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED \
            -jar ./vertx-msa-store/target/vertx-msa-store-0.0.1-SNAPSHOT-fat.jar
    else
        __error "HTTP port is required"
    fi
}

edge() {
    if [[ -n $HTTP_PORT && -n $STORE_HOST && -n $STORE_PORT ]]; then
        java -Dhazelcast.logging.type=slf4j \
            --add-modules java.se \
            --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
            --add-opens java.base/java.lang=ALL-UNNAMED \
            --add-opens java.base/java.nio=ALL-UNNAMED \
            --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
            --add-opens java.management/sun.management=ALL-UNNAMED \
            --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED \
            -jar ./vertx-msa-edge/target/vertx-msa-edge-0.0.1-SNAPSHOT-fat.jar
    else
        __error "HTTP port, store host and port are required"
    fi
}

readonly USAGE="
Usage: $0 [commands] [options]

Commands:
  sensor     Run sensor service
  dashboard  Run dashboard service
  initdb     Create Postgres database
  store      Run store service
  edge       Run edge service

Options:
  -p   HTTP port to listen on
  -sh  Store service host
  -sp  Store service port
"
readonly OPTIONS="${@}"
readonly ARGUMENTS=($OPTIONS)
i=0
for argument in $OPTIONS; do
    i=$(($i+1))
    case $argument in
        -p)
            export HTTP_PORT=${ARGUMENTS[i]}
            readonly HTTP_PORT
            ;;
        -sh)
            export STORE_HOST=${ARGUMENTS[i]}
            readonly STORE_HOST
            ;;
        -sp)
            export STORE_PORT=${ARGUMENTS[i]}
            readonly STORE_PORT
            ;;
    esac
done
readonly COMMAND="${1-}"
case "$COMMAND" in
    sensor)
        sensor
        ;;
    dashboard)
        dashboard
        ;;
    initdb)
        initdb
        ;;
    store)
        store
        ;;
    edge)
        edge
        ;;
    *)
        __error "$USAGE"
        ;;
esac
