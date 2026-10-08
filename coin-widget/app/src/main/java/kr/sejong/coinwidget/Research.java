package kr.sejong.coinwidget;
import java.time.*;
import java.util.*;
/** Transparent ranking heuristics, not a price predictor. All indicators use completed bars. */
final class Research {
 static final int SCAN_LIMIT=60,DISPLAY_LIMIT=30,MORNING_LIMIT=15;
 static long nextNine(long now){ZonedDateTime k=Instant.ofEpochMilli(now).atZone(ZoneId.of("Asia/Seoul"));ZonedDateTime t=k.toLocalDate().atTime(9,0).atZone(k.getZone());return (k.isBefore(t)?t:t.plusDays(1)).toInstant().toEpochMilli();}
 static boolean pool(Signals.Quote q,long now){return q!=null&&Signals.isAlt(q.market)&&!q.warning&&Signals.finite(q.price)&&q.price>0&&Signals.finite(q.turnover)&&q.turnover>=1e9&&Signals.finite(q.dayPct)&&q.dayPct>=-12&&q.dayPct<=30&&now-q.time<=600000&&q.time>0&&q.time<=now+60000;}
 static final class S {
  double ma20,ma60,rsi,atr,ratio,rel6,ret6,ret12,score,morning,high,low,entryLo,entryHi,rr;
  boolean qualified,preQualified;String state,preState,reason,risk;
 }
 static S evaluate(Signals.Quote q,List<Signals.Bar>raw,List<Signals.Bar>btcRaw,long now,boolean defensive){
  if(!pool(q,now)||now-q.time>120000)return null;List<Signals.Bar>b=Signals.closed(raw,now,Signals.HOUR),btc=Signals.closed(btcRaw,now,Signals.HOUR);
  if(!Signals.hourlyContinuous(b,now,60)||!Signals.hourlyContinuous(btc,now,13)||b.get(b.size()-1).time!=btc.get(btc.size()-1).time)return null;
  S s=new S();s.ma20=Signals.sma(b,20);s.ma60=Signals.sma(b,60);s.rsi=Signals.rsi(b,14);s.atr=Signals.atr(b,14);
  s.ret6=Signals.returnHours(b,6);s.ret12=Signals.returnHours(b,12);s.rel6=s.ret6-Signals.returnHours(btc,6);
  double sum=0;for(int i=b.size()-21;i<b.size()-1;i++)sum+=b.get(i).volume;s.ratio=sum>0?b.get(b.size()-1).volume/(sum/20):Double.NaN;
  if(!Signals.finite(s.ratio)||!Signals.finite(s.atr)||!Signals.finite(s.rel6)||!Signals.finite(s.ret12))return null;
  s.high=0;s.low=Double.MAX_VALUE;for(int i=b.size()-20;i<b.size();i++)s.high=Math.max(s.high,b.get(i).high);for(int i=b.size()-12;i<b.size();i++)s.low=Math.min(s.low,b.get(i).low);
  boolean trend=q.price>=s.ma20&&s.ma20>=s.ma60;double ap=s.atr/q.price*100;
  s.score=clamp(30+(trend?18:0)+clamp(s.rel6*3,-12,16)+clamp((s.ratio-.5)*10,-5,15)+(s.rsi>=45&&s.rsi<=68?12:0)+clamp((Math.log10(q.turnover)-9)*5,0,10)-Math.max(0,ap-3)*5-Math.max(0,q.dayPct-15));
  s.qualified=Signals.analyze(q,raw,btcRaw,now)!=null&&s.score>=65&&!defensive
   &&s.ret6>0&&s.ret12>0&&s.rel6>=.3&&s.ratio>=1.1&&s.rsi>=45&&s.rsi<=68&&ap<=3.5
   &&s.ma20>Signals.sma(b.subList(0,b.size()-3),20);
  s.state=s.qualified?"상승 조건 충족":s.rsi>76||q.dayPct>20?"추격 주의":defensive?"시장 방어·대기":trend?"확인 대기":"추세 회복 대기";
  s.reason=String.format(Locale.KOREA,"같은 6시간 동안 BTC보다 %s %.1f%%p · 직전 1시간 거래대금은 이전 20시간 평균의 %.1f배",s.rel6>=0?"강함":"약함",Math.abs(s.rel6),s.ratio);
  // Reference band around a measured moving average, never above the observed quote.
  s.entryHi=Math.min(q.price,s.ma20+.25*s.atr);s.entryLo=Math.min(s.entryHi,s.ma20-.35*s.atr);
  double middle=(s.entryLo+s.entryHi)/2,loss=middle-s.low;
  s.rr=loss>0&&s.high>middle?(s.high-middle)/loss:0;
  boolean geometry=s.low<s.entryLo&&s.entryLo>0&&s.high>s.entryHi;
  s.qualified=s.qualified&&geometry&&s.rr>=1.3&&q.price<=s.ma20+1.5*s.atr;
  s.state=s.qualified?"상승 조건 충족":s.state.equals("상승 조건 충족")?"가격 구조 재확인":s.state;
  s.preQualified=s.qualified&&s.rsi<=66&&s.rel6>=.5&&s.rr>=1.5;
  s.morning=clamp(s.score-Math.abs(s.rsi-55)*.7+clamp(s.ret12,0,8)+(s.preQualified?8:0));
  s.preState=s.preQualified?"눌림·지지 확인 후 검토":defensive?"선매수 보류":!geometry?"가격 구조 재확인":"9시 전 단기 거래 확인 필요";
  s.risk=defensive?"BTC 약세 또는 상승 종목 부족":s.rsi>70?"RSI 과열·추격 주의":!trend?"20·60시간 추세 미충족":s.ratio<.9?"최근 거래대금 약함":ap>3.5?"시간당 변동성 큼":"최근 저점 이탈 시 조건 무효";
  return s;
 }
 static double clamp(double n){return Math.max(0,Math.min(100,n));}
 static double clamp(double n,double a,double b){return Math.max(a,Math.min(b,n));}
}
