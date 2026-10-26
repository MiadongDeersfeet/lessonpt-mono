#!/bin/sh
# sqlnet.ora / ojdbc.properties 의 DIRECTORY를 현재 Wallet 경로로 바꾼다.
# JDBC는 따옴표를 경로 문자로 읽으므로 따옴표 없이 쓴다.
# cwallet.sso, ewallet.p12, *.jks 는 수정하지 않는다.

set -eu

rewrite_directory() {
  file="$1"
  wallet_dir="$2"
  [ -f "$file" ] || return 0
  tmp="${file}.tmp"
  sed -E "s#(DIRECTORY[[:space:]]*=[[:space:]]*)\"?[^)\"]*\"?#\1${wallet_dir}#g" "$file" > "$tmp"
  mv "$tmp" "$file"
  chmod 600 "$file" 2>/dev/null || true
}

rewrite_wallet_location() {
  wallet_dir=$(printf '%s' "$1" | tr '\\' '/' | tr -d '"')
  rewrite_directory "${1}/sqlnet.ora" "$wallet_dir"
  rewrite_directory "${1}/ojdbc.properties" "$wallet_dir"
}

case "${0:-}" in
  *rewrite-wallet-location.sh)
    rewrite_wallet_location "$1"
    ;;
esac
