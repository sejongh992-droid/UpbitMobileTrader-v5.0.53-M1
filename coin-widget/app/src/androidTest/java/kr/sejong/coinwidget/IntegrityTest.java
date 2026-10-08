package kr.sejong.coinwidget;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.widget.FrameLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class IntegrityTest {
 Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
 JSONObject ticker()throws Exception{return new JSONObject().put("market","KRW-TEST").put("trade_price",0.12345).put("prev_closing_price",0.12).put("signed_change_rate",0.02875).put("acc_trade_price_24h",1e10).put("trade_timestamp",System.currentTimeMillis());}
 @Test public void exactDecimalAndSignedPercent()throws Exception{
  Signals.Quote q=MarketData.parseTicker(ticker(),new JSONObject().put("korean_name","시험"));assertEquals(.12345,q.price,0);assertEquals(2.875,q.dayPct,1e-9);assertEquals("0.12345",Charts.number(q.price));
  JSONObject down=ticker().put("trade_price",.108).put("signed_change_rate",-.1);assertEquals(-10,MarketData.parseTicker(down,new JSONObject()).dayPct,1e-9);
 }
 @Test public void mismatchedAndInvalidQuotesRejected()throws Exception{
  for(JSONObject t:Arrays.asList(ticker().put("signed_change_rate",.2),ticker().put("prev_closing_price",0),ticker().put("acc_trade_price_24h",-1),ticker().put("trade_timestamp",0))){try{MarketData.parseTicker(t,new JSONObject());fail("Invalid quote accepted");}catch(java.io.IOException expected){}}
 }
 @Test public void missingAndDuplicateTickersRejected()throws Exception{
  Signals.Quote q=MarketData.parseTicker(ticker(),new JSONObject());
  try{MarketData.ensureCoverage(Arrays.asList(q),Arrays.asList("KRW-TEST","KRW-BTC"));fail("partial universe accepted");}catch(java.io.IOException expected){}
  try{MarketData.ensureCoverage(Arrays.asList(q,q),Arrays.asList("KRW-TEST","KRW-BTC"));fail("duplicate universe accepted");}catch(java.io.IOException expected){}
  MarketData.ensureCoverage(Arrays.asList(q),Arrays.asList("KRW-TEST"));
 }
 @Test public void marketCautionExcluded()throws Exception{
  JSONObject meta=new JSONObject().put("market_event",new JSONObject().put("caution",new JSONObject().put("PRICE_FLUCTUATIONS",true)));
  Signals.Quote q=MarketData.parseTicker(ticker(),meta);assertTrue(q.warning);assertFalse(Research.pool(q,System.currentTimeMillis()));
 }
 @Test public void completionTimeDoesNotReviveOldQuotes()throws Exception{
  JSONObject root=new JSONObject().put("market",new JSONObject().put("fetched_at",System.currentTimeMillis()).put("quote_at",System.currentTimeMillis()-4*Signals.HOUR));assertFalse(Renderer.freshMarket(root));
 }
 @Test public void enlargedFontsKeepActionsWithinWidget()throws Exception{
  c.getSharedPreferences("cache",0).edit().putString("snapshot",new UpgradeTest().fixture().toString()).commit();
  Configuration conf=new Configuration(c.getResources().getConfiguration());conf.fontScale=1.3f;Context enlarged=c.createConfigurationContext(conf);
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
   float d=enlarged.getResources().getDisplayMetrics().density;
   for(int h:new int[]{380,480,600,760}){
    View v=Dashboard.build(enlarged,h,21).apply(enlarged,new FrameLayout(enlarged));v.measure(View.MeasureSpec.makeMeasureSpec((int)(280*d),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec((int)(h*d),View.MeasureSpec.EXACTLY));v.layout(0,0,v.getMeasuredWidth(),v.getMeasuredHeight());
    View button=v.findViewById(R.id.refresh);int bottom=button.getBottom();View p=(View)button.getParent();while(p!=v){bottom+=p.getTop();p=(View)p.getParent();}
    assertTrue("Enlarged font clips actions at "+h+": "+bottom/d,bottom<=h*d-v.getPaddingBottom());assertTrue(button.getHeight()>=36*d);
   }
  });
 }
 @Test public void plainLanguageGuideOpens()throws Exception{
  c.getSharedPreferences("cache",0).edit().putString("snapshot",new UpgradeTest().fixture().toString()).commit();Repository.RUNNING.set(true);
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   scenario.onActivity(a->{AlertDialog dialog=HelpUi.show(a);assertTrue(dialog.isShowing());assertTrue(HelpUi.GUIDE.contains("83% 상승 확률이 아닙니다"));dialog.dismiss();});
  }finally{Repository.RUNNING.set(false);}
 }
}
