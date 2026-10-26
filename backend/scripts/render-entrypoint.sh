#!/bin/sh
# Render 컨테이너에서 Wallet과 OCI config를 런타임에만 복원한 뒤 기동한다.
# 인증값과 파일 내용은 출력하지 않는다.

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
# shellcheck disable=SC1091
. "$SCRIPT_DIR/rewrite-wallet-location.sh"

RUNTIME_DIR="${LESSONPT_RUNTIME_DIR:-/var/lessonpt}"
mkdir -p "$RUNTIME_DIR"
chmod 700 "$RUNTIME_DIR" 2>/dev/null || true

resolve_wallet_dir() {
  root="$1"
  if [ -f "$root/tnsnames.ora" ]; then
    printf '%s' "$root"
    return 0
  fi
  nested="$(find "$root" -name tnsnames.ora -type f -print -quit 2>/dev/null || true)"
  if [ -n "$nested" ]; then
    dirname "$nested"
    return 0
  fi
  printf '%s' "$root"
}

flatten_wallet_files() {
  src="$1"
  dest="$2"
  if [ "$src" = "$dest" ]; then
    return 0
  fi
  mkdir -p "$dest"
  for item in "$src"/*; do
    [ -f "$item" ] || continue
    cp "$item" "$dest/"
  done
}

log_wallet_files() {
  dir="$1"
  echo "wallet path=${dir}"
  for name in tnsnames.ora sqlnet.ora cwallet.sso; do
    if [ -f "${dir}/${name}" ]; then
      echo "wallet file ${name}=present"
    else
      echo "wallet file ${name}=missing"
    fi
  done
  if [ -r "$dir" ] && [ -r "${dir}/sqlnet.ora" ] && [ -r "${dir}/cwallet.sso" ]; then
    echo "wallet readable=yes"
  else
    echo "wallet readable=no"
  fi
  if [ -f "${dir}/sqlnet.ora" ] && grep -Eq 'DIRECTORY[[:space:]]*=[[:space:]]*"' "${dir}/sqlnet.ora"; then
    echo "wallet sqlnet directory-quoted=yes"
  else
    echo "wallet sqlnet directory-quoted=no"
  fi
}

align_jdbc_tns_admin() {
  wallet_dir="$1"
  url="${LESSONPT_DB_URL:-}"
  [ -n "$url" ] || return 0
  case "$url" in
    *[Tt][Nn][Ss]_[Aa][Dd][Mm][Ii][Nn]=*)
      aligned="$(printf '%s\n' "$url" | sed -E "s#([Tt][Nn][Ss]_[Aa][Dd][Mm][Ii][Nn]=)[^&]*#\1${wallet_dir}#")"
      export LESSONPT_DB_URL="$aligned"
      if [ "$aligned" != "$url" ]; then
        echo "wallet jdbc TNS_ADMIN aligned"
      fi
      ;;
  esac
}

restore_wallet() {
  canonical="${LESSONPT_ORACLE_WALLET_DIR:-$RUNTIME_DIR/wallet}"
  archive="${LESSONPT_ORACLE_WALLET_ARCHIVE:-}"
  encoded="${LESSONPT_ORACLE_WALLET_B64:-}"

  if [ -z "$archive" ] && [ -z "$encoded" ] && [ ! -f "$canonical/tnsnames.ora" ] && [ -z "${TNS_ADMIN:-}" ]; then
    return 0
  fi

  mkdir -p "$canonical"
  chmod 700 "$canonical"

  if [ -n "$archive" ]; then
    unzip -o -q "$archive" -d "$canonical"
  elif [ -n "$encoded" ]; then
    tmp="$(mktemp)"
    printf '%s' "$encoded" | base64 -d > "$tmp"
    unzip -o -q "$tmp" -d "$canonical"
    rm -f "$tmp"
  fi

  found="$(resolve_wallet_dir "$canonical")"
  flatten_wallet_files "$found" "$canonical"
  wallet_dir="$canonical"

  rewrite_wallet_location "$wallet_dir"
  chmod 700 "$wallet_dir"
  find "$wallet_dir" -type f -exec chmod 600 {} \;
  export TNS_ADMIN="$wallet_dir"
  align_jdbc_tns_admin "$wallet_dir"
  log_wallet_files "$wallet_dir"
}

write_oci_config() {
  if [ -n "${LESSONPT_OCI_CONFIG_FILE:-}" ] && [ -f "$LESSONPT_OCI_CONFIG_FILE" ]; then
    return 0
  fi

  user_ocid="${LESSONPT_OCI_USER_OCID:-}"
  tenancy="${LESSONPT_OCI_TENANCY_OCID:-}"
  region="${LESSONPT_OCI_REGION:-}"
  fingerprint="${LESSONPT_OCI_FINGERPRINT:-}"
  private_key="${LESSONPT_OCI_API_PRIVATE_KEY:-}"

  if [ -z "$user_ocid" ] || [ -z "$tenancy" ] || [ -z "$region" ] || [ -z "$fingerprint" ] || [ -z "$private_key" ]; then
    return 0
  fi

  oci_dir="$RUNTIME_DIR/oci"
  mkdir -p "$oci_dir"
  chmod 700 "$oci_dir"
  key_file="$oci_dir/oci_api_key.pem"
  config_file="$oci_dir/config"

  printf '%s\n' "$private_key" | sed 's/\\n/\n/g' > "$key_file"
  chmod 600 "$key_file"

  umask 077
  cat > "$config_file" <<EOF
[${LESSONPT_OCI_PROFILE:-DEFAULT}]
user=${user_ocid}
fingerprint=${fingerprint}
tenancy=${tenancy}
region=${region}
key_file=${key_file}
EOF
  chmod 600 "$config_file"
  export LESSONPT_OCI_CONFIG_FILE="$config_file"
}

restore_wallet
write_oci_config

JAVA_OPTS="${JAVA_OPTS:--XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0 -XX:+ExitOnOutOfMemoryError}"
if [ -n "${TNS_ADMIN:-}" ]; then
  JAVA_OPTS="$JAVA_OPTS -Doracle.net.tns_admin=${TNS_ADMIN}"
fi
exec java $JAVA_OPTS -jar /app/app.jar
