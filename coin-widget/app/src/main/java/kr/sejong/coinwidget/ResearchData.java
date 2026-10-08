package kr.sejong.coinwidget;
import org.json.*;
import java.util.*;
final class ResearchData {
 static JSONObject finish(Net net,List<Signals.Quote>quotes,Signals.Quote btc,List<Signals.Bar>daily,long now)throws Exception{
  int count=0,up=0;List<Signals.Quote>pool=new ArrayList<>();
  for(Signals.Quote q:quotes){if(Signals.isAlt(q.market)&&q.time>0&&now-q.time<=600000&&q.time<=now+60000){count++;if(q.dayPct>0)up++;}if(Research.pool(q,now))pool.add(q);}
  double breadth=count>0?up*100.0/count:Double.NaN;String regime=Signals.btcRegime(btc.price,daily,now);
  List<JSONObject>all=new ArrayList<>();List<Signals.Bar>bh=new ArrayList<>();int inspected=0,failed=0,invalid=0;
  String error="",defenseReason="BTC 시간봉 확인 필요";boolean defensive=true;
  pool.sort((a,b)->Double.compare(Signals.screeningScore(b,btc.dayPct),Signals.screeningScore(a,btc.dayPct)));
  try{bh=Repository.readBars(new JSONArray(net.get("https://api.upbit.com/v1/candles/minutes/60?market=KRW-BTC&count=80")));
   List<Signals.Bar>closed=Signals.closed(bh,now,Signals.HOUR);double b1=Signals.returnHours(closed,1);
   defenseReason=regime.startsWith("하락")?"BTC 일봉 하락 추세":regime.startsWith("판단 보류")?"BTC 일봉 검증 불충분":!Signals.finite(breadth)?"시장 상승비율 자료 부족":breadth<35?"상승 종목 35% 미만":!Signals.hourlyContinuous(closed,now,13)||!Signals.finite(b1)?"BTC 시간봉 검증 불충분":b1<-1.2?"BTC 직전 1시간 1.2% 초과 하락":"";defensive=!defenseReason.isEmpty();
   for(Signals.Quote q:pool.subList(0,Math.min(Research.SCAN_LIMIT,pool.size()))){
    try{List<Signals.Bar>b=Repository.readBars(new JSONArray(net.get("https://api.upbit.com/v1/candles/minutes/60?market="+q.market+"&count=80")));inspected++;
     Research.S s=Research.evaluate(q,b,bh,now,defensive);if(s==null){invalid++;continue;}if(defensive)s.risk=defenseReason;
     JSONObject o=new JSONObject().put("quote",Repository.quoteJson(q)).put("score",s.score).put("morning_score",s.morning).put("qualified",s.qualified).put("pre_qualified",s.preQualified)
      .put("state",s.state).put("pre_state",s.preState).put("reason",s.reason).put("risk",s.risk).put("rsi",s.rsi).put("atr_pct",s.atr/q.price*100).put("volume_ratio",s.ratio)
      .put("relative6h",s.rel6).put("return6h",s.ret6).put("return12h",s.ret12).put("resistance",s.high).put("support",s.low).put("ma20",s.ma20).put("ma60",s.ma60)
      .put("entry_low",s.entryLo).put("entry_high",s.entryHi).put("reference_rr",s.rr);all.add(o);
    }catch(Exception e){Repository.cancelCheck();failed++;if(failed>=5){error="통신 실패 누적: 일부 종목만 분석";break;}}
   }
  }catch(Exception e){Repository.cancelCheck();error="시간봉 조회 실패: 매수 검토 보류";}
  all.sort((a,b)->{int d=Boolean.compare(b.optBoolean("qualified"),a.optBoolean("qualified"));return d!=0?d:Double.compare(b.optDouble("score"),a.optDouble("score"));});
  JSONArray today=new JSONArray(),strict=new JSONArray();for(int i=0;i<all.size();i++){JSONObject a=all.get(i);if(i<Research.DISPLAY_LIMIT)today.put(a);if(a.optBoolean("qualified"))strict.put(a);}
  all.sort((a,b)->{int d=Boolean.compare(b.optBoolean("pre_qualified"),a.optBoolean("pre_qualified"));return d!=0?d:Double.compare(b.optDouble("morning_score"),a.optDouble("morning_score"));});
  JSONArray morning=new JSONArray();int pre=0;for(JSONObject a:all)if(a.optBoolean("pre_qualified"))pre++;for(int i=0;i<Math.min(Research.MORNING_LIMIT,all.size());i++)morning.put(all.get(i));
  List<Signals.Bar>d=Signals.closed(daily,now,24*Signals.HOUR);JSONObject technical=new JSONObject();
  if(d.size()>=60){double hi=0,lo=Double.MAX_VALUE;for(int i=d.size()-20;i<d.size();i++){hi=Math.max(hi,d.get(i).high);lo=Math.min(lo,d.get(i).low);}technical.put("rsi",Signals.rsi(d,14)).put("ma20",Signals.sma(d,20)).put("ma60",Signals.sma(d,60)).put("high20",hi).put("low20",lo);}
  List<Signals.Bar>closed=Signals.closed(bh,now,Signals.HOUR);double b6=Signals.returnHours(closed,6);if(Signals.finite(b6))technical.put("return6h",b6);
  return new JSONObject().put("defensive",defensive).put("defense_reason",defenseReason).put("schema",3).put("fetched_at",System.currentTimeMillis()).put("quote_at",now).put("btc",Repository.quoteJson(btc)).put("daily",Repository.barsJson(daily)).put("btc_technical",technical).put("btc_regime",regime)
   .put("alt_total",(int)quotes.stream().filter(q->Signals.isAlt(q.market)).count()).put("quote_count",quotes.size()).put("breadth",Signals.finite(breadth)?breadth:JSONObject.NULL).put("advancing",up).put("alt_count",count).put("candidates",strict).put("watchlist",today).put("next_candidates",morning).put("pre_count",pre)
   .put("screened",pool.size()).put("inspected",inspected).put("invalid",invalid).put("failed",failed).put("screening_error",error+(failed>0?" · 실패 "+failed+"개":"")).put("recheck_at",Research.nextNine(now));
 }
}
