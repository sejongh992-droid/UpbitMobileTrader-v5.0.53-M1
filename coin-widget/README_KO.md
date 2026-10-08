# 코인 시장 위젯 1.1.1

패키지 kr.sejong.coinwidget / versionCode 4 / minSdk 26 / compile·targetSdk 35.

## 빌드
JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3, SDK 35.
coin-widget 폴더에서 `gradle :app:assembleRelease :app:assembleReleaseAndroidTest :app:lintRelease`.
원본 Java·XML을 그대로 빌드합니다. 소스 변환 스크립트가 필요하지 않습니다.
`bash tests/run_tests.sh`는 순수 계산 검사 및 소스·리소스 검사를 수행합니다.
CI 인증서는 검사 전용입니다. 배포 APK는 기존 비공개 업데이트 인증서로 별도 서명합니다. 개인 키를 저장소에 올리지 마세요.

## 표시와 해석
업비트 원화 시세, 일봉, 완료 시간봉에 근거한 조회 전용 앱입니다. 주문 기능은 없습니다.
오늘 관심 목록 최대 30개, 다음 09시 재확인 최대 15개. 실제 분석은 1차 선별 상위 최대 60개입니다.
관찰 대기와 조건 충족은 다릅니다. 조회 당시 조건을 통과해야 조건 충족으로 표시합니다.
점수는 순위용 규칙 점수이며 상승 확률 또는 수익 보장이 아닙니다.
앱의 ‘화면 읽는 법 · 쉬운 설명’에서 지표와 사용 순서를 확인할 수 있습니다.

## 데이터 검사
현재가와 전일 종가로 등락률을 재계산해 API 등락률과 대조합니다.
중복·누락 시세, 음수 거래대금, 불완전 시간봉, 오래된 봉은 배제합니다.
전일 대비는 KST 09시 기준입니다. 24시간 거래대금과 기간이 다릅니다.
BTC 일봉 추세 확인 불가·일봉 하락·알트 상승비율 35% 미만·BTC 직전 완료 시간봉 1.2% 초과 하락 시 방어 관찰합니다.
도미넌스는 CoinPaprika 기본, CoinGecko 선택이며 동일 공급자 실측 기록만 비교합니다.
약24시간 변화는 21~27시간 전 가장 가까운 관측과의 %p 차이입니다.
환율은 Frankfurter/ECB 일별 기준 환율로 현재 환전 시세가 아닙니다.

## 공식 자료
https://docs.upbit.com/kr/reference/list-quote-tickers
https://docs.upbit.com/kr/reference/list-candles-minutes
https://docs.coinpaprika.com/api-reference/global/get-market-overview-data
https://frankfurter.dev/
https://developer.android.com/develop/ui/views/appwidgets/advanced

## 검사 범위
합성 계산 검사는 수익성 백테스트가 아닙니다. Android 실행 검사의 수치 및 실물 단말 미확인 사항은 전달 점검결과를 따릅니다.
