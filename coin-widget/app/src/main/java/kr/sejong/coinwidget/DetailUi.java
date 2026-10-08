package kr.sejong.coinwidget;
import android.widget.LinearLayout;
import org.json.*;
import java.util.Locale;
final class DetailUi {
 static void add(MainActivity a,LinearLayout out,JSONObject r){
  JSONObject m=Renderer.obj(r,"market");out.addView(a.text("관찰 후보 상세",19,true));
  out.addView(a.text("조건점수는 상승 확률·예상 수익률이 아닙니다. 아래 저항선·저점은 과거 가격이며 도달을 보장하지 않습니다. 뉴스·거시경제·호가·체결 흐름은 이 버전에서 분석하지 않습니다.",13,false));
  if(!Renderer.freshMarket(r))out.addView(a.text("최신 시세가 없거나 마지막 조회가 실패했습니다. 새로운 후보 제시는 보류합니다.",14,false));
  else{
   JSONArray items=Renderer.arr(m,"candidates");
   if(items.length()==0)out.addView(a.text("현재 조건을 만족한 관찰 후보가 없습니다. 무리하게 종목을 채우지 않습니다.",14,false));
   for(int i=0;i<items.length();i++){
    JSONObject item=items.optJSONObject(i);if(item==null)continue;JSONObject q=Renderer.obj(item,"quote");String code=q.optString("market");
    double price=q.optDouble("price"),hi=item.optDouble("resistance"),lo=item.optDouble("support");
    out.addView(a.button(q.optString("name")+" ("+code.replace("KRW-","")+") · 업비트 차트",()->a.openChart(code)));
    String upper=hi>price?"최근 20시간 고점까지 "+Charts.pct((hi/price-1)*100):"최근 20시간 고점 돌파 구간 · 목표 수익률 미산정";
    String lower=lo<price?"최근 12시간 저점까지 "+Charts.pct((lo/price-1)*100):"현재가와 최근 저점의 위치 재확인 필요";
    String detail="조회 가격 "+Charts.number(price)+"원 / 전일 대비 "+Charts.pct(q.optDouble("day_pct"))
     +String.format(Locale.KOREA,"\n조건점수 %.0f/100 · RSI14 %.1f · 1시간 ATR %.2f%%",item.optDouble("score"),item.optDouble("rsi"),item.optDouble("atr_pct"))
     +String.format(Locale.KOREA,"\n완료 1시간 거래대금 %.2f배 · BTC 대비 6시간 %+.2f%%p",item.optDouble("volume_ratio"),item.optDouble("relative6h"))
     +"\n저항 참고 "+Charts.number(hi)+"원 / "+upper+"\n저점 참고 "+Charts.number(lo)+"원 / "+lower;
    out.addView(a.text(detail,13,false));
   }
  }
  out.addView(a.text("데이터와 판정 기준",19,true));
  out.addView(a.text("업비트 원화 마켓 현재가를 조회하고, BTC·지정 스테이블코인을 제외해 상승 종목 비율을 계산합니다.\n"
   +"관찰 후보는 유의·주의 종목 제외, 24시간 거래대금 50억 원 이상, 전일 대비 -4~+20% 범위에서 1차 선별합니다. 순위 상위 최대 8개만 1시간 봉을 추가 분석합니다.\n"
   +"완료된 봉의 20·60 이동평균, RSI14, ATR14, 거래대금 배율, BTC 대비 6시간 강도를 사용합니다. 거래 없는 시간 간격·시세 지연·과열 조건이면 제외합니다.\n"
   +"내일 09시 항목은 현재 후보 중 RSI≤68, BTC 대비 6시간 강도≥0인 종목을 재확인 목록으로 표시합니다. 내일 오를 종목을 미리 맞히는 모델이 아닙니다.\n"
   +"설치 후 도미넌스를 2시간 구간별로 기록합니다. 약24시간 변화는 24시간 전±3시간 이내 실측 자료가 있을 때만 표시합니다. 데이터 공급원을 바꾸면 기록이 새로 시작됩니다.\n"
   +"시장 국면은 단순 규칙의 요약입니다. 매수 판단·자동 주문·체결 보장 기능이 없습니다. 백테스트·실전 수익률 검증은 하지 않았습니다.",13,false));
  out.addView(a.text("이번 조회: 1차 대상 "+m.optInt("screened")+"종목 / 시간봉 응답 확인 "+m.optInt("inspected")+"종목\n"+m.optString("screening_error"),12,false));
  JSONObject dom=Renderer.obj(r,"dominance"),fx=Renderer.obj(r,"fx");
  String sources="가격·캔들: Upbit · "+Charts.date(m.optLong("fetched_at"),"yyyy-MM-dd HH:mm:ss")+" KST"
   +"\n도미넌스: "+dom.optString("source","CoinPaprika")+" · 공급자 기준 "+Charts.date(dom.optLong("time"),"MM/dd HH:mm:ss")+" KST"
   +"\n환율: Frankfurter / ECB · "+fx.optString("date","자료 없음")+" 기준 · 실시간 환전 가격 아님"
   +"\n최근 조회 시도: "+Charts.date(r.optLong("last_attempt"),"MM/dd HH:mm:ss")+" KST";
  JSONArray errors=Renderer.arr(r,"errors");for(int i=0;i<errors.length();i++)sources+="\n오류: "+errors.optString(i);out.addView(a.text(sources,12,false));
  out.addView(a.text("2시간 주기는 Android의 작업 예약입니다. 절전·인터넷 단절·앱 강제 종료 시 늦어질 수 있습니다. 새로고침은 누를 때 즉시 조회를 시작하며 통신 시간이 필요합니다. 화면 가격은 마지막 조회 시점 값입니다.\n\n업비트 개인 키는 필요하지 않습니다. 선택 키는 CoinGecko Demo 전용이며 기기 내 암호화 저장 후 CoinGecko로만 전송합니다. 계좌·잔고·주문·출금 API는 사용하지 않습니다. 광고·위치·연락처 수집 코드는 포함하지 않았습니다.",12,false));
 }
}
