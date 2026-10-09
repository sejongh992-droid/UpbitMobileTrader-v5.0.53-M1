package kr.sejong.coinwidget;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.IOException;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class DataRecoveryTest {
 final Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
 @Before public void setup(){Scheduler.cancel(c);Repository.RUNNING.set(false);c.getSharedPreferences("settings",0).edit().putBoolean("auto",false).commit();}
 @After public void cleanup(){Repository.RUNNING.set(false);Scheduler.cancel(c);}
 JSONObject seed()throws Exception{return new RepairRegressionTest().seed();}
 void save(JSONObject r){c.getSharedPreferences("cache",0).edit().putString("snapshot",r.toString()).commit();}
 @Test public void fxIsCommittedBeforeMarketAndSurvivesItsFailure()throws Exception{
  JSONObject old=seed();save(old);long checked=System.currentTimeMillis();
  boolean ok=Repository.refresh(c,new Repository.Sources(){
   public JSONObject fx()throws Exception{return new JSONObject().put("rate",1400).put("date","2026-10-08").put("fetched_at",checked);}
   public JSONObject market()throws Exception{assertEquals(checked,Repository.load(c).getJSONObject("fx").getLong("fetched_at"));throw new IOException("injected market timeout");}
   public JSONObject dominance(JSONObject previous){return previous;}
  });
  JSONObject r=Repository.load(c);assertFalse(ok);assertTrue(r.getBoolean("market_failed"));assertFalse(r.getBoolean("fx_failed"));assertEquals(checked,r.getJSONObject("fx").getLong("fetched_at"));assertEquals(old.getJSONObject("market").toString(),r.getJSONObject("market").toString());assertFalse(Repository.RUNNING.get());
 }
 @Test public void failedFxDoesNotPreventNewMarketSnapshot()throws Exception{
  JSONObject old=seed();save(old);JSONObject market=new JSONObject(old.getJSONObject("market").toString()).put("quote_count",321);
  assertTrue(Repository.refresh(c,new Repository.Sources(){
   public JSONObject fx()throws Exception{throw new IOException("injected FX failure");}
   public JSONObject market(){return market;}
   public JSONObject dominance(JSONObject previous){return previous;}
  }));
  JSONObject r=Repository.load(c);assertTrue(r.getBoolean("fx_failed"));assertFalse(r.getBoolean("market_failed"));assertEquals(321,r.getJSONObject("market").getInt("quote_count"));assertEquals(old.getJSONObject("fx").toString(),r.getJSONObject("fx").toString());
 }
 @Test public void failedLaneRetainsOriginalTimeAndCannotQualify()throws Exception{
  long now=System.currentTimeMillis();JSONObject previous=seed().getJSONObject("market");previous.put("quote_at",now-1000).put("recheck_at",now+3600000);
  JSONObject next=new JSONObject(previous.toString()).put("quote_at",now).put("recheck_at",now+86400000);
  for(int mode=0;mode<3;mode++)next.put(SnapshotState.lane(mode)+"_status","error").put(SnapshotState.lane(mode)+"_at",now).put(SnapshotState.watch(mode),new JSONArray());
  JSONObject merged=SnapshotState.merge(previous,next),r=new JSONObject().put("market",merged);
  for(int mode=0;mode<3;mode++){
   assertEquals(20,Dashboard.list(merged,mode).length());assertEquals(now-1000,SnapshotState.time(merged,mode));assertTrue(SnapshotState.failed(merged,mode));assertEquals(0,merged.getJSONArray(SnapshotState.strict(mode)).length());assertFalse(Dashboard.freshRecommendation(r,mode,now));assertFalse(Dashboard.qualified(merged,Dashboard.list(merged,mode).getJSONObject(0),mode));
  }
  assertEquals(now+3600000,SnapshotState.nine(merged));
 }
 @Test public void validEmptyAndPartialResultsNeverResurrectOldSignals()throws Exception{
  JSONObject previous=seed().getJSONObject("market"),next=new JSONObject(previous.toString());
  next.put("day_status","insufficient").put("watchlist",new JSONArray()).put("candidates",new JSONArray());
  JSONObject one=new JSONObject(previous.getJSONArray("long_watchlist").getJSONObject(0).toString());
  next.put("long_status","partial").put("long_watchlist",new JSONArray().put(one));
  JSONObject merged=SnapshotState.merge(previous,next);assertEquals(0,Dashboard.list(merged,0).length());assertFalse(merged.getBoolean("day_retained"));assertEquals(1,Dashboard.list(merged,2).length());assertFalse(merged.getBoolean("long_retained"));assertEquals("일부 종목 분석",SnapshotState.state(merged,2));
 }
 @Test public void cancellationKeepsCommittedFxAndReleasesLock()throws Exception{
  save(seed());java.util.concurrent.atomic.AtomicReference<Throwable> result=new java.util.concurrent.atomic.AtomicReference<>();long checked=System.currentTimeMillis();
  Thread worker=new Thread(()->{try{Repository.refresh(c,new Repository.Sources(){
   public JSONObject fx()throws Exception{return new JSONObject().put("rate",1400).put("date","2026-10-08").put("fetched_at",checked);}
   public JSONObject market()throws Exception{Thread.currentThread().interrupt();Repository.cancelCheck();return null;}
   public JSONObject dominance(JSONObject previous){throw new AssertionError("cancelled refresh continued");}
  });result.set(new AssertionError("cancellation was swallowed"));}catch(java.io.InterruptedIOException expected){}catch(Throwable e){result.set(e);}});
  worker.start();worker.join(5000);assertFalse(worker.isAlive());assertNull(result.get());assertFalse(Repository.RUNNING.get());assertEquals(checked,Repository.load(c).getJSONObject("fx").getLong("fetched_at"));
 }
}
