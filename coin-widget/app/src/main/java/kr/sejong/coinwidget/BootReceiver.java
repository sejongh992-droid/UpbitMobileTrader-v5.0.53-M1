package kr.sejong.coinwidget;
import android.content.*;
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        if(i==null)return;
        String action=i.getAction();
        if(!Intent.ACTION_BOOT_COMPLETED.equals(action)&&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))return;
        Scheduler.ensure(c);MarketWidget.renderAll(c);if(Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)&&MarketWidget.ids(c).length>0)Scheduler.request(c);
    }
}
