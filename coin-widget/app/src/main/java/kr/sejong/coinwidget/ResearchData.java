package kr.sejong.coinwidget;
import org.json.*;
import java.util.*;
final class ResearchData {
 static JSONObject record(Signals.Quote q,StrategyV130.Result s,int mode)throws JSONException{
  boolean approved=s.pass&&ValidationPolicy.approved(mode);
  return new JSONObject().put("quote",Repository.quoteJson(q)).put("rule_pass",s.pass).put("pre_rule_pass",mode==1&&s.pass).put("qualified",mode!=1&&approved).put("pre_qualified",mode==1&&approved)
   .put("score",s.score).put("morning_score",s.score).put("reason",s.reason).put("risk",s.risk).put("rsi",s.rsi).put("atr_pct",s.atr/q.price*100).put("volume_ratio",s.volume)
   .put("relative6h",s.rel6).put("return6h",s.ret6).put("return12h",s.ret12).put("return30",s.ret30).put("relative30",s.rel30).put("return90",s.ret90).put("relative90",s.rel90)
   .put("support",s.support).put("resistance",s.resistance).put("entry_low",q.price).put("entry_high",q.price).put("reference_rr",s.rr).put("net_room_pct",s.netRoom).put("net_risk_pct",s.netRisk)
   .put("ma20",s.ma20).put("ma60",s.ma60).put("ma120",s.ma120).put("pulse_ratio",s.pulseRatio).put("pulse_change",s.pulseChange).put("pulse_through",s.through)
   .put("compression",s.compression).put("strategy_version","1.3.0").put("evaluation_approved",ValidationPolicy.approved(mode));
 }
 static JSONArray[] ranked(List<JSONObject>items,int limit,int mode)throws JSONException{
  items.sort((a,b)->{int d=Boolean.compare(b.optBoolean("rule_pass"),a.optBoolean("rule_pass"));if(d!=0)return d;d=Double.compare(b.optDouble("score"),a.optDouble("score"));return d!=0?d:Renderer.obj(a,"quote").optString("market").compareTo(Renderer.obj(b,"quote").optString("market"));});
  JSONArray watch=new JSONArray(),strict=new JSONArray();
  for(JSONObject a:items){boolean qualifies=a.optBoolean(mode==1?"pre_qualified":"qualified");if(qualifies&&strict.length()>=5){a.put("qualified",false).put("pre_qualified",false);qualifies=false;}if(watch.length()<limit)watch.put(a);if(qualifies)strict.put(a);}
  return new JSONArray[]{watch,strict};
 }
 static String status(List<JSONObject>a,int failures,boolean stopped){return a.isEmpty()&&(failures>0||stopped)?"error":failures>0?"partial":a.isEmpty()?"insufficient":"ok";}
 static JSONObject finish(Net net,List<Signals.Quote>quotes,Signals.Quote btc,List<Signals.Bar>daily,long now)throws Exception{
  int count=0,up=0;List<Signals.Quote>pool=new ArrayList<>();
  for(Signals.Quote q:quotes){if(Signals.isAlt(q.market)&&!q.market.equals("KRW-EURC")&&q.time>0&&now-q.time<=600000&&q.time<=now){count++;if(q.dayPct>0)up++;}if(StrategyV130.universe(q,now)&&!q.market.equals("KRW-EURC"))pool.add(q);}
  pool.sort((a,b)->{int d=Double.compare(b.turnover,a.turnover);return d!=0?d:a.market.compareTo(b.market);});
  List<Signals.Quote>scan=pool.subList(0,Math.min(StrategyV130.SCAN,pool.size()));
  List<JSONObject>days=new ArrayList<>(),pre=new ArrayList<>(),longs=new ArrayList<>();List<Signals.Bar>bh=new ArrayList<>();
  int inspected=0,failed=0,invalid=0,pulseChecked=0,pulseFailed=0,longInspected=0,longFailed=0;boolean hourlyError=false;
  try{bh=Repository.readBars(new JSONArray(net.get("https://api.upbit.com/v1/candles/minutes/60?market=KRW-BTC&count=80")));}catch(Exception e){Repository.cancelCheck();hourlyError=true;}
  if(!hourlyError)for(Signals.Quote q:scan){
   List<Signals.Bar>h;
   try{h=Repository.readBars(new JSONArray(net.get("https://api.upbit.com/v1/candles/minutes/60?market="+q.market+"&count=80")));inspected++;}
   catch(Exception e){Repository.cancelCheck();failed++;if(failed>=5)break;continue;}
   try{
    List<Signals.Bar>f=Repository.readBars(new JSONArray(net.get("https://api.upbit.com/v1/candles/minutes/5?market="+q.market+"&count=30")));pulseChecked++;
    StrategyV130.Result d=StrategyV130.shortRule(0,q,h,bh,f,now),p=StrategyV130.shortRule(1,q,h,bh,f,now);
    if(d!=null)days.add(record(q,d,0));else invalid++;if(p!=null)pre.add(record(q,p,1));
   }catch(Exception e){Repository.cancelCheck();pulseFailed++;if(pulseFailed>=5)break;}
  }
  for(Signals.Quote q:scan){
   try{List<Signals.Bar>b=Repository.readBars(new JSONArray(net.get("https://api.upbit.com/v1/candles/days?market="+q.market+"&count=160")));longInspected++;StrategyV130.Result s=StrategyV130.longRule(q,b,daily,now);if(s!=null)longs.add(record(q,s,2));}
   catch(Exception e){Repository.cancelCheck();longFailed++;if(longFailed>=3)break;}
  }
  JSONArray[]d=ranked(days,30,0),p=ranked(pre,15,1),l=ranked(longs,15,2);
  double breadth=count>0?up*100.0/count:Double.NaN;String regime=Signals.btcRegime(btc.price,daily,now);boolean defensive=regime.startsWith("하락")||regime.startsWith("판단 보류")||!Signals.finite(breadth)||breadth<35;
  String defenseReason=regime.startsWith("하락")?"BTC 일봉 하락 추세":regime.startsWith("판단 보류")?"BTC 일봉 자료 부족":breadth<35?"상승 종목 35% 미만":"";
  JSONObject technical=new JSONObject();List<Signals.Bar>closed=Signals.closed(daily,now,24*Signals.HOUR);
  if(closed.size()>=60)technical.put("rsi",Signals.rsi(closed,14)).put("ma20",Signals.sma(closed,20)).put("ma60",Signals.sma(closed,60)).put("high20",StrategyV130.high(closed,20)).put("low20",StrategyV130.low(closed,20));
  double b6=Signals.returnHours(Signals.closed(bh,now,Signals.HOUR),6);if(Signals.finite(b6))technical.put("return6h",b6);
  return new JSONObject().put("schema",7).put("strategy_version","1.3.0").put("fetched_at",System.currentTimeMillis()).put("quote_at",now)
   .put("day_status",status(days,failed+pulseFailed,hourlyError)).put("pre_status",status(pre,failed+pulseFailed,hourlyError)).put("long_status",status(longs,longFailed,false))
   .put("day_at",now).put("pre_at",now).put("long_at",now).put("recheck_at",Research.nextNine(now)).put("pre_recheck_at",Research.nextNine(now))
   .put("btc",Repository.quoteJson(btc)).put("daily",Repository.barsJson(daily)).put("btc_technical",technical).put("btc_regime",regime)
   .put("defensive",defensive).put("defense_reason",defenseReason).put("long_defensive",regime.startsWith("하락")||regime.startsWith("판단 보류"))
   .put("alt_total",count).put("quote_count",quotes.size()).put("breadth",Signals.finite(breadth)?breadth:JSONObject.NULL).put("advancing",up).put("alt_count",count)
   .put("watchlist",d[0]).put("candidates",d[1]).put("pre_watchlist",p[0]).put("next_candidates",p[1]).put("pre_count",p[1].length()).put("long_watchlist",l[0]).put("long_candidates",l[1])
   .put("screened",pool.size()).put("inspected",inspected).put("failed",failed).put("invalid",invalid).put("pulse_checked",pulseChecked).put("pulse_failed",pulseFailed).put("long_inspected",longInspected).put("long_failed",longFailed)
   .put("screening_error",hourlyError?"BTC 시간봉 조회 실패":failed+pulseFailed+longFailed>0?"일부 종목 통신 실패":"");
 }
}
