import kr.sejong.coinwidget.Signals;
import java.util.*;
/** Synthetic regression tests, not a profitability backtest. */
public final class SignalsTest {
 static int tests=0;static final long H=Signals.HOUR,BASE=1_700_000_000_000L;
 static void check(boolean x,String label){tests++;if(!x)throw new AssertionError(label);System.out.println("PASS "+label);}
 static void near(double x,double y,String label){check(Math.abs(x-y)<1e-7,label+" ["+x+"]");}
 static List<Signals.Bar> series(int n,double step,long period){List<Signals.Bar>a=new ArrayList<>();for(int i=0;i<n;i++){double p=100+i*step;a.add(new Signals.Bar(BASE+i*period,p,p+1,p-1,p,100));}return a;}
 static Signals.Quote quote(String s,double d,double v,boolean w,long t){return new Signals.Quote(s,s,110,d,v,w,t);}
 public static void main(String[]args){
  List<Signals.Bar>up=series(75,1,H),down=series(75,-0.5,H),flat=series(75,0,H);
  near(Signals.rsi(up,14),100,"RSI rising");near(Signals.rsi(down,14),0,"RSI falling");near(Signals.rsi(flat,14),50,"RSI flat");
  check(Double.isNaN(Signals.rsi(series(10,1,H),14)),"RSI insufficient");
  near(Signals.sma(up,20),164.5,"SMA20");near(Signals.atr(up,14),2,"ATR14 true range");
  check(Double.isNaN(Signals.sma(up,80)),"SMA insufficient");check(Double.isNaN(Signals.atr(series(2,0,H),14)),"ATR insufficient");
  check(!new Signals.Bar(BASE,10,9,8,10,1).valid(),"reject invalid OHLC");check(!new Signals.Bar(BASE,10,11,9,Double.NaN,1).valid(),"reject NaN");
  long now=BASE+75*H;
  List<Signals.Bar>input=new ArrayList<>(up);input.add(new Signals.Bar(now,200,201,199,200,10));Collections.reverse(input);input.add(up.get(5));
  List<Signals.Bar>closed=Signals.closed(input,now,H);check(closed.size()==75,"exclude live bar and deduplicate");check(closed.get(0).time==BASE,"sort oldest first");
  check(Signals.hourlyContinuous(up,now,24),"continuous hourly");List<Signals.Bar>gap=new ArrayList<>(up);gap.remove(66);check(!Signals.hourlyContinuous(gap,now,24),"reject hourly gap");
  check(!Signals.hourlyContinuous(up,now+3*H,24),"reject old candles");near(Signals.returnHours(flat,6),0,"six hour return");
  check(Signals.eligible(quote("KRW-ETH",5,5e9,false,now),now),"liquidity lower boundary accepted");
  check(!Signals.eligible(quote("KRW-ETH",5,4.9e9,false,now),now),"low turnover excluded");
  check(!Signals.eligible(quote("KRW-ETH",5,1e10,true,now),now),"warning excluded");
  check(!Signals.eligible(quote("KRW-USDT",0,1e10,false,now),now),"stablecoin excluded");
  check(!Signals.eligible(quote("KRW-BTC",1,1e10,false,now),now),"BTC excluded from alts");
  check(!Signals.eligible(quote("KRW-ETH",20.1,1e10,false,now),now),"overheated daily move excluded");
  check(!Signals.eligible(quote("KRW-ETH",-4.1,1e10,false,now),now),"large daily fall excluded");
  check(!Signals.eligible(quote("KRW-ETH",5,1e10,false,now-601000),now),"stale ticker excluded");
  check(!Signals.eligible(quote("KRW-ETH",5,1e10,false,now+61000),now),"future ticker excluded");
  List<Signals.Bar>wave=new ArrayList<>();for(int i=0;i<75;i++){double p=100+i*.12+2*Math.sin(i*.7);wave.add(new Signals.Bar(BASE+i*H,p,p+.7,p-.7,p,i==74?200:100));}
  Signals.Quote q=new Signals.Quote("KRW-TEST","TEST",wave.get(74).close,5,2e10,false,now);Signals.Candidate candidate=Signals.analyze(q,wave,flat,now);
  check(candidate!=null,"positive observation fixture");check(candidate.score>=60&&candidate.score<=100,"score within range");near(candidate.volumeRatio,2,"completed hourly turnover ratio");
  check(Signals.analyze(q,wave,flat,now+3*H)==null,"stale recommendation rejected");
  check(Signals.analyze(q,wave.subList(20,75),flat,now)==null,"insufficient hourly history rejected");
  check(Signals.analyze(q,wave,flat.subList(0,74),now)==null,"unaligned BTC comparison rejected");
  check(Signals.analyze(new Signals.Quote("KRW-TEST","TEST",174,5,2e10,false,now),up,flat,now)==null,"overbought RSI rejected");
  List<Signals.Bar>days=series(90,1,24*H);long dayNow=BASE+90*24*H;
  check(Signals.btcRegime(191,days,dayNow).equals("상승 우세"),"BTC bull rule");
  check(Signals.btcRegime(53,series(90,-.5,24*H),dayNow).equals("하락 우세"),"BTC bear rule");
  check(Signals.btcRegime(100,days,dayNow).equals("조정·혼조"),"BTC mixed rule");
  check(Signals.btcRegime(100,days.subList(0,20),dayNow).startsWith("판단 보류"),"BTC insufficient day history");
  List<double[]>history=Arrays.asList(new double[]{now-24*H,55.4},new double[]{now-2*H,55.1},new double[]{now,55});
  near(Signals.dominanceDelta24(now,55,history),-.4,"dominance difference in percentage points");
  check(Double.isNaN(Signals.dominanceDelta24(now,55,Collections.singletonList(new double[]{now-H,54}))),"do not mislabel short change as 24h");
  check(Double.isNaN(Signals.dominanceDelta24(now,55,Collections.emptyList())),"dominance empty history");
  check(Signals.altRegime(80,"하락 우세",-.5).contains("방어"),"BTC weakness overrides broad advance");
  check(Signals.altRegime(75,"상승 우세",-.3).contains("일부 충족"),"alt strength conditional");
  check(Signals.altRegime(75,"상승 우세",Double.NaN).contains("확정 아님"),"missing dominance no bull-market certainty");
  check(Signals.altRegime(Double.NaN,"상승 우세",-.3).equals("판단 보류"),"missing breadth defers judgment");
  check(Signals.btcRegime(Double.NaN,days,dayNow).startsWith("판단 보류"),"invalid BTC price defers judgment");
  List<Signals.Bar>olderGap=new ArrayList<>(wave);olderGap.remove(30);check(Signals.analyze(q,olderGap,flat,now)==null,"gap inside 60-bar average rejected");
  System.out.println("TOTAL "+tests+" passed; synthetic unit tests only.");
 }
}
