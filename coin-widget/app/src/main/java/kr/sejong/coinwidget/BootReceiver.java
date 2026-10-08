package kr.sejong.coinwidget;
import android.content.*;
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){Scheduler.ensure(c);MarketWidget.renderAll(c);}
}
