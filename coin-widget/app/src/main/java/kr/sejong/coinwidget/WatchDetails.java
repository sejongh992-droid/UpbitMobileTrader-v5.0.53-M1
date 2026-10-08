package kr.sejong.coinwidget;
import android.content.*;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;
import org.json.*;
import java.util.Locale;
final class WatchDetails {
 static void link(MainActivity a,String url){try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){a.toast("브라우저를 확인해 주세요.");}}
 static void add(MainActivity a,LinearLayout out,JSONObject root){
  JSONObject m=Renderer.obj(root,"market"),dom=Renderer.obj(root,"dominance");int mode=Dashboard.mode(a,0);
  out.addView(a.text(mode==1?"09시 전 매수 검토 · 선정 근거":"오늘 후보 · 전체 순위와 근거",19,true));
  long deadline=m.optLong("recheck_at");boolean fresh=Renderer.freshMarket(root)&&m.optInt("schema")>=2;
  out.addView(a.text("전체 원화 시세 → 최대 60종목 시간봉 분석 → 오늘 30개 / 09시 전 15개까지 표시합니다. 실제 검토 조건을 통과한 종목과 추가 관찰 종목은 구분합니다.",13,false));
  if(mode==1)out.addView(a.text(Charts.date(deadline,"MM/dd HH:mm")+" KST 이전 검토용. 현재 흐름을 평가하며, 내일 급등을 예측하는 모델은 아닙니다. 매수 검토는 참고 구간 도달 후 지지 재확인 조건입니다.",13,false));
  JSONArray list=fresh&&!(mode==1&&System.currentTimeMillis()>=deadline)?Dashboard.list(m,mode):new JSONArray();
  if(list.length()==0)out.addView(a.text("후보가 없거나 유효 시간이 지났습니다. 새로고침으로 다시 확인하세요.",14,false));
  for(int i=0;i<list.length();i++){
   JSONObject x=list.optJSONObject(i),q=Renderer.obj(x,"quote");String code=q.optString("market"),symbol=code.replace("KRW-","");boolean qualified=x.optBoolean(mode==1?"pre_qualified":"qualified");
   LinearLayout card=new LinearLayout(a);card.setOrientation(1);card.setPadding(a.dp(12),a.dp(9),a.dp(12),a.dp(10));
   GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(a.dp(16));bg.setStroke(a.dp(1),0xffe7e9f1);card.setBackground(bg);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=a.dp(9);out.addView(card,lp);
   card.addView(a.text((i+1)+". "+q.optString("name",symbol)+" ("+symbol+")",17,true));
   TextView price=a.text(Charts.number(q.optDouble("price"))+"원  "+Charts.pct(q.optDouble("day_pct")),16,true);price.setTextColor(q.optDouble("day_pct")>=0?Charts.UP:Charts.DOWN);card.addView(price);
   TextView state=a.text(Math.round(x.optDouble(mode==1?"morning_score":"score"))+"점 · "+x.optString(mode==1?"pre_state":"state"),14,true);state.setTextColor(qualified?Dashboard.PURPLE:Dashboard.MUTED);card.addView(state);
   card.addView(a.text(x.optString("reason")+String.format(Locale.KOREA,"\nRSI %.1f · 시간봉 ATR %.2f%% · 12h %+.2f%%",x.optDouble("rsi"),x.optDouble("atr_pct"),x.optDouble("return12h")),13,false));
   double low=x.optDouble("entry_low"),high=x.optDouble("entry_high"),support=x.optDouble("support"),res=x.optDouble("resistance");
   if(support<low&&low>0&&res>high){
    card.addView(a.text("눌림 참고 구간  "+Charts.number(low)+" ~ "+Charts.number(high)+"원\n현재 매수 지시 아님 · 도달 후 지지/거래대금 재확인",13,true));
    card.addView(a.text("최근 20시간 고점  "+Charts.number(res)+"원\n최근 12시간 저점  "+Charts.number(support)+"원 (이탈 시 무효 참고)\n중간 진입가 가정 거리비 "+Dashboard.f(x.optDouble("reference_rr"))+" : 1 · 수수료/슬리피지 미포함",12,false));
   }else card.addView(a.text("참고 진입 구간 산정 보류: 지지·저항 위치가 불충분합니다.",13,false));
   card.addView(a.text("주의: "+x.optString("risk")+(qualified?"":"\n아직 매수 검토 조건을 충족하지 않았습니다."),12,false));
   card.addView(a.button("업비트에서 "+q.optString("name",symbol)+" 차트 보기",()->a.openChart(code)));
  }
  out.addView(a.text("비트코인 · 도미넌스 해석",19,true));
  JSONObject t=Renderer.obj(m,"btc_technical");out.addView(a.text("비트코인: "+m.optString("btc_regime","자료 부족")+"\n20일 평균 "+Charts.number(t.optDouble("ma20"))+"원 / 60일 평균 "+Charts.number(t.optDouble("ma60"))+"원\n일봉 RSI14 "+Dashboard.f(t.optDouble("rsi"))+" · 최근 6시간 "+Charts.pct(t.optDouble("return6h")),13,false));
  double delta=dom.optDouble("delta24",Double.NaN);String meaning=!Signals.finite(delta)?"24시간 전 실측 자료가 없어 비중 방향 판정은 보류합니다.":delta<-.2?"BTC 비중 감소: 기타 코인의 상대 비중 확대. 신규 자금 유입을 뜻하지는 않습니다.":delta>.2?"BTC 비중 증가: BTC 상대 비중 확대. 비트코인 가격 상승을 보장하지 않습니다.":"BTC 비중 변화가 작습니다. 알트 상승 종목 비율을 함께 확인하세요.";
  out.addView(a.text(meaning+"\n기타 코인 비중에는 스테이블코인이 포함됩니다. 원화 알트 상승 비율은 BTC·지정 스테이블을 제외합니다. 서로 다른 지표입니다.",13,false));
  JSONArray h=Renderer.arr(dom,"history");if(h.length()>=2){ImageView chart=new ImageView(a);chart.setImageBitmap(Charts.dominance(h));chart.setAdjustViewBounds(true);out.addView(chart);}
  out.addView(a.text("앱 그래프는 "+dom.optString("source","CoinPaprika")+"의 실제 관측 기록 "+h.length()+"개입니다. 과거 값을 임의 생성하지 않습니다.",12,false));
  out.addView(a.button("도미넌스 과거 차트 열기 (TradingView)",()->link(a,"https://www.tradingview.com/symbols/BTC.D/")));
  out.addView(a.text("외부 BTC.D 차트는 TradingView 기준으로 공급자와 산정 범위가 달라 앱 수치와 다를 수 있습니다.",12,false));
  out.addView(a.text("조회 범위 · 정확성 안내",19,true));
  out.addView(a.text("1차 후보 "+m.optInt("screened")+"개 / 시간봉 응답 "+m.optInt("inspected")+"개 / 간격·자료 부족 제외 "+m.optInt("invalid")+"개 / 통신 실패 "+m.optInt("failed")+"개\n시세 기준 "+Charts.date(m.optLong("quote_at"),"MM/dd HH:mm:ss")+" KST\n"+m.optString("screening_error"),12,false));
  out.addView(a.text("분석: 24시간 거래대금 10억 이상, -12~+30% 범위의 유의·주의 제외 종목 중 상위 60개. 완료 60시간 연속 봉, 이동평균·RSI·ATR·BTC 대비 강도·거래대금을 검사합니다. 매수 검토는 더 엄격한 조건(50억 이상 등) 및 시장 방어 조건을 적용합니다.\n\n09시 전 검토는 RSI 45~66, 12h 상승, BTC 대비 6h 강도 ≥0, 거래대금 배율 ≥0.9, ATR ≤3.5%, 과거 지지·저항 거리비 ≥1.3 등의 조건을 추가합니다. 계산된 참고값은 미래 체결 가격·수익률이 아닙니다.\n\n점수는 조건 우선순위이며 확률이 아닙니다. 수익성 백테스트·뉴스·실시간 호가 분석·자동주문은 없습니다. 진입 전 최신 조회가 필요합니다. 절전·통신장애로 2시간 예약은 늦어질 수 있습니다.",12,false));
 }
}
