package kr.sejong.coinwidget;
import java.util.*;
/** Daily-trend screen for a 1–3 month review horizon; no price/return forecast. */
final class LongerTerm {
 static final int SCAN_LIMIT=30,DISPLAY_LIMIT=15;
 static boolean continuous(List<Signals.Bar>b,long now,int count){
  long day=24*Signals.HOUR;if(b.size()<count||now-(b.get(b.size()-1).time+day)>=day)return false;
  for(int i=b.size()-count+1;i<b.size();i++)if(b.get(i).time-b.get(i-1).time!=day)return false;return true;
 }
 static final class S {double ma20,ma60,ma120,rsi,atr,ret30,relative30,volumeRatio,low,high,entryLow,entryHigh,rr,score;}
 static S evaluate(Signals.Quote q,List<Signals.Bar>raw,List<Signals.Bar>btcRaw,long now){
  if(!Signals.eligible(q,now)||now-q.time>120000||q.turnover<1e10||q.dayPct>12)return null;
  List<Signals.Bar>b=Signals.closed(raw,now,24*Signals.HOUR),btc=Signals.closed(btcRaw,now,24*Signals.HOUR);
  if(!continuous(b,now,125)||!continuous(btc,now,31)||b.get(b.size()-1).time!=btc.get(btc.size()-1).time)return null;
  S s=new S();s.ma20=Signals.sma(b,20);s.ma60=Signals.sma(b,60);s.ma120=Signals.sma(b,120);s.rsi=Signals.rsi(b,14);s.atr=Signals.atr(b,14);
  s.ret30=(b.get(b.size()-1).close/b.get(b.size()-31).close-1)*100;s.relative30=s.ret30-(btc.get(btc.size()-1).close/btc.get(btc.size()-31).close-1)*100;
  double last=0,previous=0;for(int i=b.size()-7;i<b.size();i++)last+=b.get(i).volume;for(int i=b.size()-28;i<b.size()-7;i++)previous+=b.get(i).volume;
  s.volumeRatio=previous>0?(last/7)/(previous/21):Double.NaN;
  if(!Signals.finite(s.volumeRatio)||s.volumeRatio<1||s.ret30<=0||s.relative30<0||s.rsi<45||s.rsi>68||s.atr/q.price*100>8)return null;
  if(q.price<s.ma20||s.ma20<=s.ma60||s.ma60<=s.ma120||s.ma60<=Signals.sma(b.subList(0,b.size()-5),60)||q.price>s.ma20+1.5*s.atr)return null;
  s.low=Double.MAX_VALUE;s.high=0;for(int i=b.size()-60;i<b.size();i++){s.high=Math.max(s.high,b.get(i).high);if(i>=b.size()-20)s.low=Math.min(s.low,b.get(i).low);}
  s.entryHigh=Math.min(q.price,s.ma20+.3*s.atr);s.entryLow=Math.min(s.entryHigh,s.ma20-.3*s.atr);double middle=(s.entryLow+s.entryHigh)/2;
  if(s.low>=s.entryLow||s.high<=s.entryHigh||s.entryLow<=0)return null;s.rr=(s.high-middle)/(middle-s.low);if(s.rr<1.3)return null;
  s.score=Research.clamp(55+Math.min(15,s.relative30*.5)+Math.min(15,(s.volumeRatio-1)*10)+(s.rsi<=60?10:5)+Math.min(5,s.rr));return s;
 }
}
