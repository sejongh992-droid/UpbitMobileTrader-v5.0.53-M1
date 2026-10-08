package kr.sejong.coinwidget;
import java.time.*;
import java.util.*;
/** Pre-09:00 KST entry screening. This is a rule filter, not a calibrated forecast. */
final class BeforeNine {
 static final long MINUTE=60_000L;
 static long target(long now){return Research.nextNine(now);}
 static boolean entryWindow(long now){int hour=Instant.ofEpochMilli(now).atZone(ZoneId.of("Asia/Seoul")).getHour();return hour==8;}
 static long nextCheck(long now){
  ZonedDateTime k=Instant.ofEpochMilli(now).atZone(ZoneId.of("Asia/Seoul"));
  for(int minute:new int[]{30,50}){long t=k.toLocalDate().atTime(8,minute).atZone(k.getZone()).toInstant().toEpochMilli();if(t>now)return t;}
  return k.toLocalDate().plusDays(1).atTime(8,30).atZone(k.getZone()).toInstant().toEpochMilli();
 }
 static final class Pulse {double ratio,change;int rising;long through;boolean passes;}
 static Pulse pulse(List<Signals.Bar> raw,long now){
  List<Signals.Bar>b=Signals.closed(raw,now,5*MINUTE);if(b.size()<15)return null;int n=b.size();
  if(now-(b.get(n-1).time+5*MINUTE)>=5*MINUTE)return null;
  for(int i=n-14;i<n;i++)if(b.get(i).time-b.get(i-1).time!=5*MINUTE)return null;
  double previous=0,recent=0;for(int i=n-15;i<n-3;i++)previous+=b.get(i).volume;
  Pulse p=new Pulse();for(int i=n-3;i<n;i++){recent+=b.get(i).volume;if(b.get(i).close>b.get(i).open)p.rising++;}
  if(previous<=0)return null;p.ratio=(recent/3)/(previous/12);p.change=(b.get(n-1).close/b.get(n-4).close-1)*100;p.through=b.get(n-1).time+5*MINUTE;
  p.passes=Signals.finite(p.ratio)&&Signals.finite(p.change)&&p.ratio>=1.3&&p.change>=.15&&p.change<=3&&p.rising>=2;return p;
 }
}
