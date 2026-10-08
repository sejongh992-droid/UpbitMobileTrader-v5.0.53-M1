package kr.sejong.coinwidget;

import android.graphics.*;
import org.json.*;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class Charts {
    static final int INK=Color.rgb(31,45,60),MUTED=Color.rgb(104,117,130),UP=Color.rgb(210,65,68),DOWN=Color.rgb(51,113,182);
    private static Paint paint(int color,float size){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));return p;}
    static Bitmap btc(JSONArray data){
        Bitmap b=Bitmap.createBitmap(640,180,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);
        List<Signals.Bar>bars;
        try{bars=Repository.readBars(data==null?new JSONArray():data);}catch(Exception e){bars=Collections.emptyList();}
        if(bars.size()<2){c.drawText("새로고침하면 실제 일봉을 표시합니다",15,88,paint(MUTED,18));return b;}
        int start=Math.max(0,bars.size()-60),n=bars.size()-start;float left=8,right=553,top=27,bottom=153;
        double lo=Double.MAX_VALUE,hi=0;
        for(int i=start;i<bars.size();i++){lo=Math.min(lo,bars.get(i).low);hi=Math.max(hi,bars.get(i).high);}
        double pad=Math.max((hi-lo)*0.08,hi*0.002);lo-=pad;hi+=pad;
        Paint grid=paint(Color.rgb(230,234,238),13);
        for(int i=0;i<3;i++){float y=top+(bottom-top)*i/2;c.drawLine(left,y,right,y,grid);c.drawText(new DecimalFormat("#,##0").format((hi-(hi-lo)*i/2)/10000)+"만",559,y+4,paint(MUTED,13));}
        c.drawText("일봉 · 조회 시점 기준",8,17,paint(MUTED,13));
        c.drawText("MA20",382,17,paint(Color.rgb(174,121,45),13));c.drawText("MA60",459,17,paint(Color.rgb(80,130,96),13));
        float step=(right-left)/n,w=Math.max(2,step*0.60f);
        for(int i=start;i<bars.size();i++){
            Signals.Bar a=bars.get(i);float x=left+(i-start+0.5f)*step;Paint p=paint(a.close>=a.open?UP:DOWN,12);p.setStrokeWidth(1.3f);
            float yo=y(a.open,lo,hi,top,bottom),yc=y(a.close,lo,hi,top,bottom);
            c.drawLine(x,y(a.high,lo,hi,top,bottom),x,y(a.low,lo,hi,top,bottom),p);
            c.drawRect(x-w/2,Math.min(yo,yc),x+w/2,Math.max(Math.min(yo,yc)+1,Math.max(yo,yc)),p);
        }
        for(int period:new int[]{20,60}){
            Path path=new Path();boolean began=false;
            for(int i=start;i<bars.size();i++)if(i>=period-1){
                double sum=0;for(int j=i-period+1;j<=i;j++)sum+=bars.get(j).close;
                float x=left+(i-start+0.5f)*step,yy=y(sum/period,lo,hi,top,bottom);
                if(!began){path.moveTo(x,yy);began=true;}else path.lineTo(x,yy);
            }
            Paint line=paint(period==20?Color.rgb(174,121,45):Color.rgb(80,130,96),12);line.setStyle(Paint.Style.STROKE);line.setStrokeWidth(1.6f);c.save();c.clipRect(left,top,right,bottom);c.drawPath(path,line);c.restore();
        }
        c.drawText(date(bars.get(start).time,"MM/dd"),8,175,paint(MUTED,13));c.drawText(date(bars.get(bars.size()-1).time,"MM/dd"),511,175,paint(MUTED,13));
        return b;
    }
    private static float y(double v,double lo,double hi,float top,float bottom){return (float)(bottom-(v-lo)/(hi-lo)*(bottom-top));}
    static Bitmap dominance(JSONArray arr){
        Bitmap b=Bitmap.createBitmap(640,112,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);
        if(arr==null||arr.length()<2){
            c.drawText(arr!=null&&arr.length()==1?"첫 관측 저장 완료 · 다음 2시간 구간부터 선 표시":"설치 후 관측 기록이 쌓이면 표시합니다",10,57,paint(MUTED,17));return b;
        }
        double lo=Double.MAX_VALUE,hi=-Double.MAX_VALUE;List<double[]>pts=new ArrayList<>();
        for(int i=0;i<arr.length();i++){
            JSONArray p=arr.optJSONArray(i);if(p==null)continue;double t=p.optDouble(0),v=p.optDouble(1);
            if(!Signals.finite(t)||!Signals.finite(v))continue;pts.add(new double[]{t,v});lo=Math.min(lo,v);hi=Math.max(hi,v);
        }
        if(pts.size()<2)return b;
        double first=pts.get(0)[0],last=pts.get(pts.size()-1)[0];if(last<=first)return b;
        double pad=Math.max(0.1,(hi-lo)*0.15);lo-=pad;hi+=pad;
        Paint grid=paint(Color.rgb(230,234,238),13);c.drawLine(8,18,553,18,grid);c.drawLine(8,83,553,83,grid);
        Path path=new Path();boolean begun=false;double previous=0;
        for(double[]p:pts){float x=(float)(8+(p[0]-first)/(last-first)*545),yy=y(p[1],lo,hi,18,83);
            if(!begun || p[0]-previous>6*Signals.HOUR){path.moveTo(x,yy);begun=true;}else path.lineTo(x,yy);
            c.drawCircle(x,yy,2,paint(UP,12));previous=p[0];}
        Paint line=paint(UP,12);line.setStyle(Paint.Style.STROKE);line.setStrokeWidth(2.4f);c.drawPath(path,line);
        c.drawText(String.format(Locale.KOREA,"%.2f%%",hi),558,23,paint(MUTED,13));c.drawText(String.format(Locale.KOREA,"%.2f%%",lo),558,85,paint(MUTED,13));
        c.drawText(date((long)first,"MM/dd HH:mm"),8,107,paint(MUTED,13));c.drawText(date((long)last,"MM/dd HH:mm"),450,107,paint(MUTED,13));return b;
    }
    static String date(long t,String pattern){if(t<=0)return "—";return Instant.ofEpochMilli(t).atZone(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern(pattern,Locale.KOREA));}
    static String number(double v){if(!Signals.finite(v))return "—";return new DecimalFormat("#,##0.########").format(v);}
    static String pct(double v){return Signals.finite(v)?String.format(Locale.KOREA,"%+.2f%%",v):"—";}
}
