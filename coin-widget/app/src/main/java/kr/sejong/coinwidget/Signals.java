package kr.sejong.coinwidget;
import java.util.*;
/** Pure deterministic indicators. Observation only; no orders or profit guarantee. */
public final class Signals {
 private Signals(){}
 public static final long HOUR=3_600_000L;
 public static final class Bar {
  public final long time; public final double open,high,low,close,volume;
  public Bar(long t,double o,double h,double l,double c,double v){time=t;open=o;high=h;low=l;close=c;volume=v;}
  public boolean valid(){return time>0&&finite(open)&&finite(high)&&finite(low)&&finite(close)&&finite(volume)&&low>0&&high>=Math.max(open,close)&&low<=Math.min(open,close)&&volume>=0;}
 }
 public static final class Quote {
  public final String market,name; public final double price,dayPct,turnover; public final boolean warning; public final long time;
  public Quote(String m,String n,double p,double d,double v,boolean w,long t){market=m;name=n;price=p;dayPct=d;turnover=v;warning=w;time=t;}
 }
 public static final class Candidate {
  public final Quote quote; public final double score,rsi,atrPct,volumeRatio,relative6h,resistance,support;
  public Candidate(Quote q,double s,double r,double a,double v,double rel,double hi,double lo){quote=q;score=s;rsi=r;atrPct=a;volumeRatio=v;relative6h=rel;resistance=hi;support=lo;}
 }
 public static boolean finite(double n){return !Double.isNaN(n)&&!Double.isInfinite(n);}
 public static boolean isAlt(String market){if(market==null||!market.startsWith("KRW-"))return false;return !Arrays.asList("BTC","USDT","USDC","DAI","USDE","USDD","TUSD","FDUSD","PYUSD","USD1","USDS").contains(market.substring(4));}
 public static boolean eligible(Quote q,long now){return q!=null&&isAlt(q.market)&&!q.warning&&finite(q.price)&&q.price>0&&finite(q.turnover)&&q.turnover>=5_000_000_000.0&&finite(q.dayPct)&&q.dayPct>=-4&&q.dayPct<=20&&q.time>0&&now-q.time<=10*60_000L&&q.time<=now+60_000L;}
 public static List<Bar> closed(List<Bar> input,long now,long period){TreeMap<Long,Bar>s=new TreeMap<>();if(input!=null)for(Bar b:input)if(b!=null&&b.valid()&&b.time+period<=now)s.put(b.time,b);return new ArrayList<>(s.values());}
 public static double sma(List<Bar>b,int p){if(p<=0||b.size()<p)return Double.NaN;double s=0;for(int i=b.size()-p;i<b.size();i++)s+=b.get(i).close;return s/p;}
 public static double rsi(List<Bar>b,int p){
  if(p<=0||b.size()<p+1)return Double.NaN;double gain=0,loss=0;
  for(int i=1;i<=p;i++){double d=b.get(i).close-b.get(i-1).close;gain+=Math.max(0,d);loss+=Math.max(0,-d);}gain/=p;loss/=p;
  for(int i=p+1;i<b.size();i++){double d=b.get(i).close-b.get(i-1).close;gain=(gain*(p-1)+Math.max(0,d))/p;loss=(loss*(p-1)+Math.max(0,-d))/p;}
  if(gain==0&&loss==0)return 50;return loss==0?100:100-100/(1+gain/loss);
 }
 public static double atr(List<Bar>b,int p){if(p<=0||b.size()<p+1)return Double.NaN;double v=0;for(int i=1;i<=p;i++)v+=tr(b.get(i),b.get(i-1).close);v/=p;for(int i=p+1;i<b.size();i++)v=(v*(p-1)+tr(b.get(i),b.get(i-1).close))/p;return v;}
 private static double tr(Bar b,double prev){return Math.max(b.high-b.low,Math.max(Math.abs(b.high-prev),Math.abs(b.low-prev)));}
 public static boolean hourlyContinuous(List<Bar>b,long now,int count){if(b.size()<count||now-(b.get(b.size()-1).time+HOUR)>=HOUR||now<(b.get(b.size()-1).time+HOUR))return false;for(int i=b.size()-count+1;i<b.size();i++)if(b.get(i).time-b.get(i-1).time!=HOUR)return false;return true;}
 public static double returnHours(List<Bar>b,int hours){if(b.size()<=hours)return Double.NaN;int n=b.size()-1;if(b.get(n).time-b.get(n-hours).time!=hours*HOUR)return Double.NaN;return (b.get(n).close/b.get(n-hours).close-1)*100;}
 public static double screeningScore(Quote q,double btcDay){return Math.log10(Math.max(q.turnover,1))*3+(q.dayPct-btcDay)*1.4-Math.max(0,q.dayPct-12)*2;}
 public static Candidate analyze(Quote q,List<Bar>input,List<Bar>btcInput,long now){
  if(!eligible(q,now))return null;List<Bar>b=closed(input,now,HOUR),btc=closed(btcInput,now,HOUR);
  if(b.size()<60||!hourlyContinuous(b,now,60)||!hourlyContinuous(btc,now,7))return null;
  if(b.get(b.size()-1).time!=btc.get(btc.size()-1).time)return null;
  double ma20=sma(b,20),ma60=sma(b,60),r=rsi(b,14),a=atr(b,14)/q.price*100;
  double rel=returnHours(b,6)-returnHours(btc,6),v=0;
  for(int i=b.size()-21;i<b.size()-1;i++)v+=b.get(i).volume;
  double vr=v>0?b.get(b.size()-1).volume/(v/20):Double.NaN;
  if(!finite(rel)||!finite(r)||!finite(vr)||!finite(a))return null;
  if(q.price<ma20||ma20<ma60||r<40||r>76||a>5||a<0.05||rel<-0.5||vr<0.7)return null;
  double hi=0,lo=Double.MAX_VALUE;
  for(int i=b.size()-20;i<b.size();i++)hi=Math.max(hi,b.get(i).high);
  for(int i=b.size()-12;i<b.size();i++)lo=Math.min(lo,b.get(i).low);
  double score=40+Math.min(15,Math.max(0,rel)*3)+Math.min(15,Math.max(0,vr-0.7)*10)+(r>=48&&r<=65?10:4)+Math.min(10,Math.max(0,Math.log10(q.turnover)-9)*5)+(q.dayPct>=0&&q.dayPct<=10?10:3)-Math.max(0,a-2)*5;
  score=Math.min(100,Math.max(0,score));return score>=60?new Candidate(q,score,r,a,vr,rel,hi,lo):null;
 }
 public static String btcRegime(double price,List<Bar>bars,long now){
  if(!finite(price)||price<=0)return "판단 보류(시세 오류)";List<Bar>b=closed(bars,now,24*HOUR);
  if(b.size()<60||now-(b.get(b.size()-1).time+24*HOUR)>=24*HOUR)return "판단 보류(일봉 부족)";
  for(int i=b.size()-59;i<b.size();i++)if(b.get(i).time-b.get(i-1).time!=24*HOUR)return "판단 보류(일봉 누락)";
  double m20=sma(b,20),m60=sma(b,60);if(price>m20&&m20>m60)return "상승 우세";if(price<m20&&m20<m60)return "하락 우세";return "조정·혼조";
 }
 public static double dominanceDelta24(long currentTime,double current,List<double[]>history){double val=Double.NaN;long distance=Long.MAX_VALUE;for(double[]p:history){if(p.length<2||!finite(p[1]))continue;long d=Math.abs((long)p[0]-(currentTime-24*HOUR));if(d<=3*HOUR&&d<distance&&p[0]<currentTime){distance=d;val=p[1];}}return finite(val)?current-val:Double.NaN;}
 public static String altRegime(double breadth,String btc,double domDelta){if(!finite(breadth)||btc==null||btc.startsWith("판단 보류"))return "판단 보류";if(breadth<35||btc.startsWith("하락"))return "약세·방어 관찰";if(breadth>=65&&btc.startsWith("상승")&&finite(domDelta)&&domDelta<=-0.2)return "알트 강세 조건 일부 충족";if(breadth>=65)return "상승 종목 확산(불장 확정 아님)";return "선별·혼조";}
}
