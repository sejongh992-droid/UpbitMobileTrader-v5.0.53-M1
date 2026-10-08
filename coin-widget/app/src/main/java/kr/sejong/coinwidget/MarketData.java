package kr.sejong.coinwidget;
import org.json.*;
import java.io.IOException;
import java.time.*;
import java.util.*;
/** Public Upbit prices and conditional observation lists only. */
final class MarketData {
 static JSONObject fetch(Net net)throws Exception{
  JSONArray meta=new JSONArray(net.get("https://api.upbit.com/v1/market/all?is_details=true"));
  Map<String,JSONObject> names=new HashMap<>();List<String> markets=new ArrayList<>();
  for(int i=0;i<meta.length();i++){JSONObject m=meta.getJSONObject(i);String code=m.getString("market");if(code.startsWith("KRW-")){markets.add(code);names.put(code,m);}}
  if(markets.size()<2||!markets.contains("KRW-BTC"))throw new IOException("원화 마켓 목록 부족");
  List<Signals.Quote> quotes=new ArrayList<>();
  for(int from=0;from<markets.size();from+=80){
   String query=String.join(",",markets.subList(from,Math.min(from+80,markets.size())));
   JSONArray arr=new JSONArray(net.get("https://api.upbit.com/v1/ticker?markets="+query));
   for(int i=0;i<arr.length();i++){
    JSONObject t=arr.getJSONObject(i);String market=t.getString("market");JSONObject m=names.get(market);if(m==null)continue;
    JSONObject event=m.optJSONObject("market_event");
    boolean warning="CAUTION".equals(m.optString("market_warning"))||(event!=null&&event.optBoolean("warning"));
    JSONObject caution=event==null?null:event.optJSONObject("caution");
    if(caution!=null){Iterator<String> keys=caution.keys();while(keys.hasNext())warning|=caution.optBoolean(keys.next());}
    double price=t.getDouble("trade_price"),day=t.getDouble("signed_change_rate")*100,vol=t.getDouble("acc_trade_price_24h");
    if(!Signals.finite(price)||price<=0||!Signals.finite(day)||!Signals.finite(vol))continue;
    quotes.add(new Signals.Quote(market,m.optString("korean_name",market.substring(4)),price,day,vol,warning,t.optLong("trade_timestamp",t.optLong("timestamp"))));
   }
  }
  long now=System.currentTimeMillis();Signals.Quote btc=null;int count=0,up=0;
  for(Signals.Quote q:quotes){if(q.market.equals("KRW-BTC"))btc=q;if(Signals.isAlt(q.market)){count++;if(q.dayPct>0)up++;}}
  if(btc==null||btc.time<=0||now-btc.time>10*60_000L||btc.time>now+60_000L)throw new IOException("BTC 최신 시세 확인 실패");
  List<Signals.Bar> daily=Repository.readBars(new JSONArray(net.get("https://api.upbit.com/v1/candles/days?market=KRW-BTC&count=90")));
  if(daily.size()<2)throw new IOException("BTC 일봉 자료 부족");
  String screeningError="";List<Signals.Candidate> candidates=new ArrayList<>();List<Signals.Quote> screened=new ArrayList<>();
  for(Signals.Quote q:quotes)if(Signals.eligible(q,now))screened.add(q);
  final double btcDay=btc.dayPct;screened.sort((a,b)->Double.compare(Signals.screeningScore(b,btcDay),Signals.screeningScore(a,btcDay)));
  int inspected=0,failed=0;
  try{
   List<Signals.Bar> btcHourly=Repository.readBars(new JSONArray(net.get("https://api.upbit.com/v1/candles/minutes/60?market=KRW-BTC&count=75")));
   for(Signals.Quote q:screened.subList(0,Math.min(8,screened.size()))){
    try{List<Signals.Bar> bars=Repository.readBars(new JSONArray(net.get("https://api.upbit.com/v1/candles/minutes/60?market="+q.market+"&count=75")));inspected++;Signals.Candidate a=Signals.analyze(q,bars,btcHourly,now);if(a!=null)candidates.add(a);}catch(Exception e){Repository.cancelCheck();failed++;}
   }
   if(failed>0)screeningError=failed+"종목 시간봉 조회 실패: 해당 종목 제외";
  }catch(Exception e){Repository.cancelCheck();screeningError="시간봉 분석 불가: 후보 제시 보류";}
  candidates.sort((a,b)->Double.compare(b.score,a.score));
  JSONArray result=new JSONArray();for(Signals.Candidate a:candidates)result.put(Repository.candidateJson(a));
  List<Signals.Candidate> next=new ArrayList<>(candidates);next.removeIf(a->a.rsi>68||a.relative6h<0);
  next.sort((a,b)->Double.compare(b.score-Math.abs(b.rsi-55)*0.6,a.score-Math.abs(a.rsi-55)*0.6));
  JSONArray nextResult=new JSONArray();for(Signals.Candidate a:next)nextResult.put(Repository.candidateJson(a));
  return new JSONObject().put("fetched_at",System.currentTimeMillis()).put("btc",Repository.quoteJson(btc)).put("daily",Repository.barsJson(daily)).put("btc_regime",Signals.btcRegime(btc.price,daily,now))
   .put("breadth",count==0?JSONObject.NULL:up*100.0/count).put("advancing",up).put("alt_count",count).put("candidates",result).put("next_candidates",nextResult).put("screened",screened.size()).put("inspected",inspected).put("screening_error",screeningError)
   .put("recheck_at",ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDate().plusDays(1).atTime(9,0).atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli());
 }
}
