package kr.sejong.coinwidget;
import org.json.*;
import java.util.*;
final class ResearchData {
 static JSONObject finish(Net net,List<Signals.Quote>quotes,Signals.Quote btc,List<Signals.Bar>daily,long now)throws Exception{
  int count=0,up=0;List<Signals.Quote>pool=new ArrayList<>();
  for(Signals.Quote q:quotes){if(Signals.isAlt(q.market)&&q.time>0&&now-q.time<=600000&&q.time<=now+60000){count++;if(q.dayPct>0)up++;}if(Research.pool(q,now))pool.add(q);}
  double breadth=count>0?up*100.0/count:Double.NaN;String regime=Signals.btcRegime(btc.price,daily,now);
  List<JSONObject>all=new ArrayList<>();List<Signals.Bar>bh=new ArrayList<>();int inspected=0,failed=0,invalid=0;
  String error="",defenseReason="BTC 시간봉 확인 필요";boolean defensive=true,hourlyError=false;
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
  }catch(Exception e){Repository.cancelCheck();hourlyError=true;error="시간봉 조회 실패: 분석 보류";}
  // Keep verified observations visible even when no coin passes every buy condition.
  all.sort((a,b)->{int d=Boolean.compare(b.optBoolean("qualified"),a.optBoolean("qualified"));return d!=0?d:Double.compare(b.optDouble("morning_score"),a.optDouble("morning_score"));});
  List<JSONObject> preWatch=new ArrayList<>();int pulseChecked=0,pulseFailed=0;
  for(JSONObject a:all){
   boolean base=a.optBoolean("pre_qualified"),dayBase=a.optBoolean("qualified");a.put("pre_qualified",false).put("qualified",false);
   if(pulseChecked>=Research.DISPLAY_LIMIT){a.put("pulse_risk","단기 거래 확인 전");continue;}pulseChecked++;
   try{
    String code=Renderer.obj(a,"quote").getString("market");
    List<Signals.Bar> shortBars=Repository.readBars(new JSONArray(net.get("https://api.upbit.com/v1/candles/minutes/5?market="+code+"&count=30")));
    BeforeNine.Pulse pulse=BeforeNine.pulse(shortBars,now);
    if(pulse==null){a.put("pulse_risk","완료 5분봉 자료 부족");continue;}
    a.put("pulse_ratio",pulse.ratio).put("pulse_change",pulse.change).put("pulse_through",pulse.through).put("pulse_rising",pulse.rising)
     .put("qualified",dayBase&&pulse.passes).put("pre_qualified",base&&pulse.passes)
     .put("pulse_risk",pulse.passes?"":pulse.ratio<1.3?"최근 15분 거래 증가 부족":pulse.change<.15?"최근 15분 상승 약함":pulse.change>3?"최근 15분 급등·추격 주의":"연속 상승 확인 부족")
     .put("morning_score",Research.clamp(a.optDouble("morning_score")+Research.clamp((pulse.ratio-1)*6,-6,8)+Research.clamp(pulse.change*3,-12,6)));
    preWatch.add(a);
   }catch(Exception e){Repository.cancelCheck();pulseFailed++;a.put("pulse_risk","단기 봉 조회 실패");}
  }
  all.sort((a,b)->{int d=Boolean.compare(b.optBoolean("qualified"),a.optBoolean("qualified"));return d!=0?d:Double.compare(b.optDouble("score"),a.optDouble("score"));});
  JSONArray today=new JSONArray(),strict=new JSONArray();for(JSONObject a:all){if(today.length()<Research.DISPLAY_LIMIT)today.put(a);if(a.optBoolean("qualified")&&strict.length()<Research.DISPLAY_LIMIT)strict.put(a);}
  preWatch.sort((a,b)->{int d=Boolean.compare(b.optBoolean("pre_qualified"),a.optBoolean("pre_qualified"));return d!=0?d:Double.compare(b.optDouble("morning_score"),a.optDouble("morning_score"));});
  JSONArray morning=new JSONArray(),morningWatch=new JSONArray();for(JSONObject a:preWatch){if(morningWatch.length()<Research.MORNING_LIMIT)morningWatch.put(a);if(a.optBoolean("pre_qualified")&&morning.length()<Research.MORNING_LIMIT)morning.put(a);}int pre=morning.length();
  List<JSONObject> longs=new ArrayList<>();int longInspected=0,longFailed=0;boolean longDefense=regime.startsWith("하락")||regime.startsWith("판단 보류");
  List<Signals.Quote> liquid=new ArrayList<>();for(Signals.Quote q:quotes)if(LongerTerm.pool(q,now))liquid.add(q);
  liquid.sort((a,b)->Double.compare(b.turnover,a.turnover));
  for(Signals.Quote q:liquid.subList(0,Math.min(LongerTerm.SCAN_LIMIT,liquid.size()))){
   try{
    List<Signals.Bar> bars=Repository.readBars(new JSONArray(net.get("https://api.upbit.com/v1/candles/days?market="+q.market+"&count=160")));longInspected++;
    LongerTerm.S s=LongerTerm.evaluate(q,bars,daily,now);if(s==null)continue;
    longs.add(new JSONObject().put("quote",Repository.quoteJson(q)).put("qualified",s.qualified&&!longDefense).put("score",s.score).put("rsi",s.rsi).put("atr_pct",s.atr/q.price*100)
     .put("risk",longDefense?"BTC 일봉 약세·자료 확인 필요":s.risk).put("ma20",s.ma20).put("ma60",s.ma60).put("ma120",s.ma120).put("return30",s.ret30).put("relative30",s.relative30).put("volume_ratio",s.volumeRatio)
     .put("support",s.low).put("resistance",s.high).put("entry_low",s.entryLow).put("entry_high",s.entryHigh).put("reference_rr",s.rr));
   }catch(Exception e){Repository.cancelCheck();longFailed++;if(longFailed>=3)break;}
  }
  longs.sort((a,b)->{int d=Boolean.compare(b.optBoolean("qualified"),a.optBoolean("qualified"));return d!=0?d:Double.compare(b.optDouble("score"),a.optDouble("score"));});
  JSONArray longList=new JSONArray(),longWatch=new JSONArray();for(JSONObject a:longs){if(longWatch.length()<LongerTerm.DISPLAY_LIMIT)longWatch.put(a);if(a.optBoolean("qualified")&&longList.length()<LongerTerm.DISPLAY_LIMIT)longList.put(a);}
  List<Signals.Bar>d=Signals.closed(daily,now,24*Signals.HOUR);JSONObject technical=new JSONObject();
  if(d.size()>=60){double hi=0,lo=Double.MAX_VALUE;for(int i=d.size()-20;i<d.size();i++){hi=Math.max(hi,d.get(i).high);lo=Math.min(lo,d.get(i).low);}technical.put("rsi",Signals.rsi(d,14)).put("ma20",Signals.sma(d,20)).put("ma60",Signals.sma(d,60)).put("high20",hi).put("low20",lo);}
  List<Signals.Bar>closed=Signals.closed(bh,now,Signals.HOUR);double b6=Signals.returnHours(closed,6);if(Signals.finite(b6))technical.put("return6h",b6);
  String dayStatus=hourlyError||all.isEmpty()&&failed>0?"error":failed+pulseFailed>0?"partial":all.isEmpty()?"insufficient":"ok";
  String preStatus=hourlyError||preWatch.isEmpty()&&(failed+pulseFailed)>0?"error":failed+pulseFailed>0?"partial":preWatch.isEmpty()?"insufficient":"ok";
  String longStatus=longs.isEmpty()&&longFailed>0?"error":longFailed>0?"partial":longs.isEmpty()?"insufficient":"ok";
  return new JSONObject().put("day_status",dayStatus).put("pre_status",preStatus).put("long_status",longStatus).put("day_at",now).put("pre_at",now).put("long_at",now).put("pre_recheck_at",Research.nextNine(now))
   .put("defensive",defensive).put("defense_reason",defenseReason).put("schema",6).put("fetched_at",System.currentTimeMillis()).put("quote_at",now).put("btc",Repository.quoteJson(btc)).put("daily",Repository.barsJson(daily)).put("btc_technical",technical).put("btc_regime",regime)
   .put("alt_total",(int)quotes.stream().filter(q->Signals.isAlt(q.market)).count()).put("quote_count",quotes.size()).put("breadth",Signals.finite(breadth)?breadth:JSONObject.NULL).put("advancing",up).put("alt_count",count).put("candidates",strict).put("watchlist",today).put("next_candidates",morning).put("pre_watchlist",morningWatch).put("pre_count",pre)
   .put("long_candidates",longList).put("long_watchlist",longWatch).put("long_inspected",longInspected).put("long_failed",longFailed).put("long_defensive",longDefense).put("pulse_checked",pulseChecked).put("pulse_failed",pulseFailed).put("screened",pool.size()).put("inspected",inspected).put("invalid",invalid).put("failed",failed).put("screening_error",error+(failed>0?" · 실패 "+failed+"개":"")).put("recheck_at",Research.nextNine(now));
 }
}
