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
    quotes.add(parseTicker(t,m));
   }
  }
  ensureCoverage(quotes,markets);
  long now=System.currentTimeMillis();Signals.Quote btc=null;int count=0,up=0;
  for(Signals.Quote q:quotes){if(q.market.equals("KRW-BTC"))btc=q;if(Signals.isAlt(q.market)){count++;if(q.dayPct>0)up++;}}
  if(btc==null||btc.time<=0||now-btc.time>10*60_000L||btc.time>now+60_000L)throw new IOException("BTC 최신 시세 확인 실패");
  List<Signals.Bar> daily=Repository.readBars(new JSONArray(net.get("https://api.upbit.com/v1/candles/days?market=KRW-BTC&count=120")));
  if(daily.size()<2)throw new IOException("BTC 일봉 자료 부족");
  return ResearchData.finish(net,quotes,btc,daily,now);
 }
 static Signals.Quote parseTicker(JSONObject t,JSONObject m)throws Exception{
  String market=t.getString("market");double price=t.getDouble("trade_price"),prev=t.getDouble("prev_closing_price"),day=t.getDouble("signed_change_rate")*100,vol=t.getDouble("acc_trade_price_24h");
  long time=t.getLong("trade_timestamp");
  if(!Signals.finite(price)||price<=0||!Signals.finite(prev)||prev<=0||!Signals.finite(day)||!Signals.finite(vol)||vol<0||time<=0||Math.abs(day-(price/prev-1)*100)>0.0001)throw new IOException("시세 값 또는 전일대비 일치 검사 실패");
  JSONObject event=m.optJSONObject("market_event");boolean warning="CAUTION".equals(m.optString("market_warning"))||(event!=null&&event.optBoolean("warning"));
  JSONObject caution=event==null?null:event.optJSONObject("caution");if(caution!=null){Iterator<String> keys=caution.keys();while(keys.hasNext())warning|=caution.optBoolean(keys.next());}
  return new Signals.Quote(market,m.optString("korean_name",market.substring(4)),price,day,vol,warning,time);
 }
 static void ensureCoverage(List<Signals.Quote>quotes,List<String>expected)throws IOException{
  Set<String>seen=new HashSet<>();for(Signals.Quote q:quotes)if(!seen.add(q.market))throw new IOException("중복 시세 응답: 재조회 필요");
  if(seen.size()!=expected.size()||!seen.containsAll(expected))throw new IOException("원화 시세 일부 누락: 재조회 필요");
 }
}
