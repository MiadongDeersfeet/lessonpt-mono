#!/bin/sh
# Render 컨테이너에서 Wallet과 OCI config를 런타임에만 복원한 뒤 기동한다.
# 인증값과 파일 내용은 출력하지 않는다.

set -eu

RUNTIME_DIR="${LESSONPT_RUNTIME_DIR:-/var/lessonpt}"
mkdir -p "$RUNTIME_DIR"
chmod 700 "$RUNTIME_DIR" 2>/dev/null || true

restore_wallet() {
  if [ -n "${TNS_ADMIN:-}" ] && [ -f "${TNS_ADMIN}/tnsnames.ora" ]; then
    return 0
  fi

  wallet_dir="${LESSONPT_ORACLE_WALLET_DIR:-$RUNTIME_DIR/wallet}"
  archive="${LESSONPT_ORACLE_WALLET_ARCHIVE:-}"
  encoded="${LESSONPT_ORACLE_WALLET_B64:-}"

  if [ -z "$archive" ] && [ -z "$encoded" ] && [ ! -f "$wallet_dir/tnsnames.ora" ]; then
    return 0
  fi

  mkdir -p "$wallet_dir"
  chmod 700 "$wallet_dir"

  if [ -n "$archive" ]; then
    unzip -o -q "$archive" -d "$wallet_dir"
  elif [ -n "$encoded" ]; then
    tmp="$(mktemp)"
    printf '%s' "$encoded" | base64 -d > "$tmp"
    unzip -o -q "$tmp" -d "$wallet_dir"
    rm -f "$tmp"
  fi

  if [ ! -f "$wallet_dir/tnsnames.ora" ]; then
    nested="$(find "$wallet_dir" -name tnsnames.ora -type f -print -quit 2>/dev/null || true)"
    if [ -n "$nested" ]; then
      wallet_dir="$(dirname "$nested")"
    fi
  fi

  chmod 700 "$wallet_dir"
  find "$wallet_dir" -type f -exec chmod 600 {} \;
  export TNS_ADMIN="$wallet_dir"
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
exec java $JAVA_OPTS -jar /app/app.jar
