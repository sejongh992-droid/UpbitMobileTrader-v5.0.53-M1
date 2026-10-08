package kr.sejong.coinwidget;
import java.util.*;
import java.time.*;
public class StrategyTest {
 static int tests;static void ok(boolean x,String label){tests++;if(!x)throw new AssertionError(label);}
 static long kst(int day,int hour,int minute){return ZonedDateTime.of(2026,10,day,hour,minute,0,0,ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();}
 static List<Signals.Bar> pulse(long now){List<Signals.Bar>b=new ArrayList<>();for(int i=0;i<16;i++){double p=100+i*.1;b.add(new Signals.Bar(now-(16-i)*300000,p-.08,p+.15,p-.15,p,i>=13?200:100));}return b;}
 public static void main(String[]args){
  ok(!BeforeNine.entryWindow(kst(9,7,59)),"before entry window");ok(BeforeNine.entryWindow(kst(9,8,0)),"entry begins 08");ok(BeforeNine.entryWindow(kst(9,8,59)),"pre-open entry");ok(!BeforeNine.entryWindow(kst(9,9,0)),"09 entry ends");
  ok(BeforeNine.nextCheck(kst(9,8,29))==kst(9,8,30),"08:30 schedule");ok(BeforeNine.nextCheck(kst(9,8,30))==kst(9,8,50),"08:50 schedule");ok(BeforeNine.nextCheck(kst(9,8,50))==kst(10,8,30),"tomorrow schedule");ok(Research.nextNine(kst(9,8,59))==kst(9,9,0),"09 deadline");ok(Research.nextNine(kst(9,9,0))==kst(10,9,0),"new target after 09");
  long now=kst(9,8,50);List<Signals.Bar>b=pulse(now);BeforeNine.Pulse p=BeforeNine.pulse(b,now);ok(p!=null&&p.passes,"growing volume and positive short momentum pass");ok(Math.abs(p.ratio-2)<1e-9,"15 vs prior60 means");ok(p.through==now,"completed bar end");
  List<Signals.Bar> running=new ArrayList<>(b);running.add(new Signals.Bar(now,100,500,1,400,1e8));ok(Math.abs(BeforeNine.pulse(running,now).ratio-p.ratio)<1e-9,"unfinished spike ignored");
  b.remove(7);ok(BeforeNine.pulse(b,now)==null,"missing bar rejects");ok(BeforeNine.pulse(pulse(now),now+300000)==null,"stale pulse rejects");
  b=pulse(now);Signals.Bar last=b.get(b.size()-1);b.set(b.size()-1,new Signals.Bar(last.time,last.open,150,last.low,140,last.volume));ok(!BeforeNine.pulse(b,now).passes,"overextended 15min rejects");
  b=pulse(now);for(int i=0;i<b.size();i++){Signals.Bar a=b.get(i);b.set(i,new Signals.Bar(a.time,a.open,a.high,a.low,a.close,100));}ok(!BeforeNine.pulse(b,now).passes,"no volume acceleration rejects");
  List<Signals.Bar> hours=new ArrayList<>(),btc=new ArrayList<>();for(int i=0;i<75;i++){double v=100+i*.12+2*Math.sin(i*.7+1);hours.add(new Signals.Bar(now-(75-i)*Signals.HOUR,v-.1,v+.7,v-.7,v,i==74?200:100));btc.add(new Signals.Bar(now-(75-i)*Signals.HOUR,100,101,99,100,100));}
  Signals.Quote q=new Signals.Quote("KRW-TEST","test",hours.get(74).close,5,2e10,false,now);Research.S s=Research.evaluate(q,hours,btc,now,false);ok(s!=null&&s.qualified,"balanced rising trend can qualify");ok(s.entryHi<=q.price&&s.entryLo>s.low&&s.entryHi<s.high,"entry geometry");ok(!Research.evaluate(q,hours,btc,now,true).qualified,"market defense veto");ok(Research.evaluate(q,hours,btc,now+180000,false)==null,"old trade cannot qualify");
  Signals.Quote warning=new Signals.Quote(q.market,q.name,q.price,5,2e10,true,now);ok(Research.evaluate(warning,hours,btc,now,false)==null,"warning coin rejected");
  List<Signals.Bar> days=new ArrayList<>(),bd=new ArrayList<>();for(int i=0;i<150;i++){double v=100+i*.22+5*Math.sin(i*.6+1.6);days.add(new Signals.Bar(now-(150-i)*24*Signals.HOUR,v-.1,v+(i==110?25:1.8),v-1.8,v,i>=143?200:100));bd.add(new Signals.Bar(now-(150-i)*24*Signals.HOUR,100,101,99,100,100));}
  boolean found=false;for(int trim=0;trim<10;trim++){List<Signals.Bar> sample=days.subList(0,150-trim);long sampleNow=now-trim*24*Signals.HOUR;Signals.Quote daily=new Signals.Quote("KRW-TEST","test",sample.get(sample.size()-1).close,3,2e10,false,sampleNow);LongerTerm.S d=LongerTerm.evaluate(daily,sample,bd.subList(0,150-trim),sampleNow);if(d!=null&&d.qualified){found=true;ok(d.ma20>d.ma60&&d.ma60>d.ma120,"daily averages ordered");ok(d.entryLow>d.low&&d.entryHigh<d.high,"long horizon geometry");ok(d.volumeRatio>=1&&d.relative30>=0,"long liquidity and relative momentum");ok(LongerTerm.evaluate(daily,sample.subList(0,100),bd,sampleNow)==null,"long history required");break;}}
  ok(found,"long-term filter admits a balanced rising fixture");
  ok(LongerTerm.SCAN_LIMIT==30&&LongerTerm.DISPLAY_LIMIT==15,"bounded long scan");System.out.println("PASS StrategyTest: "+tests+" assertions (timing, momentum, freshness, trend, geometry)");
 }
}
