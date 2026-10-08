package kr.sejong.coinwidget;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
/** Real public HTTPS calls via the actual manual-refresh service. No credentials. */
@RunWith(AndroidJUnit4.class)
public class LiveRefreshTest {
 @Test public void manualRefreshCompletesWithRealPublicData()throws Exception{
  Instrumentation ins=InstrumentationRegistry.getInstrumentation();Context c=ins.getTargetContext();Scheduler.cancel(c);Repository.RUNNING.set(false);
  c.getSharedPreferences("settings",0).edit().putBoolean("auto",false).putString("dominance_source","CoinPaprika").commit();SecureStore.clear(c);
  JSONObject seed=new JSONObject().put("market",new JSONObject().put("schema",4).put("fetched_at",System.currentTimeMillis()));
  c.getSharedPreferences("cache",0).edit().putString("snapshot",seed.toString()).commit();
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   long start=System.currentTimeMillis();scenario.onActivity(a->{assertNotNull(a.findViewById(R.id.refresh));a.findViewById(R.id.refresh).performClick();a.refresh();a.refresh();});
   long end=SystemClock.elapsedRealtime()+165000;
   while(SystemClock.elapsedRealtime()<end){JSONObject r=Repository.load(c);if(r.optLong("last_finished")>=start&&!Repository.RUNNING.get())break;SystemClock.sleep(300);}
   JSONObject r=Repository.load(c);File dir=new File(c.getExternalFilesDir(null),"test-results");assertTrue(dir.isDirectory()||dir.mkdirs());
   try(FileOutputStream out=new FileOutputStream(new File(dir,"live-snapshot.json"))){out.write(r.toString(2).getBytes(StandardCharsets.UTF_8));}
   ins.waitForIdleSync();SystemClock.sleep(800);Bitmap image=ins.getUiAutomation().takeScreenshot();
   if(image!=null)try(FileOutputStream out=new FileOutputStream(new File(dir,"live-app.png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}
   assertTrue("Manual refresh did not finish: "+r.optJSONArray("errors"),r.optLong("last_finished")>=start);assertFalse(Repository.RUNNING.get());
   JSONObject market=r.getJSONObject("market");assertEquals(5,market.getInt("schema"));
   assertTrue(market.getInt("inspected")<=60);assertTrue(market.getInt("long_inspected")<=30);assertTrue(market.getJSONArray("long_candidates").length()<=15);
   for(String key:new String[]{"candidates","next_candidates","long_candidates"})for(int n=0;n<market.getJSONArray(key).length();n++){JSONObject x=market.getJSONArray(key).getJSONObject(n);assertTrue(x.optBoolean(key.equals("next_candidates")?"pre_qualified":"qualified"));assertTrue(x.getDouble("support")<x.getDouble("entry_low"));assertTrue(x.getDouble("entry_high")<x.getDouble("resistance"));}assertTrue(market.getJSONArray("watchlist").length()<=30);assertTrue(market.getJSONArray("next_candidates").length()<=15);
   for(int n=0;n<market.getJSONArray("watchlist").length();n++)assertFalse(market.getJSONArray("watchlist").getJSONObject(n).getJSONObject("quote").getString("name").isEmpty());
   assertTrue("Real hourly data must yield visible analyzed observations",market.getJSONArray("watchlist").length()>0);
   assertTrue("Pre-09 observations must be analyzed even during defensive markets",market.getJSONArray("pre_watchlist").length()>0);
   assertTrue("Long-term analysis must run even with no strict buy candidates",market.getJSONArray("long_watchlist").length()>0);
   for(int mode=0;mode<3;mode++)assertTrue("Visible list must not be blank: "+mode,Dashboard.list(market,mode).length()>0);
   assertTrue(r.getJSONObject("fx").getLong("fetched_at")>=start);
   for(int viewMode=0;viewMode<3;viewMode++){
    final int selected=viewMode;scenario.onActivity(a->{Dashboard.move(a,0,selected==2?Dashboard.LONG:selected);a.draw();assertNotNull("Visible coin row "+selected,a.findViewById(R.id.row_name));});ins.waitForIdleSync();SystemClock.sleep(250);
    Bitmap shot=ins.getUiAutomation().takeScreenshot();if(shot!=null)try(FileOutputStream out=new FileOutputStream(new File(dir,viewMode==0?"live-today.png":viewMode==1?"live-before09.png":"live-long.png"))){shot.compress(Bitmap.CompressFormat.PNG,100,out);}
   }
   assertFalse("Upbit failed: "+r.optJSONArray("errors"),r.optBoolean("market_failed",true));
   assertTrue(r.getJSONObject("market").getJSONObject("btc").getDouble("price")>0);assertTrue(r.getJSONObject("market").getJSONArray("daily").length()>=60);
   assertFalse("Dominance failed: "+r.optJSONArray("errors"),r.optBoolean("dom_failed",true));assertTrue(r.getJSONObject("dominance").getDouble("value")>0);
   assertFalse("FX failed: "+r.optJSONArray("errors"),r.optBoolean("fx_failed",true));assertTrue(r.getJSONObject("fx").getDouble("rate")>0);
  }
 }
}
