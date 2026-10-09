package kr.sejong.coinwidget;
import java.util.*;
public final class StrategyV130Test {
 static int tests;static void ok(boolean b,String s){tests++;if(!b)throw new AssertionError(s);}
 static final long NOW=1791503400000L;
 static Signals.Quote quote(double p,long t){return new Signals.Quote("KRW-TEST","테스트",p,1,2e10,false,t);}
 static List<Signals.Bar> hours(boolean btc){List<Signals.Bar>b=new ArrayList<>();for(int i=0;i<75;i++){int j=i-50;double p=btc?100:i<50?104-i*.065:100.3+j*.015+.24*Math.sin(j*.9);b.add(new Signals.Bar(NOW-(75-i)*Signals.HOUR,p-.03,p+1.5,p-.2,p,100));}return b;}
 static List<Signals.Bar> five(){List<Signals.Bar>b=new ArrayList<>();for(int i=0;i<18;i++){double p=i<15?100.45+.1*Math.sin(i):100.6+.1*(i-15);b.add(new Signals.Bar(NOW-(18-i)*300000,p-.1,Math.max(100.85,p+.05),100.15,p,i<15?100:200));}return b;}
 static List<Signals.Bar> days(boolean btc){List<Signals.Bar>b=new ArrayList<>();for(int i=0;i<150;i++){double p=btc?100:100+i*.22+5*Math.sin(i*.6+1.6);b.add(new Signals.Bar(NOW-(150-i)*24*Signals.HOUR,p-.1,p+(i==110?25:1.8),p-1.8,p,i>=143?200:100));}return b;}
 public static void main(String[]args){
  ok(StrategyV130.universe(quote(100,NOW),NOW),"fresh liquid alt admitted");ok(!StrategyV130.universe(quote(100,NOW+1),NOW),"future quote rejected");ok(!StrategyV130.universe(quote(100,NOW-120001),NOW),"stale quote rejected");ok(!StrategyV130.universe(quote(Double.NaN,NOW),NOW),"NaN rejected");
  ok(!StrategyV130.universe(new Signals.Quote("KRW-USDT","stable",100,1,2e10,false,NOW),NOW),"stable excluded");ok(!StrategyV130.universe(new Signals.Quote("KRW-TEST","warning",100,1,2e10,true,NOW),NOW),"warning excluded");
  double expected=(102*.9995*.9995/(100*1.0005*1.0005)-1)*100;ok(Math.abs(StrategyV130.net(100,102)-expected)<1e-12,"both-sided fees and slippage");ok(StrategyV130.net(100,100)<-.19,"flat price loses costs");
  StrategyV130.Result geometry=new StrategyV130.Result();geometry.support=98;geometry.resistance=104;StrategyV130.geometry(geometry,100);double before=geometry.rr;StrategyV130.geometry(geometry,103);ok(geometry.rr<before&&geometry.netRisk>4.9,"uses current higher entry rather than hypothetical discount");
  List<Signals.Bar>h=hours(false),b=hours(true),f=five();Signals.Quote q=quote(100.8,NOW);
  StrategyV130.Result pre=StrategyV130.shortRule(1,q,h,b,f,NOW),day=StrategyV130.shortRule(0,q,h,b,f,NOW);
  ok(pre!=null&&pre.pass,"independent pre09 fixture qualifies: "+(pre==null?"null":pre.risk));ok(day!=null&&!day.pass,"pre09 does not depend on day qualification");ok(pre.through==NOW,"latest completed pulse timestamp");
  List<Signals.Bar>future=new ArrayList<>(f);future.add(new Signals.Bar(NOW,100,1000,1,900,1e12));StrategyV130.Result ignore=StrategyV130.shortRule(1,q,h,b,future,NOW);ok(ignore!=null&&ignore.score==pre.score&&ignore.pass==pre.pass,"unfinished future candle excluded");
  List<Signals.Bar>gap=new ArrayList<>(h);gap.remove(gap.size()-8);ok(StrategyV130.shortRule(1,q,gap,b,f,NOW)==null,"hour gap rejects");gap=new ArrayList<>(f);gap.remove(gap.size()-8);ok(StrategyV130.shortRule(1,q,h,b,gap,NOW)==null,"five-minute gap rejects");ok(StrategyV130.shortRule(1,q,h,b,f,NOW+300000)==null,"stale quote/pulse rejects");
  List<Signals.Bar>d=days(false),bd=days(true);StrategyV130.Result lon=StrategyV130.longRule(quote(d.get(149).close,NOW),d,bd,NOW);ok(lon!=null,"long enough daily history");ok(Math.abs(lon.ret90-(d.get(149).close/d.get(59).close-1)*100)<1e-10,"90 day return uses past completed close");ok(StrategyV130.longRule(q,d.subList(30,150),bd,NOW)==null,"125 daily bars required");
  List<Signals.Bar>risingBtc=new ArrayList<>();for(int i=0;i<150;i++)risingBtc.add(new Signals.Bar(NOW-(150-i)*24*Signals.HOUR,100+i*2,101+i*2,99+i*2,100+i*2,100));StrategyV130.Result weak=StrategyV130.longRule(quote(d.get(149).close,NOW),d,risingBtc,NOW);ok(weak!=null&&weak.rel90<0&&!weak.pass&&weak.risk.contains("30·90일"),"relative 90 day underperformance blocks long entry");
  System.out.println("PASS StrategyV130Test: "+tests+" assertions (independent modes, real-price cost, no future candles, freshness, gaps, 90-day strength)");
 }
}
