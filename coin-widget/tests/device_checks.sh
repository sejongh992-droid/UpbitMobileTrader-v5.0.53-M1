#!/usr/bin/env bash
set -euo pipefail
mkdir -p device-results
adb wait-for-device
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb install -r delivery/CoinMarketWidget-1.1.1-ci.apk | tee device-results/install-app.txt
adb install -r delivery/tests.apk | tee device-results/install-tests.txt
adb shell pm grant kr.sejong.coinwidget android.permission.POST_NOTIFICATIONS
adb logcat -c
set +e
adb shell am instrument -w -r -e class kr.sejong.coinwidget.StorageRenderTest,kr.sejong.coinwidget.WidgetHostTest,kr.sejong.coinwidget.UpgradeTest,kr.sejong.coinwidget.IntegrityTest kr.sejong.coinwidget.test/androidx.test.runner.AndroidJUnitRunner | tee device-results/runtime-tests.txt
adb shell am instrument -w -r -e class kr.sejong.coinwidget.LiveRefreshTest kr.sejong.coinwidget.test/androidx.test.runner.AndroidJUnitRunner | tee device-results/live-tests.txt
adb pull /sdcard/Android/data/kr.sejong.coinwidget/files/test-results device-results/
adb shell dumpsys package kr.sejong.coinwidget > device-results/package.txt
adb shell dumpsys jobscheduler > device-results/jobscheduler.txt
adb logcat -d > device-results/logcat.txt
set -e
grep -Eq 'OK \([0-9]+ tests?\)' device-results/runtime-tests.txt
grep -Eq 'OK \([0-9]+ tests?\)' device-results/live-tests.txt
! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' device-results/runtime-tests.txt device-results/live-tests.txt
! grep -q 'FATAL EXCEPTION' device-results/logcat.txt
echo 'PASS release APK install, deterministic device tests, and public-API refresh' | tee device-results/RESULT.txt
