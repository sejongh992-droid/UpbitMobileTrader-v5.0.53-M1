package kr.sejong.coinwidget;
import android.app.Instrumentation;
import android.app.job.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class WidgetHostTest {
 private final Instrumentation ins=InstrumentationRegistry.getInstrumentation();
 private final Context c=ins.getTargetContext();
 private void fixture()throws Exception{
  Scheduler.cancel(c);c.getSharedPreferences("settings",0).edit().putBoolean("auto",false).commit();
  long now=System.currentTimeMillis();List<Signals.Bar> bars=new ArrayList<>();
  for(int i=0;i<90;i++){double p=100000000+i*100000;bars.add(new Signals.Bar(now-(90-i)*24*Signals.HOUR,p,p+1000000,p-1000000,p+200000,100));}
  JSONObject market=new JSONObject().put("fetched_at",now).put("btc",new JSONObject().put("price",109000000).put("day_pct",1.2)).put("daily",Repository.barsJson(bars)).put("btc_regime","테스트 자료").put("breadth",50);
  JSONArray hist=new JSONArray().put(new JSONArray().put(now-2*Signals.HOUR).put(57.1)).put(new JSONArray().put(now).put(56.9));
  JSONObject dom=new JSONObject().put("value",56.9).put("time",now).put("source","TEST DATA").put("history",hist);
  JSONObject root=new JSONObject().put("market",market).put("dominance",dom);
  c.getSharedPreferences("cache",0).edit().putString("snapshot",root.toString()).commit();
 }
 private void screenshot(String name)throws Exception{Bitmap b=ins.getUiAutomation().takeScreenshot();assertNotNull(b);File dir=new File(c.getExternalFilesDir(null),"test-results");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,name))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}}
 @Test public void frameworkHostResizeAndTwoHourJob()throws Exception{
  fixture();Repository.RUNNING.set(true);ins.getUiAutomation().adoptShellPermissionIdentity();
  AppWidgetHost host=new AppWidgetHost(c,7123);int id=host.allocateAppWidgetId();AppWidgetManager m=AppWidgetManager.getInstance(c);
  ActivityScenario<MainActivity> scenario=null;
  try{
   Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,300);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,560);
   assertTrue("Widget binding failed",m.bindAppWidgetIdIfAllowed(id,new ComponentName(c,MarketWidget.class),options));
   AtomicReference<AppWidgetHostView> view=new AtomicReference<>();scenario=ActivityScenario.launch(MainActivity.class);
   scenario.onActivity(a->{host.startListening();LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(a.dp(8),a.dp(40),a.dp(8),0);root.addView(a.text("TEST DATA · Android widget host",12,true));AppWidgetHostView v=host.createView(a,id,m.getAppWidgetInfo(id));view.set(v);root.addView(v,new LinearLayout.LayoutParams(-1,a.dp(560)));a.setContentView(root);});
   SystemClock.sleep(1200);Repository.RUNNING.set(false);
   c.getSharedPreferences("settings",0).edit().putBoolean("auto",true).commit();Scheduler.ensure(c);MarketWidget.renderAll(c);ins.waitForIdleSync();SystemClock.sleep(700);
   JobInfo job=c.getSystemService(JobScheduler.class).getPendingJob(Scheduler.PERIODIC);assertNotNull(job);assertEquals(7_200_000L,job.getIntervalMillis());assertTrue(job.isPersisted());
   scenario.onActivity(a->{assertNotNull("Framework widget failed to inflate",view.get().findViewById(R.id.refresh));assertEquals(View.VISIBLE,view.get().findViewById(R.id.dom_chart).getVisibility());});screenshot("synthetic-widget-full.png");
   options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,340);m.updateAppWidgetOptions(id,options);MarketWidget.renderAll(c);ins.waitForIdleSync();SystemClock.sleep(700);
   scenario.onActivity(a->{assertEquals(View.GONE,view.get().findViewById(R.id.dom_chart).getVisibility());assertNotNull(view.get().findViewById(R.id.refresh));});
   c.getSharedPreferences("settings",0).edit().putBoolean("auto",false).commit();Scheduler.ensure(c);assertNull(c.getSystemService(JobScheduler.class).getPendingJob(Scheduler.PERIODIC));
  }finally{Repository.RUNNING.set(false);host.stopListening();host.deleteHost();Scheduler.cancel(c);if(scenario!=null)scenario.close();ins.getUiAutomation().dropShellPermissionIdentity();}
 }
 @Test public void activityAndSettingsOpen()throws Exception{
  fixture();try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   scenario.onActivity(a->{assertNotNull(a.findViewById(R.id.refresh));SettingsUi.show(a);});ins.waitForIdleSync();SystemClock.sleep(400);
   assertNotNull(ins.getUiAutomation().getRootInActiveWindow());
   assertFalse(ins.getUiAutomation().getRootInActiveWindow().findAccessibilityNodeInfosByText("2시간 주기 자동 갱신").isEmpty());
   ins.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
  }
 }
}
