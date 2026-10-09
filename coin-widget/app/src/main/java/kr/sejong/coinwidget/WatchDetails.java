package kr.sejong.coinwidget;
import android.widget.LinearLayout;
import org.json.*;
final class WatchDetails {
 static void add(MainActivity a,LinearLayout out,JSONObject root){
  HelpUi.summary(a,out,root);JSONObject m=Renderer.obj(root,"market");
  out.addView(a.text("추천은 세 가지 목적입니다",19,true));
  out.addView(a.text("금일 추천: 오늘 장중 상승 흐름을 노리는 후보. 시간봉 상승 추세와 최근 15분 거래대금 증가를 확인합니다.\n\n09시 전: 9시 전에 매수해 9시 직후 급등 시 매도를 검토할 후보. 08:30~08:55 최신 분석으로 검토하며, 이 시간 밖에서는 다음 9시의 예비 후보입니다.\n\n중장기: 1~3개월 관점의 후보. 완료 일봉 20·60·120일 추세와 30·90일 BTC 대비 강도, 최근 7일 거래대금을 확인합니다. 사업 가치·토큰 발행 일정 등은 평가하지 않은 기술적 분석입니다.",14,false));
  out.addView(a.text("상단 위젯에서 종목을 누르면 선정 이유, 현재가 기준 지지·저항, 부족한 조건을 읽을 수 있습니다. 이전·다음으로 설명 페이지를 넘깁니다. 목록·시장·설정·새로고침도 위젯 안에서 이용합니다.",14,false));
  out.addView(a.text("검증 결과",18,true));out.addView(a.text(Evidence.TEXT,14,false));
  out.addView(a.text("조회 범위",18,true));
  out.addView(a.text("시세 수신 "+m.optInt("quote_count")+"개 / 시간봉 분석 "+m.optInt("inspected")+"개 / 단기 검증 "+m.optInt("pulse_checked")+"개 / 중장기 일봉 분석 "+m.optInt("long_inspected")+"개\n시간봉 통신 실패 "+m.optInt("failed")+" / 단기 실패 "+m.optInt("pulse_failed")+" / 중장기 실패 "+m.optInt("long_failed")+"\n분석 시작 "+Charts.date(m.optLong("quote_at"),"MM/dd HH:mm:ss")+" KST\n"+m.optString("screening_error"),13,false));
  out.addView(a.text("24시간 거래대금 상위 최대 30개 종목의 시간봉·5분봉·일봉를 분석합니다. 금일 최대 30개, 9시 전·중장기 최대 15개씩 표시하며, 분석 자료가 있는 종목은 조건 충족·관찰로 구분합니다. 차트 규칙 통과와 과거 수익 검증은 별도로 표시하며 평가한 통과 목록은 상위 5개입니다. 금일·중장기는 5분, 9시 전은 2분 뒤에도 목록은 남고 이전 분석으로 표시됩니다. 매수 전 재조회하세요. 자동 갱신은 15분 및 08:30·08:50이며 절전으로 지연될 수 있습니다.",13,false));
  out.addView(a.text("수익 검증 결과는 위의 실제 과거 평가 수치를 확인하세요. 점수는 상승 확률·9시 급등 확률이 아닙니다. 공개 시세의 기술적 조건을 선별합니다. 가격 구간은 과거 측정값이며 미래 수익률·체결가 보장이 아닙니다. 자동 매수·매도 기능은 없습니다.",13,false));
 }
}
