package kr.sejong.coinwidget;
import android.content.*;
import android.graphics.Bitmap;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.util.*;
import java.io.IOException;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class StorageRenderTest {
 private Context c;
 @Before public void setup(){c=InstrumentationRegistry.getInstrumentation().getTargetContext();Scheduler.cancel(c);c.getSharedPreferences("settings",0).edit().putBoolean("auto",false).commit();c.getSharedPreferences("cache",0).edit().clear().commit();}
 @After public void cleanup(){Repository.RUNNING.set(false);SecureStore.clear(c);Scheduler.cancel(c);}
 @Test public void coldAndCorruptCache(){assertEquals(0,Repository.load(c).length());c.getSharedPreferences("cache",0).edit().putString("snapshot","not-json").commit();assertEquals(0,Repository.load(c).length());}
 @Test public void candleRoundTripAndUtc()throws Exception{
  List<Signals.Bar>b=Arrays.asList(new Signals.Bar(1700000000000L,100,110,90,102,500));
  List<Signals.Bar>back=Repository.readBars(Repository.barsJson(b));assertEquals(1,back.size());assertEquals(102,back.get(0).close,0);
  String raw="[{\"candle_date_time_utc\":\"2024-01-01T00:00:00\",\"opening_price\":100,\"high_price\":110,\"low_price\":90,\"trade_price\":102,\"candle_acc_trade_price\":500}]";
  assertEquals(1704067200000L,Repository.readBars(new JSONArray(raw)).get(0).time);
 }
 @Test public void staleAndFailedMarketSuppressed()throws Exception{
  JSONObject m=new JSONObject().put("schema",3).put("fetched_at",System.currentTimeMillis());JSONObject root=new JSONObject().put("market",m);
  assertTrue(Renderer.freshMarket(root));root.put("market_failed",true);assertFalse(Renderer.freshMarket(root));root.put("market_failed",false);
  m.put("fetched_at",System.currentTimeMillis()-4*Signals.HOUR);assertFalse(Renderer.freshMarket(root));
 }
 @Test public void bitmapFallbacks()throws Exception{
  Bitmap b=Charts.btc(new JSONArray());assertEquals(640,b.getWidth());assertEquals(180,b.getHeight());
  assertEquals(112,Charts.dominance(new JSONArray()).getHeight());assertNotNull(Charts.btc(new JSONArray("[{}]")));
 }
 @Test public void optionalKeyEncryptedRoundTrip()throws Exception{
  String key="CG_TEST_ONLY_NOT_A_REAL_KEY_123";SecureStore.save(c,key);assertTrue(SecureStore.has(c));assertEquals(key,SecureStore.read(c));
  assertFalse(c.getSharedPreferences("secure",0).getAll().toString().contains(key));SecureStore.clear(c);assertFalse(SecureStore.has(c));assertEquals("",SecureStore.read(c));
 }
 @Test public void invalidKeyAndWrongHostRejected()throws Exception{
  try{SecureStore.save(c,"bad key !");fail("invalid key accepted");}catch(IllegalArgumentException expected){}
  try{new Net().get("http://api.upbit.com/v1/market/all");fail("HTTP accepted");}catch(IOException expected){}
  try{new Net().get("https://example.com");fail("unknown host accepted");}catch(IOException expected){}
  try{new Net().get("https://api.upbit.com/v1/market/all","DUMMY_TEST");fail("key sent to wrong host");}catch(IOException expected){}
 }
 @Test public void fullAndCompactRemoteViewsInflate(){
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
   FrameLayout parent=new FrameLayout(c);Dashboard.move(c,0,Dashboard.MARKET);View full=Renderer.build(c,false).apply(c,parent);assertNotNull(full.findViewById(R.id.refresh));assertEquals(View.VISIBLE,full.findViewById(R.id.dom_group).getVisibility());
   View compact=Renderer.build(c,true).apply(c,parent);assertEquals(View.GONE,compact.findViewById(R.id.dom_group).getVisibility());assertNotNull(compact.findViewById(R.id.btc_chart));
  });
 }
 @Test public void noConcurrentRefresh()throws Exception{Repository.RUNNING.set(true);assertFalse(Repository.refresh(c));}
 @Test public void errorRecordingInvalidatesOldCandidates()throws Exception{
  c.getSharedPreferences("cache",0).edit().putString("snapshot",new JSONObject().put("market",new JSONObject().put("schema",3).put("fetched_at",System.currentTimeMillis())).toString()).commit();
  assertTrue(Renderer.freshMarket(Repository.load(c)));Repository.recordFailure(c,"TEST_FAILURE");assertFalse(Renderer.freshMarket(Repository.load(c)));assertEquals("TEST_FAILURE",Repository.load(c).getJSONArray("errors").getString(0));
 }
 @Test public void immutableActivityIntent(){assertTrue(Renderer.activity(c,909,new Intent(c,MainActivity.class)).isImmutable());}
}
