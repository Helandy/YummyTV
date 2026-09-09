#!/bin/sh

set -eu

repo_root=$(git rev-parse --show-toplevel 2>/dev/null) || {
  echo "install-git-hooks: this script must be run inside a Git repository." >&2
  exit 1
}

cd "$repo_root"

for hook_file in ".githooks/commit-msg" ".githooks/pre-commit" ".githooks/ktlint.sh"; do
  if [ ! -f "$hook_file" ]; then
    echo "install-git-hooks: missing $hook_file." >&2
    exit 1
  fi
  chmod +x "$hook_file"
done

git config core.hooksPath .githooks

echo "Git hooks installed: core.hooksPath=.githooks"
