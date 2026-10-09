package kr.sejong.coinwidget;
/** Generated from the frozen-rule, date-separated retrospective evaluation. */
final class ValidationPolicy {
 static boolean approved(int mode){switch(mode){case 0:return false;case 1:return false;case 2:return false;default:return false;}}
 static String state(int mode){return approved(mode)?"후향 평가 통과":"수익 검증 보류";}
 static String reason(int mode){switch(mode){case 0:return "새 엄격 조건 통과 사례가 없어 수익성을 평가할 수 없습니다. 관찰 순위를 매수 추천으로 보지 마세요.";case 1:return "새 엄격 조건 통과 사례가 없어 수익성을 평가할 수 없습니다. 관찰 순위를 매수 추천으로 보지 마세요.";case 2:return "새 엄격 조건 통과 사례가 없어 수익성을 평가할 수 없습니다. 관찰 순위를 매수 추천으로 보지 마세요.";default:return "분류 확인 필요";}}
}
