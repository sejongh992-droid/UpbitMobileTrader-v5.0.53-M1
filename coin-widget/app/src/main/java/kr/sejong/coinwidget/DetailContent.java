package kr.sejong.coinwidget;
import android.content.Context;
import android.graphics.Bitmap;
import org.json.*;
import java.util.*;
final class DetailContent {
 static final class Result {String title,price="",text;Bitmap chart;Result(String title,String text){this.title=title;this.text=text;}}
 static final String GUIDE="① 위쪽에서 금일·09시 전·중장기 선택\n② 종목을 누르면 이유와 부족한 조건 확인\n③ 아래 목록으로 돌아오기\n\n매수 전에는 새로고침을 누르세요. 관찰은 차트 조건 미충족입니다. 5분이 지나도 목록은 유지되며 ‘이전 분석’으로 바뀝니다.\n\n09시 전은 선매수 후 09시 이후 상승 시 매도 검토 목적입니다. 08~09시 외에는 예비 목록이며 9시가 지나면 재조회해야 합니다.\n\n중장기는 1~3개월의 기술적 추세 분석입니다. 뉴스·사업가치·토큰 발행은 포함하지 않습니다.\n\n83점은 83% 상승 확률이 아닙니다. 조건점수는 정렬용이며 상승·수익을 보장하지 않습니다.\n\n앱은 홈 화면 앱 아이콘으로만 엽니다.";
 static String lack(JSONObject m,JSONObject a,int mode){
  if(m.optBoolean(mode==2?"long_defensive":"defensive",true))return mode==2?"BTC 일봉 약세·자료 확인 필요":m.optString("defense_reason","시장 약세");
  if(mode!=2&&!a.optString("pulse_risk").isEmpty())return a.optString("pulse_risk");
  return a.optString("risk","거래량·추세·가격 조건 재확인");
 }
 static Result forScreen(Context c,int id,int screen,JSONObject root,int mode,long now){
  JSONObject m=Renderer.obj(root,"market"),dom=Renderer.obj(root,"dominance");
  if(screen==Dashboard.CHECK)return new Result("검증 결과 · 쉬운 사용법",Evidence.TEXT+"\n\n"+GUIDE);
  if(screen==Dashboard.HELP)return new Result("사용법",GUIDE);
  if(screen==Dashboard.SETTINGS)return new Result("설정 · 데이터 확인","사용법: 위쪽 분류 → 종목 → 아래 목록\n\n자동 조회 15분 · 08:30/08:50 추가\n절전·통신 상태에 따라 늦어질 수 있습니다.\n\n"+FxData.explanation(root,now)+"\n\n매수 전 새로고침. 5분 뒤에는 이전 분석 표시.\n앱은 홈 화면 앱 아이콘으로 엽니다.");
  if(screen==Dashboard.BTC){Result d=new Result("BTC 일봉 확대","일봉 · 금색 20일 / 청록 60일 평균\n마지막 일봉은 진행 중일 수 있습니다. RSI·추세는 완료 봉만 분석합니다.");d.price=Charts.number(Renderer.obj(m,"btc").optDouble("price"))+"원";d.chart=MiniCharts.btc(Renderer.arr(m,"daily"));return d;}
  if(screen==Dashboard.DOM){
   double delta=dom.optDouble("delta24",Double.NaN);boolean fresh=!root.optBoolean("dom_failed")&&dom.optLong("time")>0&&now-dom.optLong("time")<=3*Signals.HOUR&&now>=dom.optLong("time")-60000;
   Result d=new Result("비트코인 비중",dom.optString("source","자료 없음")+" · "+Charts.date(dom.optLong("time"),"MM/dd HH:mm")+" KST"+(fresh?"":" · 이전 자료")+"\n"+(Signals.finite(delta)?String.format(Locale.KOREA,"약 24시간 %+.2f%%p",delta):"24시간 전 실측 자료 수집 중")+"\n비중 변화는 상승 확률이 아닙니다.");
   d.price=dom.has("value")?String.format(Locale.KOREA,"BTC %.2f%%",dom.optDouble("value")):"비중 조회 전";d.chart=Charts.dominance(Renderer.arr(dom,"history"));return d;
  }
  String code=Dashboard.prefs(c).getString("coin"+id,"");JSONArray list=Dashboard.list(m,mode);JSONObject a=null;
  for(int i=0;i<list.length();i++)if(code.equals(Renderer.obj(list.optJSONObject(i),"quote").optString("market"))){a=list.optJSONObject(i);break;}
  if(a==null)return new Result("목록에서 다시 선택","새 분석에서 순위가 바뀌었거나 자료가 부족한 종목입니다. 아래 목록을 눌러 다시 선택하세요.");
  JSONObject q=Renderer.obj(a,"quote");Result d=new Result(q.optString("name")+" ("+code.replace("KRW-","")+")","");d.price=Charts.number(q.optDouble("price"))+"원  "+Charts.pct(q.optDouble("day_pct"));
  boolean fresh=Dashboard.freshRecommendation(root,mode,now),ok=Dashboard.qualified(m,a,mode),entry=fresh&&ok&&(mode!=1||BeforeNine.entryWindow(now));
  String metrics=mode==2?"30일 "+Charts.pct(a.optDouble("return30",Double.NaN))+" · BTC 대비 "+Dashboard.f(a.optDouble("relative30",Double.NaN))+"%p\n7일 거래대금 "+Dashboard.f(a.optDouble("volume_ratio",Double.NaN))+"배 (이전 21일 대비)":
   "6시간 "+Charts.pct(a.optDouble("return6h",Double.NaN))+" · BTC 대비 "+Dashboard.f(a.optDouble("relative6h",Double.NaN))+"%p\n15분 "+Charts.pct(a.optDouble("pulse_change",Double.NaN))+" · 거래 "+Dashboard.f(a.optDouble("pulse_ratio",Double.NaN))+"배";
  d.text=Dashboard.label(root,a,mode,now)+"\n"+metrics+"\n\n";
  if(!fresh)d.text+="지금은 이전 분석입니다.\n새로고침 후 조건을 다시 확인하세요.";
  else if(!ok)d.text+="부족한 조건\n"+lack(m,a,mode)+"\n현재 매수 신호는 아닙니다.";
  else if(!entry)d.text+="다음 "+Charts.date(SnapshotState.nine(m),"MM/dd")+" 09시 예비 후보\n08~09시에 새로고침 후 재검토하세요.";
  else d.text+="눌림 검토 "+Charts.number(a.optDouble("entry_low"))+" ~ "+Charts.number(a.optDouble("entry_high"))+"원\n지지 유지·거래 증가 확인 후 검토\n무효 참고: "+Charts.number(a.optDouble("support"))+"원 저점 이탈\n저항 참고: "+Charts.number(a.optDouble("resistance"))+"원";
  d.text+="\n\n"+(mode==1?"9시 전 선매수 / 이후 상승 시 매도 검토\n":mode==2?"1~3개월 기술적 관찰 · 사업가치 미평가\n":"")+"차트 조건 선별 · 수익 우위 미확인\n점수 "+Math.round(a.optDouble(mode==1?"morning_score":"score"))+"/100 · 상승 확률 아님\n시세 "+Charts.date(q.optLong("time"),"MM/dd HH:mm:ss")+" KST\n가격은 조회 시점 값 · 상승 보장 없음";
  return d;
 }
}
