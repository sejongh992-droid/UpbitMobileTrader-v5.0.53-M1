package kr.sejong.coinwidget;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.*;
import android.util.SizeF;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class WidgetInteractionTest {
 final Instrumentation ins=InstrumentationRegistry.getInstrumentation();final Context c=ins.getTargetContext();
 void seed()throws Exception{
  JSONObject root=new UpgradeTest().fixture();JSONObject m=root.getJSONObject("market");
  JSONArray d=new JSONArray();long day=24*Signals.HOUR,now=System.currentTimeMillis();for(int i=0;i<150;i++){double p=100000000+i*100000;d.put(new JSONObject().put("t",now-(150-i)*day).put("o",p).put("h",p+1000000).put("l",p-1000000).put("c",p+250000).put("v",1e10));}m.put("daily",d);
  for(String key:new String[]{"candidates","next_candidates","long_candidates"})for(int i=0;i<m.getJSONArray(key).length();i++)m.getJSONArray(key).getJSONObject(i).put("entry_low",12000).put("entry_high",12100).put("support",11700).put("resistance",12900).put("rsi",58).put("atr_pct",1.4).put("reason","같은 6시간 동안 BTC보다 1.2%p 강함 · 시간 거래대금 1.7배");
  c.getSharedPreferences("cache",0).edit().putString("snapshot",root.toString()).commit();c.getSharedPreferences("widget_ui",0).edit().clear().commit();
 }
 void awaitScreen(int id,int screen){long end=SystemClock.elapsedRealtime()+7000;while(Dashboard.screen(c,id)!=screen&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(30);assertEquals(screen,Dashboard.screen(c,id));}
 @Test public void rowAndToolbarStayInsideWidget()throws Exception{
  seed();int id=902;Instrumentation.ActivityMonitor monitor=ins.addMonitor(MainActivity.class.getName(),null,true);
  try{
   assertTrue(Dashboard.nav(c,id,Dashboard.HELP).isBroadcast());assertFalse(Dashboard.nav(c,id,Dashboard.HELP).isActivity());
   ins.runOnMainSync(()->{View view=Dashboard.build(c,340,760,id).apply(c,new FrameLayout(c));assertFalse(view.findViewById(R.id.panel).hasOnClickListeners());assertTrue(view.findViewById(R.id.title).hasOnClickListeners());assertTrue(view.findViewById(R.id.row_root).performClick());});
   awaitScreen(id,Dashboard.COIN);assertEquals("KRW-LPT",Dashboard.prefs(c).getString("coin"+id,""));
   for(int action:new int[]{Dashboard.BACK,Dashboard.MARKET,Dashboard.BTC,Dashboard.DOM,Dashboard.CHECK,Dashboard.HELP,Dashboard.SETTINGS}){Dashboard.nav(c,id,action).send();awaitScreen(id,action==Dashboard.BACK?0:action);}
   assertEquals("Widget click must never start MainActivity",0,monitor.getHits());
  }finally{ins.removeMonitor(monitor);}
 }
 @Test public void threeListsAreIsolatedAndFailClosed()throws Exception{
  seed();JSONObject root=Repository.load(c),m=root.getJSONObject("market");
  m.put("watchlist",new JSONArray().put(new JSONObject().put("qualified",false)));assertEquals(0,Dashboard.list(m,0).length());assertEquals(20,Dashboard.list(m,1).length());assertEquals(20,Dashboard.list(m,2).length());
  m.put("defensive",true);assertEquals(20,Dashboard.list(m,1).length());assertEquals(20,Dashboard.list(m,2).length());m.put("long_defensive",true);assertEquals(20,Dashboard.list(m,2).length());
  long now=System.currentTimeMillis();m.put("quote_at",now-300001);assertFalse(Dashboard.freshRecommendation(root,0,now));m.put("quote_at",now).put("recheck_at",now);assertFalse(Dashboard.freshRecommendation(root,1,now));assertTrue(Dashboard.freshRecommendation(root,0,now));m.put("schema",3);assertFalse(Dashboard.freshRecommendation(root,0,now));
 }
 @Test public void expiredDetailNeverShowsOldBuyBand()throws Exception{
  seed();Dashboard.move(c,903,Dashboard.COIN,"KRW-LPT");JSONObject root=Repository.load(c);root.getJSONObject("market").put("quote_at",System.currentTimeMillis()-600000);
  DetailContent.Result d=DetailContent.forScreen(c,903,Dashboard.COIN,root,0,System.currentTimeMillis());assertTrue(d.text.contains("이전 분석"));assertFalse(d.text.contains("12,000"));
 }
 @Test public void portraitUsesMaximumHeightAndSizeMapInflates()throws Exception{
  seed();Bundle opts=new Bundle();opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,380);opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,760);opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,340);opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,600);
  Configuration conf=new Configuration(c.getResources().getConfiguration());conf.orientation=Configuration.ORIENTATION_PORTRAIT;Context portrait=c.createConfigurationContext(conf);
  ins.runOnMainSync(()->{View v=MarketWidget.layouts(portrait,904,opts).apply(portrait,new FrameLayout(portrait));assertEquals(Dashboard.rows(portrait,760),((LinearLayout)v.findViewById(R.id.coin_rows)).getChildCount());});
  if(Build.VERSION.SDK_INT>=31){opts.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,new ArrayList<>(Arrays.asList(new SizeF(280,480),new SizeF(340,760),new SizeF(600,600),new SizeF(680,380))));ins.runOnMainSync(()->{View v=MarketWidget.layouts(c,904,opts).apply(c,new FrameLayout(c));assertNotNull(v.findViewById(R.id.tab_long));});}
 }
 @Test public void everyScreenFitsAndAllActionsAreLarge()throws Exception{
  seed();ins.runOnMainSync(()->{
   for(float font:new float[]{1f,1.3f}){Configuration config=new Configuration(c.getResources().getConfiguration());config.fontScale=font;Context cc=c.createConfigurationContext(config);float den=cc.getResources().getDisplayMetrics().density;
    for(int height:new int[]{380,480,600,760})for(int action:new int[]{0,1,Dashboard.LONG,Dashboard.MARKET,Dashboard.COIN,Dashboard.HELP,Dashboard.CHECK,Dashboard.SETTINGS,Dashboard.BTC,Dashboard.DOM}){
     Dashboard.move(cc,905,action,"KRW-LPT");View v=Dashboard.build(cc,280,height,905).apply(cc,new FrameLayout(cc));v.measure(View.MeasureSpec.makeMeasureSpec((int)(280*den),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec((int)(height*den),View.MeasureSpec.EXACTLY));v.layout(0,0,v.getMeasuredWidth(),v.getMeasuredHeight());
     for(int id:new int[]{R.id.title,R.id.tab_today,R.id.tab_morning,R.id.tab_long,R.id.page_prev,R.id.page_label,R.id.page_next,R.id.list_home,R.id.market_home,R.id.open_app,R.id.refresh}){View b=v.findViewById(id);if(action==Dashboard.SETTINGS&&(id==R.id.page_prev||id==R.id.page_label||id==R.id.page_next))continue;assertTrue("Touch height "+id,b.getHeight()>=48*den-1);assertTrue("Touch width "+id,b.getWidth()>=48*den-1);int bottom=b.getBottom();View parent=(View)b.getParent();while(parent!=v){bottom+=parent.getTop();parent=(View)parent.getParent();}assertTrue("Footer clipped h="+height+" action="+action+" font="+font,bottom<=v.getHeight()-v.getPaddingBottom()+1);}
     TextView detail=v.findViewById(R.id.detail_text);if(action==Dashboard.COIN||action==Dashboard.HELP||action==Dashboard.CHECK||action==Dashboard.SETTINGS){assertNotNull(detail.getLayout());assertTrue("Detail text clipped h="+height+" action="+action+" font="+font+" actualText="+detail.getLayout().getHeight()+" available="+detail.getHeight()+" text="+detail.getText(),detail.getLayout().getHeight()<=detail.getHeight()+1);}
     if(font==1f&&(height==380||height==760))save(v,"synthetic-v122-"+height+"-"+action+".png");
    }
   }
  });
 }
 static void save(View v,String name){try{Context c=v.getContext();Bitmap bitmap=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(bitmap));File dir=new File(c.getExternalFilesDir(null),"test-results");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}catch(Exception e){throw new AssertionError(e);}}
}
