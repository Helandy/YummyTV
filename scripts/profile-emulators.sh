#!/bin/sh
# Поднимает и гасит AVD для генерации baseline profile и бенчмарков.
#
#   profile-emulators.sh start <state-file> <avd> [<avd>...]
#   profile-emulators.sh stop  <state-file>
#
# start пропускает уже запущенные AVD и ждёт полной загрузки каждого;
# в <state-file> пишутся serial только тех эмуляторов, которые поднял сам скрипт —
# stop гасит только их, чужие (запущенные вручную) не трогает.
# SDK берётся из ANDROID_SDK_ROOT. PROFILE_EMULATOR_WINDOW=1 — показывать окна эмуляторов.

set -eu

sdk=${ANDROID_SDK_ROOT:?"ANDROID_SDK_ROOT не задан"}
adb="$sdk/platform-tools/adb"
emulator="$sdk/emulator/emulator"
boot_timeout_s=300

running_serials() {
  "$adb" devices | awk '/^emulator-[0-9]+\tdevice$/ { print $1 }'
}

avd_name_of() {
  "$adb" -s "$1" emu avd name 2>/dev/null | head -1 | tr -d '\r'
}

serial_of_avd() {
  for serial in $(running_serials); do
    if [ "$(avd_name_of "$serial")" = "$1" ]; then
      echo "$serial"
      return 0
    fi
  done
  return 1
}

wait_booted() {
  serial=$1
  elapsed=0
  until [ "$("$adb" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    sleep 3
    elapsed=$((elapsed + 3))
    if [ "$elapsed" -ge "$boot_timeout_s" ]; then
      echo "profile-emulators: $serial не загрузился за ${boot_timeout_s}s" >&2
      exit 1
    fi
  done
  # экран блокировки мешает UiAutomator
  "$adb" -s "$serial" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
  "$adb" -s "$serial" shell wm dismiss-keyguard >/dev/null 2>&1 || true
}

start() {
  state_file=$1
  shift
  mkdir -p "$(dirname "$state_file")"
  : >"$state_file"
  "$adb" start-server >/dev/null

  for avd in "$@"; do
    if existing=$(serial_of_avd "$avd"); then
      echo "profile-emulators: $avd уже запущен ($existing)"
      continue
    fi
    if ! "$emulator" -list-avds | grep -qx "$avd"; then
      echo "profile-emulators: AVD '$avd' не найден. Доступные:" >&2
      "$emulator" -list-avds >&2
      exit 1
    fi
    echo "profile-emulators: запускаю $avd"
    window_flag="-no-window"
    [ "${PROFILE_EMULATOR_WINDOW:-0}" = "1" ] && window_flag=""
    # shellcheck disable=SC2086
    nohup "$emulator" -avd "$avd" -no-snapshot-save -no-boot-anim $window_flag >/dev/null 2>&1 &

    serial=""
    elapsed=0
    until serial=$(serial_of_avd "$avd"); do
      sleep 3
      elapsed=$((elapsed + 3))
      if [ "$elapsed" -ge "$boot_timeout_s" ]; then
        echo "profile-emulators: $avd не появился в adb за ${boot_timeout_s}s" >&2
        exit 1
      fi
    done
    echo "$serial" >>"$state_file"
    wait_booted "$serial"
    echo "profile-emulators: $avd готов ($serial)"
  done

  for serial in $(running_serials); do
    wait_booted "$serial"
  done
}

stop() {
  state_file=$1
  [ -f "$state_file" ] || exit 0
  while read -r serial; do
    [ -n "$serial" ] || continue
    echo "profile-emulators: гашу $serial"
    "$adb" -s "$serial" emu kill >/dev/null 2>&1 || true
  done <"$state_file"
  rm -f "$state_file"
}

command=${1:-}
[ $# -gt 0 ] && shift
case "$command" in
  start) start "$@" ;;
  stop) stop "$@" ;;
  *)
    echo "usage: $0 start <state-file> <avd>... | stop <state-file>" >&2
    exit 2
    ;;
esac
