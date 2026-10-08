package kr.sejong.coinwidget;
import android.graphics.*;
import org.json.*;
import java.util.*;
final class MiniCharts {
 static Paint p(int color,float size){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));return p;}
 static String won(double n){return n>=1e8?String.format(Locale.KOREA,"%.2f억",n/1e8):n>=10000?String.format(Locale.KOREA,"%,.0f만",n/10000):Charts.number(n);}
 static Bitmap btc(JSONArray data){
  Bitmap bm=Bitmap.createBitmap(720,360,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bm);c.drawColor(Color.WHITE);List<Signals.Bar>b;
  try{b=Repository.readBars(data);}catch(Exception e){b=Collections.emptyList();}
  if(b.size()<2){c.drawText("새로고침 후 실제 일봉 표시",16,180,p(0xff738096,25));return bm;}
  int start=Math.max(0,b.size()-45),n=b.size()-start;double lo=Double.MAX_VALUE,hi=0;
  for(int i=start;i<b.size();i++){lo=Math.min(lo,b.get(i).low);hi=Math.max(hi,b.get(i).high);}
  for(int i=start;i<b.size();i++)for(int period:new int[]{20,60})if(i>=period-1){double sum=0;for(int k=i-period+1;k<=i;k++)sum+=b.get(k).close;lo=Math.min(lo,sum/period);hi=Math.max(hi,sum/period);}
  double pad=Math.max((hi-lo)*.08,hi*.003);lo-=pad;hi+=pad;float top=20,bottom=308,right=595,step=(right-12)/n;
  for(int j=0;j<3;j++){float y=top+(bottom-top)*j/2;c.drawLine(12,y,right,y,p(0xffedf0f5,1));c.drawText(won(hi-(hi-lo)*j/2),605,y+9,p(0xff738096,22));}
  for(int i=start;i<b.size();i++){Signals.Bar a=b.get(i);float x=12+(i-start+.5f)*step;Paint q=p(a.close>=a.open?0xffdc4d65:0xff357bc4,1);q.setStrokeWidth(2);
   c.drawLine(x,y(a.high,lo,hi,top,bottom),x,y(a.low,lo,hi,top,bottom),q);float o=y(a.open,lo,hi,top,bottom),cc=y(a.close,lo,hi,top,bottom);c.drawRect(x-step*.3f,Math.min(o,cc),x+step*.3f,Math.max(Math.min(o,cc)+2,Math.max(o,cc)),q);
  }
  for(int period:new int[]{20,60}){Path path=new Path();boolean first=true;
   for(int i=start;i<b.size();i++)if(i>=period-1){double sum=0;for(int j=i-period+1;j<=i;j++)sum+=b.get(j).close;float x=12+(i-start+.5f)*step,yy=y(sum/period,lo,hi,top,bottom);if(first){path.moveTo(x,yy);first=false;}else path.lineTo(x,yy);}
   Paint q=p(period==20?0xffba892f:0xff4b9e93,1);q.setStyle(Paint.Style.STROKE);q.setStrokeWidth(3);c.save();c.clipRect(12,top,right,bottom);c.drawPath(path,q);c.restore();}
  c.drawText(Charts.date(b.get(start).time,"MM/dd"),12,346,p(0xff738096,23));c.drawText(Charts.date(b.get(b.size()-1).time,"MM/dd"),531,346,p(0xff738096,23));return bm;
 }
 static float y(double v,double lo,double hi,float t,float b){return (float)(b-(v-lo)/(hi-lo)*(b-t));}
 static Bitmap share(JSONObject dom){
  Bitmap bm=Bitmap.createBitmap(720,82,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bm);c.drawColor(Color.WHITE);double value=dom.optDouble("value",Double.NaN);
  if(!Signals.finite(value)){c.drawText("도미넌스 자료 조회 전",12,45,p(0xff738096,25));return bm;}
  c.drawText(String.format(Locale.KOREA,"비트코인 %.2f%%",value),12,26,p(0xff6751d8,26));
  String text=String.format(Locale.KOREA,"기타 코인 %.2f%%",100-value);Paint q=p(0xff66758b,26);c.drawText(text,708-q.measureText(text),26,q);
  c.drawRoundRect(12,42,708,68,13,13,p(0xffe5e8f2,1));c.save();c.clipRect(12,42,(float)(12+696*value/100),68);c.drawRoundRect(12,42,708,68,13,13,p(0xff7760dc,1));c.restore();return bm;
 }
}
