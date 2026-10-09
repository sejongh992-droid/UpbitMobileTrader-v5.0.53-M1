package kr.sejong.coinwidget;
import java.util.*;
/** Fixed candidate rules recorded before their historical evaluation. No calibrated probability. */
final class StrategyV130 {
 static final double FEE=.0005,SLIP=.0005;
 static final int SCAN=30;
 static final class Result {
  boolean pass;String risk="",reason="";
  double score,ma20,ma60,ma120,rsi,atr,ret6,ret12,rel6,ret30,rel30,ret90,rel90,volume,pulseRatio,pulseChange,support,resistance,netRoom,netRisk,rr,compression,position;
  long through;
 }
 static boolean universe(Signals.Quote q,long now){return q!=null&&Signals.isAlt(q.market)&&!q.warning&&Signals.finite(q.price)&&q.price>0&&Signals.finite(q.turnover)&&q.turnover>=5e9&&q.time>0&&now>=q.time&&now-q.time<=120000&&Signals.finite(q.dayPct);}
 static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
 static double ret(List<Signals.Bar>b,int n){return b.size()>n?(b.get(b.size()-1).close/b.get(b.size()-1-n).close-1)*100:Double.NaN;}
 static double low(List<Signals.Bar>b,int n){double x=Double.MAX_VALUE;for(int i=b.size()-n;i<b.size();i++)x=Math.min(x,b.get(i).low);return x;}
 static double high(List<Signals.Bar>b,int n){double x=0;for(int i=b.size()-n;i<b.size();i++)x=Math.max(x,b.get(i).high);return x;}
 static double net(double entry,double exit){return (exit*(1-FEE)*(1-SLIP)/(entry*(1+FEE)*(1+SLIP))-1)*100;}
 static void geometry(Result s,double price){s.netRoom=net(price,s.resistance);s.netRisk=-net(price,s.support);s.rr=s.netRisk>0?s.netRoom/s.netRisk:0;}
 static void need(List<String>fail,boolean ok,String why){if(!ok)fail.add(why);}
 static Result shortRule(int mode,Signals.Quote q,List<Signals.Bar>hourRaw,List<Signals.Bar>btcRaw,List<Signals.Bar>fiveRaw,long now){
  if(!universe(q,now))return null;
  List<Signals.Bar>h=Signals.closed(hourRaw,now,Signals.HOUR),b=Signals.closed(btcRaw,now,Signals.HOUR),f=Signals.closed(fiveRaw,now,300000);
  if(!Signals.hourlyContinuous(h,now,60)||!Signals.hourlyContinuous(b,now,25)||h.get(h.size()-1).time!=b.get(b.size()-1).time)return null;
  BeforeNine.Pulse p=BeforeNine.pulse(f,now);if(p==null||f.size()<18)return null;
  Result s=new Result();s.ma20=Signals.sma(h,20);s.ma60=Signals.sma(h,60);s.rsi=Signals.rsi(h,14);s.atr=Signals.atr(h,14);s.ret6=ret(h,6);s.ret12=ret(h,12);s.rel6=s.ret6-ret(b,6);s.pulseRatio=p.ratio;s.pulseChange=p.change;s.through=p.through;
  double prior=0;for(int i=h.size()-21;i<h.size()-1;i++)prior+=h.get(i).volume;s.volume=prior>0?h.get(h.size()-1).volume/(prior/20):Double.NaN;
  double ap=s.atr/q.price*100,dist=(q.price-s.ma20)/s.atr;
  if(!Signals.finite(s.rsi)||!Signals.finite(ap)||ap<=0||!Signals.finite(s.volume)||!Signals.finite(s.rel6))return null;
  List<String>fail=new ArrayList<>();
  if(mode==0){
   s.support=low(h,6);s.resistance=high(h,20);geometry(s,q.price);
   boolean trend=q.price>=s.ma20&&s.ma20>=s.ma60&&s.ma20>Signals.sma(h.subList(0,h.size()-3),20);
   need(fail,trend,"20·60시간 추세 미충족");need(fail,s.ret6>0&&s.ret12>0&&s.rel6>0,"6·12시간 상승·BTC 대비 강도 부족");
   need(fail,ret(b,1)>=-.6&&ret(b,6)>=-.5,"BTC 단기 약세");need(fail,s.volume>=1,"직전 시간 거래대금 부족");
   need(fail,s.rsi>=45&&s.rsi<=70&&ap<=3.5&&dist<=1.5,"과열·변동성·추격 거리 초과");
   need(fail,p.ratio>=1.2&&p.change>=.1&&p.change<=2.5&&p.rising>=2,"15분 상승·거래 증가 부족");
   need(fail,q.dayPct>=-4&&q.dayPct<=12,"09시 기준 등락 범위 밖");
   need(fail,s.support<q.price&&s.netRoom>=.6&&s.netRisk>0&&s.netRisk<=3&&s.rr>=1.2,"현재가·비용 기준 가격 여유 부족");
   s.score=clamp(50+clamp(s.rel6/Math.max(ap,.25)*12,-25,25)+clamp((p.ratio-1)*8,-10,12)+(trend?10:0)+clamp(p.change*5,-10,10)-Math.max(0,dist-1)*8,0,100);
   s.reason="시간봉 상대강도 + 완료 15분 거래 증가 · 현재가 기준 비용 차감";
  }else{
   List<Signals.Bar>priorFive=f.subList(0,f.size()-3);double lo=low(priorFive,12),hi=high(priorFive,12);
   s.compression=(hi-lo)/q.price*100;s.position=hi>lo?(f.get(f.size()-1).close-lo)/(hi-lo):Double.NaN;
   s.support=lo;s.resistance=high(h,12);geometry(s,q.price);
   need(fail,q.price>=s.ma20&&s.rel6>=0,"20시간 평균·BTC 대비 강도 부족");need(fail,ret(b,1)>=-.4&&ret(b,6)>=-.6,"BTC 단기 약세");
   need(fail,s.compression>0&&s.compression<=3&&s.position>=.65&&s.position<=1.25,"직전 1시간 압축·상단 접근 미충족");
   need(fail,p.ratio>=1.5&&p.change>=.15&&p.change<=1.8&&p.rising>=2,"15분 거래 가속·완만한 상승 부족");
   need(fail,s.rsi>=45&&s.rsi<=70&&ap<=3.5&&dist<=1.5&&q.dayPct>=-4&&q.dayPct<=10,"과열·변동성·추격 조건 초과");
   need(fail,s.support<q.price&&s.netRoom>=.4&&s.netRisk>0&&s.netRisk<=1.5&&s.rr>=1.2,"현재가·비용 기준 단기 여유 부족");
   s.score=clamp(35+clamp(s.rel6/Math.max(ap,.25)*8,-15,15)+clamp((p.ratio-1)*10,0,20)+(s.position>=.65&&s.position<=1.25?10:0)+(s.compression<=2?10:0)+clamp(p.change*5,-10,8)-Math.max(0,dist-1)*8,0,100);
   s.reason="9시 전 독립 기준: 가격 압축 + 상단 접근 + 거래 가속";
  }
  s.pass=fail.isEmpty();s.risk=s.pass?"차트 규칙 통과 · 검증 상태 별도 확인":String.join(" / ",fail);return s;
 }
 static Result longRule(Signals.Quote q,List<Signals.Bar>raw,List<Signals.Bar>btcRaw,long now){
  if(!universe(q,now))return null;long day=24*Signals.HOUR;List<Signals.Bar>d=Signals.closed(raw,now,day),b=Signals.closed(btcRaw,now,day);
  if(!LongerTerm.continuous(d,now,125)||!LongerTerm.continuous(b,now,125)||d.get(d.size()-1).time!=b.get(b.size()-1).time)return null;
  Result s=new Result();s.ma20=Signals.sma(d,20);s.ma60=Signals.sma(d,60);s.ma120=Signals.sma(d,120);s.rsi=Signals.rsi(d,14);s.atr=Signals.atr(d,14);s.ret30=ret(d,30);s.rel30=s.ret30-ret(b,30);s.ret90=ret(d,90);s.rel90=s.ret90-ret(b,90);
  double last=0,prior=0;for(int i=d.size()-7;i<d.size();i++)last+=d.get(i).volume;for(int i=d.size()-28;i<d.size()-7;i++)prior+=d.get(i).volume;s.volume=prior>0?(last/7)/(prior/21):Double.NaN;
  double ap=s.atr/q.price*100,dist=(q.price-s.ma20)/s.atr;if(!Signals.finite(ap)||ap<=0||!Signals.finite(s.volume)||!Signals.finite(s.rel90))return null;
  s.support=Math.max(low(d,20),s.ma60);s.resistance=high(d,60);geometry(s,q.price);s.through=d.get(d.size()-1).time+day;
  boolean trend=q.price>=s.ma20&&s.ma20>s.ma60&&s.ma60>s.ma120&&s.ma60>Signals.sma(d.subList(0,d.size()-5),60);
  List<String>fail=new ArrayList<>();need(fail,trend,"20·60·120일 추세 미충족");need(fail,s.ret30>0&&s.ret90>0&&s.rel30>=0&&s.rel90>=0,"30·90일 상승·BTC 대비 강도 부족");
  need(fail,Signals.sma(b,20)>=Signals.sma(b,60)&&b.get(b.size()-1).close>=Signals.sma(b,60),"BTC 중기 추세 약세");
  need(fail,s.volume>=1,"7일 거래대금 감소");need(fail,s.rsi>=45&&s.rsi<=70&&ap<=6&&dist<=1,"과열·변동성·추격 거리 초과");
  need(fail,q.turnover>=1e10&&q.dayPct>=-4&&q.dayPct<=12,"거래대금·당일 등락 조건 미충족");
  need(fail,s.support<q.price&&s.netRoom>=3&&s.netRisk>0&&s.netRisk<=15&&s.rr>=1.3,"현재가·비용 기준 중기 여유 부족");
  s.score=clamp(40+clamp(s.rel30/Math.max(ap,1)*3,-15,20)+clamp(s.rel90/Math.max(ap,1),-10,15)+(trend?15:0)+clamp((s.volume-1)*10,-10,10)-Math.max(0,dist)*5,0,100);
  s.pass=fail.isEmpty();s.risk=s.pass?"차트 규칙 통과 · 검증 상태 별도 확인":String.join(" / ",fail);s.reason="30·90일 상대강도를 변동성으로 조정 · BTC 추세·실제 가격 여유 확인";return s;
 }
}
