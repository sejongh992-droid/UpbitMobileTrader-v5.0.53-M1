#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="$(mktemp -d)"; trap 'rm -rf "$OUT"' EXIT
javac --release 11 -encoding UTF-8 -d "$OUT" app/src/main/java/kr/sejong/coinwidget/Signals.java tests/SignalsTest.java tests/ParseJava.java
java -cp "$OUT" SignalsTest
java -cp "$OUT" ParseJava app/src/main/java
python3 tests/static_checks.py
