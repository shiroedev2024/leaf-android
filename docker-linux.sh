#!/bin/bash
set -euo pipefail

HOST_CACHE_DIR="$(pwd)/.cache"
GRADLE_HOST_DIR="$HOST_CACHE_DIR/gradle"
CARGO_REGISTRY_HOST_DIR="$HOST_CACHE_DIR/cargo/registry"
CARGO_GIT_HOST_DIR="$HOST_CACHE_DIR/cargo/git"

mkdir -p "$GRADLE_HOST_DIR" "$CARGO_REGISTRY_HOST_DIR" "$CARGO_GIT_HOST_DIR"

docker run -it --rm \
  -v "$GRADLE_HOST_DIR":/home/android/.gradle \
  -v "$CARGO_REGISTRY_HOST_DIR":/home/android/.cargo/registry \
  -v "$CARGO_GIT_HOST_DIR":/home/android/.cargo/git \
  -v "$(pwd)":/home/android/app \
  -e GRADLE_USER_HOME=/home/android/.gradle \
  -e CARGO_HOME=/home/android/.cargo \
  --user "$(id -u):$(id -g)" \
  --workdir /home/android/app \
  registry.gitlab.com/mokhtarabadi/android-rust-toolchain:1.7.4 \
  /bin/bash -c "./build.sh apk --publish"
