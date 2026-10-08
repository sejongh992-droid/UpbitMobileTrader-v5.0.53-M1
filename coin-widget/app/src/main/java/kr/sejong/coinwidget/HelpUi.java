package kr.sejong.coinwidget;
import android.app.AlertDialog;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.json.JSONObject;
final class HelpUi {
 static final String GUIDE =
  "① 먼저 시간부터 확인하세요\n"
 +"가격은 새로고침 때 받은 업비트 원화마켓의 최종 체결가입니다. 초 단위 실시간 화면은 아닙니다. 아래 시각은 BTC 체결 시각이며, 개별 종목 시각은 세부 정보에서 확인할 수 있습니다. 2시간 자동 갱신은 절전 때문에 늦어질 수 있습니다.\n\n"
 +"② ‘09시 기준’은 무엇인가요?\n"
 +"업비트 전일 대비는 한국시간 오전 9시(UTC 0시)에 바뀌는 전일 종가 기준입니다. 밤 12시 기준이나 최근 24시간 수익률과 다릅니다. ‘6시간’은 완료된 시간봉 종가끼리 비교합니다. 따라서 실시간 가격 변화와 조금 다를 수 있습니다.\n\n"
 +"③ 시장을 어떻게 판단하나요?\n"
 +"BTC 현재가가 완료 일봉 20일 평균 위에 있고, 20일 평균이 60일 평균 위면 ‘상승 우세’입니다. 반대면 ‘하락 우세’, 그 사이면 ‘조정·혼조’입니다. 알트 상승 비율은 최근 체결을 확인한 원화 알트 중 09시 기준으로 오른 종목의 비율입니다. BTC와 지정 스테이블코인은 제외합니다. 전체 중 몇 종목이 계산됐는지도 아래에 표시합니다.\n\n"
 +"④ 도미넌스는 돈의 이동량인가요?\n"
 +"아닙니다. 전체 가상자산 시가총액에서 BTC가 차지하는 비율입니다. 예를 들어 56%면 전체 시가총액을 100으로 봤을 때 BTC 몫이 56입니다. 56%→55%는 -1%p입니다. 비중 감소만으로 알트 가격 상승이나 신규 자금 유입을 단정할 수 없습니다. 기타 코인에는 스테이블코인도 포함됩니다.\n\n"
 +"⑤ 왜 24시간 비교가 수집 중인가요?\n"
 +"앱이 설치 후 실제로 모은 기록만 사용하기 때문입니다. 같은 공급자의 약 24시간 전 기록(21~27시간 범위)이 있어야 변화량을 냅니다. 공급자를 바꾸면 과거 기록은 섞지 않습니다. 표시된 환율은 ECB 일별 기준 환율이며 실시간 환전 가격이 아닙니다.\n\n"
 +"⑥ 추천·관심 목록은 어떻게 고르나요?\n"
 +"전체 원화 시세를 받고 유의·주의 종목, 스테이블코인, 오래된 체결을 제외합니다. 거래대금·BTC 대비 강도로 1차 순위를 정해 최대 60개를 시간봉으로 분석하고, 최대 30개를 표시합니다. ‘조건 충족’은 조회 당시 규칙을 통과했다는 뜻입니다. ‘관찰 대기’는 아직 부족한 조건이 있습니다. 개수를 채우려고 조건 미달을 추천으로 바꾸지 않습니다.\n\n"
 +"⑦ 점수·상대6h·거래대금은 무엇인가요?\n"
 +"83점은 83% 상승 확률이 아닙니다. 추세·상대강도·거래대금·과열 정도로 매긴 순위 점수입니다. 코인이 6시간 +3%, BTC가 +1%면 상대6h는 +2%p입니다. 거래대금 1.5배는 직전 완료 1시간 거래대금이 그 이전 20시간 평균보다 50% 많다는 뜻입니다.\n\n"
 +"⑧ RSI·ATR·가격 구간은 어떻게 읽나요?\n"
 +"RSI는 최근 상승과 하락 강도의 균형입니다. 높다고 반드시 계속 오르는 것은 아닙니다. ATR은 최근 가격 움직임의 크기이며, 값이 클수록 변동성이 큽니다. 참고 가격 구간은 20시간 평균과 ATR로 계산한 관찰 범위입니다. 최근 저점·고점은 과거 측정값이지 보장된 손절가·목표가가 아닙니다. 거리비에는 수수료와 미끄러짐이 빠져 있습니다.\n\n"
 +"⑨ ‘09시 재확인’은 내일 급등 예측인가요?\n"
 +"아닙니다. 다음 한국시간 09시를 점검 시한으로 잡고, 현재 흐름 중 추가 조건을 만족하는 종목을 우선 보여줍니다. 09시가 지나면 다시 조회해야 합니다. 예정 시각에 자동 매수하거나 상승을 예측하는 기능은 없습니다.\n\n"
 +"⑩ 가장 쉬운 확인 순서\n"
 +"새로고침 → 시장이 방어 상태인지 확인 → 조건 충족 후보의 이유·주의점 확인 → 종목을 눌러 업비트 최신 차트 확인 순서입니다. API 오류나 자료 부족이면 보류합니다. 분석은 공개 시세 기반 규칙이며 뉴스·호가·실전 수익성 검증은 포함하지 않습니다.";
 static AlertDialog show(MainActivity a){ScrollView scroll=new ScrollView(a);scroll.setPadding(a.dp(20),a.dp(8),a.dp(20),a.dp(8));scroll.addView(a.text(GUIDE,15,false));return new AlertDialog.Builder(a).setTitle("화면 읽는 법 · 쉬운 설명").setView(scroll).setPositiveButton("확인",null).show();}
 static void summary(MainActivity a,LinearLayout out,JSONObject root){
  JSONObject m=Renderer.obj(root,"market"),dom=Renderer.obj(root,"dominance");
  out.addView(a.text("간단 분석 · 지금 볼 세 가지",19,true));
  if(!Renderer.freshMarket(root)||m.optInt("schema")<3){out.addView(a.text("최신 자료를 확인하지 못했습니다. 새로고침 후 분석과 후보를 확인하세요.",14,false));return;}
  String btc=m.optString("btc_regime","판단 보류");double breadth=m.optDouble("breadth",Double.NaN);
  String action=btc.startsWith("판단 보류")||!Signals.finite(breadth)?"자료 부족으로 판단 보류":m.optBoolean("defensive",true)?m.optString("defense_reason","시장 자료 확인 필요")+". 관찰을 우선합니다.":breadth>=65?"상승 종목이 많습니다. 과열 여부를 함께 확인하세요.":"종목별 흐름이 갈립니다. 조건 충족 후보부터 비교하세요.";
  out.addView(a.text("1. BTC 추세: "+btc+"\n2. 알트 상승: "+Dashboard.f(breadth)+"% (09시 기준)\n3. 해석: "+action,14,false));
  out.addView(a.text("조회 당시 조건 충족 "+Renderer.arr(m,"candidates").length()+"개 / 관심 목록 "+Renderer.arr(m,"watchlist").length()+"개. 후보별 선정 이유와 주의점을 아래에서 확인하세요.",13,false));
  long age=System.currentTimeMillis()-m.optLong("quote_at");
  out.addView(a.text("분석 시각 "+Charts.date(m.optLong("quote_at"),"MM/dd HH:mm:ss")+" KST"+(age>900000?" · 15분 경과, 재조회 권장":" · 조회 당시 평가"),12,false));
 }
}
