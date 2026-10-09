package kr.sejong.coinwidget;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
/** Cached read-only snapshots. No account, trading, or withdrawal endpoints. */
final class Repository {
 static final AtomicBoolean RUNNING=new AtomicBoolean(false);
 static volatile String phase="";
 interface Sources {
  JSONObject fx()throws Exception;
  JSONObject market()throws Exception;
  JSONObject dominance(JSONObject previous)throws Exception;
 }
 static JSONObject load(Context c){try{return new JSONObject(c.getSharedPreferences("cache",0).getString("snapshot","{}"));}catch(Exception e){return new JSONObject();}}
 static boolean refresh(Context context)throws Exception{
  Context c=context.getApplicationContext();
  return refresh(c,new Sources(){
   public JSONObject fx()throws Exception{return FxData.parse(new JSONObject(new Net(15_000).get("https://api.frankfurter.dev/v2/providers/ecb/rate/usd/krw")),System.currentTimeMillis());}
   public JSONObject market()throws Exception{return MarketData.fetch(new Net(105_000));}
   public JSONObject dominance(JSONObject old)throws Exception{return DominanceData.fetch(c,new Net(20_000),old);}
  });
 }
 static void save(Context c,JSONObject root)throws IOException{
  cancelCheck();if(!c.getSharedPreferences("cache",0).edit().putString("snapshot",root.toString()).commit())throw new IOException("조회 결과 저장 실패");MarketWidget.renderAll(c);
 }
 static boolean refresh(Context context,Sources sources)throws Exception{
  Context c=context.getApplicationContext();if(!RUNNING.compareAndSet(false,true))return false;
  try{
   JSONObject root=load(c);JSONArray errors=new JSONArray();root.put("last_attempt",System.currentTimeMillis()).put("errors",errors);
   phase="환율 확인 중";save(c,root);
   try{root.put("fx",sources.fx()).put("fx_failed",false);}catch(Exception e){cancelCheck();root.put("fx_failed",true);errors.put("환율: "+safe(e));}
   phase="종목 분석 중";save(c,root);
   try{root.put("market",SnapshotState.merge(Renderer.obj(root,"market"),sources.market())).put("market_failed",false);}catch(Exception e){cancelCheck();root.put("market_failed",true);errors.put("업비트: "+safe(e));}
   phase="비중 확인 중";save(c,root);
   try{root.put("dominance",sources.dominance(root.optJSONObject("dominance"))).put("dom_failed",false);}catch(Exception e){cancelCheck();root.put("dom_failed",true);errors.put("도미넌스: "+safe(e));}
   cancelCheck();root.put("last_finished",System.currentTimeMillis());save(c,root);
   JSONObject market=Renderer.obj(root,"market");Scheduler.expire(c,market.optLong("quote_at"),market.optLong("recheck_at",Long.MAX_VALUE));
   return !root.optBoolean("market_failed",true);
  }finally{phase="";RUNNING.set(false);MarketWidget.renderAll(c);}
 }
 static void recordFailure(Context c,String message){try{JSONObject r=load(c);r.put("errors",new JSONArray().put(message)).put("last_attempt",System.currentTimeMillis()).put("market_failed",true);c.getSharedPreferences("cache",0).edit().putString("snapshot",r.toString()).apply();MarketWidget.renderAll(c);}catch(Exception ignored){}}
 private static String safe(Exception e){
  if(e instanceof java.net.SocketTimeoutException)return "연결 시간 초과";
  if(e instanceof java.net.UnknownHostException)return "인터넷 또는 DNS 연결 실패";
  if(e instanceof JSONException)return "데이터 응답 형식 확인 필요";
  if(e instanceof IOException){String s=e.getMessage();return s==null?"통신 실패":s.substring(0,Math.min(100,s.length()));}
  return "설정 또는 데이터 처리 오류";
 }
 static void cancelCheck()throws InterruptedIOException{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("조회 취소");}
 static List<Signals.Bar> readBars(JSONArray arr)throws JSONException{
  TreeMap<Long,Signals.Bar> out=new TreeMap<>();
  for(int i=0;i<arr.length();i++){
   JSONObject o=arr.getJSONObject(i);boolean n=o.has("t");
   long time=n?o.getLong("t"):LocalDateTime.parse(o.getString("candle_date_time_utc"),DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC).toEpochMilli();
   Signals.Bar b=new Signals.Bar(time,o.getDouble(n?"o":"opening_price"),o.getDouble(n?"h":"high_price"),o.getDouble(n?"l":"low_price"),o.getDouble(n?"c":"trade_price"),o.getDouble(n?"v":"candle_acc_trade_price"));
   if(b.valid())out.put(time,b);
  }
  return new ArrayList<>(out.values());
 }
 static JSONArray barsJson(List<Signals.Bar> bars)throws JSONException{JSONArray a=new JSONArray();for(Signals.Bar b:bars)a.put(new JSONObject().put("t",b.time).put("o",b.open).put("h",b.high).put("l",b.low).put("c",b.close).put("v",b.volume));return a;}
 static JSONObject quoteJson(Signals.Quote q)throws JSONException{return new JSONObject().put("market",q.market).put("name",q.name).put("price",q.price).put("day_pct",q.dayPct).put("turnover",q.turnover).put("time",q.time);}
 static JSONObject candidateJson(Signals.Candidate a)throws JSONException{return new JSONObject().put("quote",quoteJson(a.quote)).put("score",a.score).put("rsi",a.rsi).put("atr_pct",a.atrPct).put("volume_ratio",a.volumeRatio).put("relative6h",a.relative6h).put("resistance",a.resistance).put("support",a.support);}
}
