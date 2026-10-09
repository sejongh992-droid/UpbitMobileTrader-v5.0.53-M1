package kr.sejong.coinwidget;
/** Replaced with the recorded evaluation outcome before release. Never inferred from chart score. */
final class ValidationPolicy {
 static boolean approved(int mode){return false;}
 static String state(int mode){return approved(mode)?"후향 평가 통과":"수익 검증 보류";}
 static String reason(int mode){return "별도 기간 평가 결과를 확인하세요. 점수는 상승 확률이 아닙니다.";}
}
