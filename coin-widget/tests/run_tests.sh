#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="$(mktemp -d)"; trap 'rm -rf "$OUT"' EXIT
if command -v javac >/dev/null; then JAVAC=(javac); else JAVAC=(java -m jdk.compiler/com.sun.tools.javac.Main); fi
"${JAVAC[@]}" --release 11 -encoding UTF-8 -d "$OUT" app/src/main/java/kr/sejong/coinwidget/Signals.java app/src/main/java/kr/sejong/coinwidget/Research.java app/src/main/java/kr/sejong/coinwidget/BeforeNine.java app/src/main/java/kr/sejong/coinwidget/LongerTerm.java app/src/main/java/kr/sejong/coinwidget/StrategyV130.java tests/StrategyV130Test.java tests/StrategyTest.java tests/SignalsTest.java tests/ParseJava.java
java -cp "$OUT" SignalsTest
java -cp "$OUT" kr.sejong.coinwidget.StrategyTest
java -cp "$OUT" kr.sejong.coinwidget.StrategyV130Test
java -cp "$OUT" ParseJava app/src/main/java
python3 tests/static_checks.py
