package kr.sejong.coinwidget;

import android.appwidget.*;
import android.content.*;
import android.os.Bundle;

public final class MarketWidget extends AppWidgetProvider {
    @Override public void onEnabled(Context c){Scheduler.ensure(c);Scheduler.request(c);}
    @Override public void onDisabled(Context c){Scheduler.cancel(c);}
    @Override public void onUpdate(Context c,AppWidgetManager m,int[]ids){Scheduler.ensure(c);renderAll(c);}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){renderAll(c);}
    static int[] ids(Context c){return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,MarketWidget.class));}
    static void renderAll(Context c){
        AppWidgetManager m=AppWidgetManager.getInstance(c);
        for(int id:ids(c)){
            try{int h=m.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,520);m.updateAppWidget(id,Renderer.build(c,h<480));}
            catch(RuntimeException e){android.util.Log.w("CoinWidget","Widget render failed: "+e.getClass().getSimpleName());}
        }
    }
}
