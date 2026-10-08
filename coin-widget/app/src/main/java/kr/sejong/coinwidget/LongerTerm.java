package kr.sejong.coinwidget;
import java.util.*;
/** Daily-trend screen for a 1–3 month review horizon; no price/return forecast. */
final class LongerTerm {
 static final int SCAN_LIMIT=30,DISPLAY_LIMIT=15;
 static boolean continuous(List<Signals.Bar>b,long now,int count){
  long day=24*Signals.HOUR;if(b.size()<count||now-(b.get(b.size()-1).time+day)>=day)return false;
  for(int i=b.size()-count+1;i<b.size();i++)if(b.get(i).time-b.get(i-1).time!=day)return false;return true;
 }
 static final class S {boolean qualified;String risk;double ma20,ma60,ma120,rsi,atr,ret30,relative30,volumeRatio,low,high,entryLow,entryHigh,rr,score;}
 static boolean pool(Signals.Quote q,long now){return q!=null&&Signals.isAlt(q.market)&&!q.warning&&Signals.finite(q.price)&&q.price>0&&Signals.finite(q.turnover)&&q.turnover>=5e9&&q.time>0&&now-q.time<=600000&&q.time<=now+60000;}
 static S evaluate(Signals.Quote q,List<Signals.Bar>raw,List<Signals.Bar>btcRaw,long now){
  if(!pool(q,now))return null;
  List<Signals.Bar>b=Signals.closed(raw,now,24*Signals.HOUR),btc=Signals.closed(btcRaw,now,24*Signals.HOUR);
  if(!continuous(b,now,125)||!continuous(btc,now,31)||b.get(b.size()-1).time!=btc.get(btc.size()-1).time)return null;
  S s=new S();s.ma20=Signals.sma(b,20);s.ma60=Signals.sma(b,60);s.ma120=Signals.sma(b,120);s.rsi=Signals.rsi(b,14);s.atr=Signals.atr(b,14);
  s.ret30=(b.get(b.size()-1).close/b.get(b.size()-31).close-1)*100;s.relative30=s.ret30-(btc.get(btc.size()-1).close/btc.get(btc.size()-31).close-1)*100;
  double last=0,previous=0;for(int i=b.size()-7;i<b.size();i++)last+=b.get(i).volume;for(int i=b.size()-28;i<b.size()-7;i++)previous+=b.get(i).volume;
  s.volumeRatio=previous>0?(last/7)/(previous/21):Double.NaN;
  if(!Signals.finite(s.volumeRatio)||!Signals.finite(s.rsi)||!Signals.finite(s.atr)||s.atr<=0)return null;
  boolean momentum=s.volumeRatio>=1&&s.ret30>0&&s.relative30>=0&&s.rsi>=45&&s.rsi<=68&&s.atr/q.price*100<=8;
  boolean trend=q.price>=s.ma20&&s.ma20>s.ma60&&s.ma60>s.ma120&&s.ma60>Signals.sma(b.subList(0,b.size()-5),60);
  s.low=Double.MAX_VALUE;s.high=0;for(int i=b.size()-60;i<b.size();i++){s.high=Math.max(s.high,b.get(i).high);if(i>=b.size()-20)s.low=Math.min(s.low,b.get(i).low);}
  s.entryHigh=Math.min(q.price,s.ma20+.3*s.atr);s.entryLow=Math.min(s.entryHigh,s.ma20-.3*s.atr);double middle=(s.entryLow+s.entryHigh)/2;
  s.rr=middle>s.low&&s.high>middle?(s.high-middle)/(middle-s.low):0;
  boolean geometry=s.low<s.entryLow&&s.high>s.entryHigh&&s.entryLow>0&&s.rr>=1.3&&q.price<=s.ma20+1.5*s.atr;
  s.qualified=Signals.eligible(q,now)&&q.turnover>=1e10&&q.dayPct<=12&&now-q.time<=120000&&momentum&&trend&&geometry;
  s.risk=!trend?"20·60·120일 상승 추세 미충족":s.ret30<=0?"최근 30일 하락":s.relative30<0?"30일 BTC보다 약함":s.volumeRatio<1?"최근 7일 거래대금 감소":s.rsi<45||s.rsi>68?"RSI 적정 구간 밖":s.atr/q.price*100>8?"일봉 변동성 큼":!geometry?"지지·저항 가격 조건 미충족":!s.qualified?"거래대금·당일 등락 조건 미충족":"조건 충족 · 저점 이탈 시 재검토";
  s.score=Research.clamp(30+(trend?25:0)+Research.clamp(s.relative30*.5,-15,15)+Research.clamp(s.ret30*.3,-10,10)+Research.clamp((s.volumeRatio-1)*10,-10,10)+(s.rsi>=45&&s.rsi<=68?10:0));return s;
 }
}
