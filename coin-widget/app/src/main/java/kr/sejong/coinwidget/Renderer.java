package kr.sejong.coinwidget;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;
import org.json.*;
import java.util.*;
final class Renderer {
 static JSONObject obj(JSONObject o,String key){JSONObject r=o==null?null:o.optJSONObject(key);return r==null?new JSONObject():r;}
 static JSONArray arr(JSONObject o,String key){JSONArray r=o==null?null:o.optJSONArray(key);return r==null?new JSONArray():r;}
 static boolean freshMarket(JSONObject r){JSONObject m=obj(r,"market");long t=m.optLong("fetched_at"),age=System.currentTimeMillis()-t;return t>0&&age>=-60_000&&age<=3*Signals.HOUR&&!r.optBoolean("market_failed",false);}
 static PendingIntent activity(Context c,int id,Intent i){
  int flags=PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE;
  if(Build.VERSION.SDK_INT>=35){ActivityOptions o=ActivityOptions.makeBasic();o.setPendingIntentCreatorBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);return PendingIntent.getActivity(c,id,i,flags,o.toBundle());}
  return PendingIntent.getActivity(c,id,i,flags);
 }
 static RemoteViews build(Context c,boolean compact){
  JSONObject r=Repository.load(c),m=obj(r,"market"),btc=obj(m,"btc"),dom=obj(r,"dominance"),fx=obj(r,"fx");
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget_market);
  if(fx.has("rate"))v.setTextViewText(R.id.fx,"1 USD = "+Charts.number(fx.optDouble("rate"))+"원\n"+fx.optString("date")+" 기준"+(r.optBoolean("fx_failed")?" · 이전 자료":""));
  if(btc.has("price")){
   v.setTextViewText(R.id.btc_price,"BTC  "+Charts.number(btc.optDouble("price"))+"원");
   v.setTextColor(R.id.btc_price,btc.optDouble("day_pct")>=0?Charts.UP:Charts.DOWN);
   v.setTextViewText(R.id.btc_sub,"전일 대비 "+Charts.pct(btc.optDouble("day_pct"))+" · KST 09시 기준 · 조회 시세");
  }
  v.setImageViewBitmap(R.id.btc_chart,Charts.btc(arr(m,"daily")));
  if(dom.has("value")){
   double delta=dom.optDouble("delta24",Double.NaN);
   String change=Signals.finite(delta)?String.format(Locale.KOREA," · 약24h %+.2f%%p",delta):" · 24h 비교 수집 중";
   v.setTextViewText(R.id.dom_title,String.format(Locale.KOREA,"BTC 도미넌스 %.2f%%",dom.optDouble("value"))+change);
   boolean stale=r.optBoolean("dom_failed")||System.currentTimeMillis()-dom.optLong("time")>3*Signals.HOUR;
   v.setTextViewText(R.id.dom_meta,dom.optString("source")+" · "+Charts.date(dom.optLong("time"),"MM/dd HH:mm")+" KST"+(stale?" · 이전 자료":"")+"\n설치 후 실측 기록 · "+arr(dom,"history").length()+"개 구간");
  }
  v.setImageViewBitmap(R.id.dom_chart,Charts.dominance(arr(dom,"history")));
  if(m.has("breadth"))v.setTextViewText(R.id.breadth,String.format(Locale.KOREA,"원화 알트 상승 %.1f%% (%d/%d) · BTC·스테이블 제외",m.optDouble("breadth"),m.optInt("advancing"),m.optInt("alt_count")));
  String regime=m.optString("btc_regime","자료 조회 전");
  double delta=(!r.optBoolean("dom_failed")&&System.currentTimeMillis()-dom.optLong("time")<=3*Signals.HOUR)?dom.optDouble("delta24",Double.NaN):Double.NaN;
  String alt=Signals.altRegime(m.optDouble("breadth",Double.NaN),regime,delta);boolean fresh=freshMarket(r);
  v.setTextViewText(R.id.market_summary,"BTC: "+regime+"\n알트: "+alt+(m.length()>0&&!fresh?" · 이전 자료":""));
  JSONArray today=arr(m,"candidates"),next=arr(m,"next_candidates");
  v.setTextViewText(R.id.today,"오늘 관찰: "+(fresh?shortCandidates(today,compact?1:2):"최신 조회 후 제시"));
  String recheck=m.has("recheck_at")?Charts.date(m.optLong("recheck_at"),"MM/dd"):"내일";
  v.setTextViewText(R.id.next_day,recheck+" 09시 재확인: "+(fresh?shortCandidates(next,compact?1:2):"보류")+"\n미래 급등 예측 아님 · 조건 유지 확인용");
  JSONArray errors=arr(r,"errors");String warning="급등·수익률 보장 없음. 조건점수는 확률이 아닙니다.";
  if(errors.length()>0)warning=errors.optString(0)+(errors.length()>1?" 외 "+(errors.length()-1)+"건":"")+" · 상세 확인";
  else if(!m.optString("screening_error").isEmpty())warning=m.optString("screening_error");
  v.setTextViewText(R.id.warning,warning);
  boolean auto=c.getSharedPreferences("settings",0).getBoolean("auto",true);
  String time=m.has("fetched_at")?Charts.date(m.optLong("fetched_at"),"MM/dd HH:mm")+" KST":"조회 전";
  v.setTextViewText(R.id.updated,(auto?"2시간 자동 조회(절전 시 지연)":"자동 조회 꺼짐")+"\n시장 자료 "+time+(r.optBoolean("market_failed")?"\n최근 시도 실패":""));
  v.setTextViewText(R.id.refresh,Repository.RUNNING.get()?"조회 중…":"↻ 새로고침");
  if(compact){v.setViewVisibility(R.id.dom_chart,View.GONE);v.setInt(R.id.next_day,"setMaxLines",1);v.setInt(R.id.today,"setMaxLines",1);v.setInt(R.id.breadth,"setMaxLines",1);v.setInt(R.id.dom_title,"setMaxLines",1);v.setInt(R.id.market_summary,"setMaxLines",2);v.setViewVisibility(R.id.dom_meta,View.GONE);}
  PendingIntent app=activity(c,10,new Intent(c,MainActivity.class));
  v.setOnClickPendingIntent(R.id.open_app,app);v.setOnClickPendingIntent(R.id.title,app);v.setOnClickPendingIntent(R.id.today,app);v.setOnClickPendingIntent(R.id.next_day,app);
  PendingIntent chart=activity(c,11,new Intent(Intent.ACTION_VIEW,Uri.parse("https://upbit.com/exchange?code=CRIX.UPBIT.KRW-BTC")));
  v.setOnClickPendingIntent(R.id.btc_chart,chart);v.setOnClickPendingIntent(R.id.btc_price,chart);
  Intent intent=new Intent(c,RefreshService.class).setAction("kr.sejong.coinwidget.REFRESH");
  v.setOnClickPendingIntent(R.id.refresh,PendingIntent.getForegroundService(c,12,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));return v;
 }
 static String shortCandidates(JSONArray a,int max){
  if(a.length()==0)return "조건 충족 없음";StringBuilder s=new StringBuilder();
  for(int i=0;i<Math.min(max,a.length());i++){JSONObject item=a.optJSONObject(i);if(item==null)continue;JSONObject q=obj(item,"quote");if(s.length()>0)s.append(" / ");s.append(q.optString("market").replace("KRW-","")).append(" ").append(Math.round(item.optDouble("score"))).append("점");}
  return s.toString();
 }
}
