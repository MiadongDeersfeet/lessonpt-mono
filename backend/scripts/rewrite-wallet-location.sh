#!/bin/sh
# sqlnet.ora / ojdbc.properties 의 Windows DIRECTORY만 현재 Wallet 경로로 바꾼다.
# cwallet.sso, ewallet.p12, *.jks 는 수정하지 않는다.

set -eu

rewrite_windows_directory() {
  file="$1"
  wallet_dir="$2"
  [ -f "$file" ] || return 0
  if ! grep -Eq '[A-Za-z]:[\\/]' "$file"; then
    return 0
  fi
  tmp="${file}.tmp"
  sed -E "s#(DIRECTORY[[:space:]]*=[[:space:]]*)\"?[A-Za-z]:[^)\"]*\"?#\1\"${wallet_dir}\"#g" "$file" > "$tmp"
  mv "$tmp" "$file"
  chmod 600 "$file" 2>/dev/null || true
}

rewrite_wallet_location() {
  wallet_dir=$(printf '%s' "$1" | tr '\\' '/')
  rewrite_windows_directory "${1}/sqlnet.ora" "$wallet_dir"
  rewrite_windows_directory "${1}/ojdbc.properties" "$wallet_dir"
}

case "${0:-}" in
  *rewrite-wallet-location.sh)
    rewrite_wallet_location "$1"
    ;;
esac
