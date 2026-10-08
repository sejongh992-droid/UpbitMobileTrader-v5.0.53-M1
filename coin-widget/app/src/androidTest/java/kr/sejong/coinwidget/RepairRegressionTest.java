package kr.sejong.coinwidget;
import android.app.*;
import android.content.*;
import android.widget.*;
import android.view.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class RepairRegressionTest {
 final Instrumentation ins=InstrumentationRegistry.getInstrumentation();final Context c=ins.getTargetContext();
 JSONObject seed()throws Exception{JSONObject r=new UpgradeTest().fixture(),m=r.getJSONObject("market");m.put("schema",5).put("pre_watchlist",m.getJSONArray("next_candidates")).put("long_watchlist",m.getJSONArray("long_candidates"));return r;}
 void store(JSONObject root){c.getSharedPreferences("cache",0).edit().putString("snapshot",root.toString()).commit();c.getSharedPreferences("widget_ui",0).edit().clear().commit();}
 @Test public void tenMinuteGapKeepsRowsButRemovesBuyPermission()throws Exception{
  JSONObject r=seed(),m=r.getJSONObject("market");long now=System.currentTimeMillis();m.put("quote_at",now-600000);store(r);
  assertFalse(Dashboard.freshRecommendation(r,0,now));assertEquals(20,Dashboard.list(m,0).length());
  ins.runOnMainSync(()->{View v=Dashboard.build(c,340,760,920).apply(c,new FrameLayout(c));assertNotNull(v.findViewById(R.id.row_name));assertTrue(((TextView)v.findViewById(R.id.row_status)).getText().toString().contains("이전 분석"));assertNull(v.findViewById(R.id.empty_label));});
 }
 @Test public void defensiveMarketRetainsAllThreeObservedLists()throws Exception{
  JSONObject r=seed(),m=r.getJSONObject("market");m.put("defensive",true).put("long_defensive",true);
  for(int mode=0;mode<3;mode++){JSONArray a=Dashboard.list(m,mode);assertEquals(20,a.length());assertFalse(Dashboard.qualified(m,a.getJSONObject(0),mode));assertEquals("관찰 · 조건 부족",Dashboard.label(r,a.getJSONObject(0),mode,System.currentTimeMillis()));}
 }
 @Test public void failureDoesNotEraseAnalyzedCandidates()throws Exception{
  JSONObject r=seed().put("market_failed",true),m=r.getJSONObject("market");assertFalse(Dashboard.freshRecommendation(r,0,System.currentTimeMillis()));assertEquals(20,Dashboard.list(m,0).length());
  Dashboard.move(c,921,Dashboard.COIN,"KRW-LPT");DetailContent.Result d=DetailContent.forScreen(c,921,Dashboard.COIN,r,0,System.currentTimeMillis());assertTrue(d.text.contains("이전 분석"));assertFalse(d.text.contains("눌림 검토"));
 }
 @Test public void observationExplainsFailureWithoutClaimingUptrend()throws Exception{
  JSONObject r=seed(),m=r.getJSONObject("market"),a=m.getJSONArray("long_watchlist").getJSONObject(0);a.put("qualified",false).put("risk","20·60·120일 상승 추세 미충족");Dashboard.move(c,922,Dashboard.COIN,"KRW-LPT");
  DetailContent.Result d=DetailContent.forScreen(c,922,Dashboard.COIN,r,2,System.currentTimeMillis());assertTrue(d.text.contains("상승 추세 미충족"));assertFalse(d.text.contains("눌림 검토"));assertTrue(d.text.contains("현재 매수 신호는 아닙니다"));
 }
 @Test public void repeatedFxDateStillRecordsNewCheck()throws Exception{
  long now=System.currentTimeMillis();String date=java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneOffset.UTC).toLocalDate().minusDays(1).toString();JSONObject raw=new JSONObject().put("rate",1343.46).put("date",date).put("base","USD").put("quote","KRW");
  JSONObject first=FxData.parse(raw,now-3600000),second=FxData.parse(raw,now),root=new JSONObject().put("fx",second);
  assertEquals(first.getString("date"),second.getString("date"));assertTrue(second.getLong("fetched_at")>first.getLong("fetched_at"));assertEquals("조회 정상",FxData.state(root,now));assertTrue(FxData.header(root,now).contains("확인 "+Charts.date(now,"HH:mm")));assertTrue(FxData.explanation(root,now).contains("새 발표 전"));
  root.put("fx_failed",true);assertEquals("조회 실패 · 이전 값",FxData.state(root,now));
  try{FxData.parse(raw.put("rate",0),now);fail("Invalid rate accepted");}catch(java.io.IOException expected){}
 }
 @Test public void toolbarLabelsAndActionsAreFixed()throws Exception{
  // Already-list screens need no queued BACK; otherwise it can arrive after direct test setup of the next screen.
  store(seed());for(int action:new int[]{0,1,Dashboard.LONG,Dashboard.MARKET,Dashboard.SETTINGS,Dashboard.COIN}){
   Dashboard.move(c,923,action,"KRW-LPT");ins.runOnMainSync(()->{View v=Dashboard.build(c,340,760,923).apply(c,new FrameLayout(c));assertEquals("목록",((TextView)v.findViewById(R.id.list_home)).getText().toString());assertEquals("시장",((TextView)v.findViewById(R.id.market_home)).getText().toString());assertEquals("설정",((TextView)v.findViewById(R.id.open_app)).getText().toString());assertFalse(v.findViewById(R.id.page_label).hasOnClickListeners());if(action==Dashboard.SETTINGS){assertEquals(View.GONE,v.findViewById(R.id.page_controls).getVisibility());assertEquals(1,Dashboard.prefs(c).getInt("pages923",0));}assertTrue(v.findViewById(R.id.list_home).hasOnClickListeners());if(action==Dashboard.MARKET||action==Dashboard.SETTINGS||action==Dashboard.COIN)assertTrue(v.findViewById(R.id.list_home).performClick());});
   long end=android.os.SystemClock.elapsedRealtime()+3000;while(Dashboard.screen(c,923)!=0&&android.os.SystemClock.elapsedRealtime()<end)android.os.SystemClock.sleep(20);assertEquals(0,Dashboard.screen(c,923));
  }
 }
}
