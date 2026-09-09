#!/bin/sh
#
# Runs the pinned ktlint CLI, downloading it into a user-level cache on first use.
# All arguments are forwarded to ktlint as-is.

set -u

KTLINT_VERSION="1.8.0"
KTLINT_SHA256="a3fd620207d5c40da6ca789b95e7f823c54e854b7fade7f613e91096a3706d75"
KTLINT_URL="https://github.com/pinterest/ktlint/releases/download/${KTLINT_VERSION}/ktlint"

cache_dir="${XDG_CACHE_HOME:-$HOME/.cache}/ktlint"
jar="$cache_dir/ktlint-${KTLINT_VERSION}.jar"

sha256_of() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    echo ""
  fi
}

if [ ! -f "$jar" ]; then
  if ! command -v curl >/dev/null 2>&1; then
    echo "ktlint: curl not found, skipping the style check." >&2
    exit 0
  fi

  mkdir -p "$cache_dir" || exit 0
  tmp="$jar.download.$$"

  echo "ktlint: downloading $KTLINT_VERSION (one time, ~68 MB)..." >&2
  if ! curl -fSL --retry 2 -o "$tmp" "$KTLINT_URL"; then
    rm -f "$tmp"
    echo "ktlint: download failed, skipping the style check." >&2
    exit 0
  fi

  actual=$(sha256_of "$tmp")
  if [ -z "$actual" ]; then
    echo "ktlint: no sha256 tool available, cannot verify the download; skipping." >&2
    rm -f "$tmp"
    exit 0
  fi
  if [ "$actual" != "$KTLINT_SHA256" ]; then
    rm -f "$tmp"
    echo "ktlint: checksum mismatch for $KTLINT_URL" >&2
    echo "  expected $KTLINT_SHA256" >&2
    echo "  actual   $actual" >&2
    exit 1
  fi

  mv "$tmp" "$jar"
fi

if ! command -v java >/dev/null 2>&1; then
  echo "ktlint: java not found, skipping the style check." >&2
  exit 0
fi

exec java -jar "$jar" --relative "$@"
