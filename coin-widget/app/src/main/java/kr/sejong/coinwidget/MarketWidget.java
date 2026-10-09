package kr.sejong.coinwidget;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.util.SizeF;
import android.widget.RemoteViews;
import java.util.*;
public final class MarketWidget extends AppWidgetProvider {
 @Override public void onReceive(Context c,Intent i){
  if(i!=null&&Dashboard.NAV.equals(i.getAction())){
   int id=i.getIntExtra("widget",0),a=i.getIntExtra("nav",-1);
   if((a>=0&&a<=12)||a==Dashboard.COIN){Dashboard.move(c,id,a,i.getStringExtra("market")==null?"":i.getStringExtra("market"));renderAll(c);}return;
  }
  super.onReceive(c,i);
 }
 @Override public void onEnabled(Context c){Scheduler.ensure(c);Scheduler.request(c);}
 @Override public void onDisabled(Context c){Scheduler.cancel(c);}
 @Override public void onUpdate(Context c,AppWidgetManager m,int[]ids){Scheduler.ensure(c);renderAll(c);}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){renderAll(c);}
 static int[] ids(Context c){return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,MarketWidget.class));}
 static RemoteViews layouts(Context c,int id,Bundle o){
  if(Build.VERSION.SDK_INT>=31){
   ArrayList<SizeF>sizes=o.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
   if(sizes!=null&&!sizes.isEmpty()){
    Map<SizeF,RemoteViews>variants=new LinkedHashMap<>();
    for(SizeF size:sizes)if(size!=null&&size.getWidth()>0&&size.getHeight()>0&&variants.size()<4)variants.put(size,Dashboard.build(c,Math.round(size.getWidth()),Math.round(size.getHeight()),id));
    if(!variants.isEmpty())return new RemoteViews(variants);
   }
  }
  int minW=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,300),minH=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,600);
  int maxW=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,minW),maxH=o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,minH);
  // min height is normally landscape; portrait must use max height.
  return new RemoteViews(Dashboard.build(c,Math.max(minW,maxW),minH,id),Dashboard.build(c,minW,Math.max(minH,maxH),id));
 }
 static void renderAll(Context c){AppWidgetManager manager=AppWidgetManager.getInstance(c);for(int id:ids(c))try{manager.updateAppWidget(id,layouts(c,id,manager.getAppWidgetOptions(id)));}catch(RuntimeException e){android.util.Log.w("CoinWidget","Widget render failed: "+e.getClass().getSimpleName());}}
}
