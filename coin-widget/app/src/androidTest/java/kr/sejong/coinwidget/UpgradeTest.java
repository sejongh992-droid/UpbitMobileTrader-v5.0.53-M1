package kr.sejong.coinwidget;
import android.content.*;
import android.view.*;
import android.widget.*;
import android.graphics.Bitmap;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class UpgradeTest {
 Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
 @Test public void nineBoundary(){
  long before=ZonedDateTime.of(2026,10,9,8,59,0,0,ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
  assertEquals(before+60000,Research.nextNine(before));assertEquals(before+60000+24*Signals.HOUR,Research.nextNine(before+60000));
  assertEquals(Research.nextNine(before),Research.nextNine(before-8*Signals.HOUR));
 }
 @Test public void expandedUniverseAndDefensiveGate(){
  long now=1700000000000L+75*Signals.HOUR;List<Signals.Bar>bars=new ArrayList<>(),btc=new ArrayList<>();
  for(int i=0;i<75;i++){double p=100+i*.12+2*Math.sin(i*.7+1);bars.add(new Signals.Bar(now-(75-i)*Signals.HOUR,p,p+.7,p-.7,p,i==74?200:100));btc.add(new Signals.Bar(now-(75-i)*Signals.HOUR,100,101,99,100,100));}
  Signals.Quote q=new Signals.Quote("KRW-TEST","테스트코인",bars.get(74).close,5,2e10,false,now);
  Research.S s=Research.evaluate(q,bars,btc,now,false);assertNotNull(s);assertTrue(s.qualified);assertTrue(s.entryHi<=q.price);assertTrue(s.score>=0&&s.score<=100);
  Research.S d=Research.evaluate(q,bars,btc,now,true);assertFalse(d.qualified);assertFalse(d.preQualified);assertEquals("선매수 보류",d.preState);
  assertNull(Research.evaluate(q,bars,btc,now+Signals.HOUR,true));
  Signals.Quote small=new Signals.Quote("KRW-TEST","테스트코인",q.price,5,2e9,false,now);assertTrue(Research.pool(small,now));assertFalse(Signals.eligible(small,now));
  assertEquals(60,Research.SCAN_LIMIT);assertEquals(30,Research.DISPLAY_LIMIT);assertEquals(15,Research.MORNING_LIMIT);
 }
 JSONObject fixture()throws Exception{
  long now=System.currentTimeMillis();JSONArray list=new JSONArray();for(int i=0;i<20;i++)list.put(new JSONObject().put("quote",new JSONObject().put("name","라이브피어").put("market","KRW-LPT").put("price",12340).put("day_pct",2.1).put("time",now)).put("score",83-i).put("morning_score",80-i).put("qualified",true).put("pre_qualified",true).put("pulse_ratio",1.8).put("pulse_change",.8).put("relative6h",1.2).put("volume_ratio",1.7).put("return30",15).put("state","상승 조건 충족").put("pre_state","09시 전 재확인"));
  JSONObject m=new JSONObject().put("schema",4).put("defensive",false).put("long_defensive",false).put("quote_at",now).put("fetched_at",now).put("btc",new JSONObject().put("price",112400000).put("day_pct",-1.01)).put("btc_regime","조정·혼조").put("breadth",34.3).put("candidates",list).put("long_candidates",list).put("watchlist",list).put("next_candidates",list).put("recheck_at",Research.nextNine(now));return new JSONObject().put("market",m).put("fx",new JSONObject().put("rate",1343.46).put("date","2026-10-08")).put("dominance",new JSONObject().put("value",56.75).put("time",now).put("source","TEST DATA"));
 }
 @Test public void namesPagingAndExpiredMorning()throws Exception{
  c.getSharedPreferences("cache",0).edit().putString("snapshot",fixture().toString()).commit();c.getSharedPreferences("widget_ui",0).edit().clear().commit();
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
   View v=Dashboard.build(c,760,999).apply(c,new FrameLayout(c));assertEquals(Dashboard.rows(c,760),((LinearLayout)v.findViewById(R.id.coin_rows)).getChildCount());assertTrue(((TextView)v.findViewById(R.id.row_name)).getText().toString().contains("라이브피어 (LPT)"));
   Dashboard.move(c,999,3);View next=Dashboard.build(c,760,999).apply(c,new FrameLayout(c));assertTrue(((TextView)next.findViewById(R.id.row_name)).getText().toString().startsWith((Dashboard.rows(c,760)+1)+" "));assertEquals(0,Dashboard.mode(c,998));
   Dashboard.move(c,999,1);assertEquals(1,Dashboard.mode(c,999));assertEquals(0,c.getSharedPreferences("widget_ui",0).getInt("page999",-1));
  });
  JSONObject r=fixture();r.getJSONObject("market").put("recheck_at",System.currentTimeMillis()-1);c.getSharedPreferences("cache",0).edit().putString("snapshot",r.toString()).commit();
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{View v=Dashboard.build(c,760,999).apply(c,new FrameLayout(c));assertNotNull(v.findViewById(R.id.empty_label));assertEquals(1,((LinearLayout)v.findViewById(R.id.coin_rows)).getChildCount());});
 }
 @Test public void navigationBroadcastAndDetailTabHandoff()throws Exception{
  c.getSharedPreferences("cache",0).edit().putString("snapshot",fixture().toString()).commit();
  Dashboard.move(c,818,0);Dashboard.nav(c,818,1).send();
  long navStart=SystemClock.elapsedRealtime();while(Dashboard.mode(c,818)!=1&&SystemClock.elapsedRealtime()-navStart<8000)SystemClock.sleep(50);
  InstrumentationRegistry.getInstrumentation().waitForIdleSync();android.util.Log.i("WidgetNav","deliveryMs="+(SystemClock.elapsedRealtime()-navStart));
  assertEquals(1,Dashboard.mode(c,818));
  try(androidx.test.core.app.ActivityScenario<MainActivity> scenario=androidx.test.core.app.ActivityScenario.launch(new Intent(c,MainActivity.class).putExtra("view_mode",1))){
   scenario.onActivity(a->{assertEquals(1,Dashboard.mode(a,0));assertNotNull(a.findViewById(R.id.tab_morning));});
  }
 }
 @Test public void measuredWidgetContentFits()throws Exception{
  c.getSharedPreferences("cache",0).edit().putString("snapshot",fixture().toString()).commit();
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
   float den=c.getResources().getDisplayMetrics().density;StringBuilder boundsErrors=new StringBuilder();
   for(int h:new int[]{380,480,600,760}){
    View v=Dashboard.build(c,h,10).apply(c,new FrameLayout(c));v.measure(View.MeasureSpec.makeMeasureSpec((int)(340*den),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec((int)(h*den),View.MeasureSpec.EXACTLY));v.layout(0,0,v.getMeasuredWidth(),v.getMeasuredHeight());
    View button=v.findViewById(R.id.refresh);int bottom=button.getBottom();View parent=(View)button.getParent();while(parent!=v){bottom+=parent.getTop();parent=(View)parent.getParent();}
    try{Bitmap bm=Bitmap.createBitmap(v.getMeasuredWidth(),v.getMeasuredHeight(),Bitmap.Config.ARGB_8888);v.draw(new android.graphics.Canvas(bm));java.io.File dir=new java.io.File(c.getExternalFilesDir(null),"test-results");dir.mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,"synthetic-layout-"+h+".png"))){bm.compress(Bitmap.CompressFormat.PNG,100,out);}}catch(Exception e){throw new AssertionError(e);}
    android.util.Log.i("WidgetBounds","height="+h+" footerBottom="+(bottom/den));if(bottom>h*den-v.getPaddingBottom() || button.getHeight()<36*den || ((View)button.getParent()).getHeight()<40*den)boundsErrors.append("height=").append(h).append(" bottom=").append(bottom/den).append(" buttonHeight=").append(button.getHeight()/den).append("; ");
   }
   assertTrue("Widget bounds: "+boundsErrors,boundsErrors.length()==0);
  });
 }
}
