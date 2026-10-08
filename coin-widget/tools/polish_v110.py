"""Post-migration fixes and resource normalization; no lint suppression."""
from pathlib import Path
import re,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1];j=root/'app/src/main/java/kr/sejong/coinwidget';res=root/'app/src/main/res'
def patch(file,a,b):
 p=j/file;s=p.read_text();p.write_text(s.replace(a,b))
patch('MiniCharts.java','Typeface.create("sans-serif",0)','Typeface.create("sans-serif",Typeface.NORMAL)')
patch('MiniCharts.java','new Paint(3)','new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG)')
patch('WatchDetails.java','card.setOrientation(1)','card.setOrientation(LinearLayout.VERTICAL)')
patch('Charts.java','new DecimalFormat(v>=100?"#,##0":v>=1?"#,##0.00":"0.####")','new DecimalFormat("#,##0.########")')
patch('MainActivity.java','card.findViewById(R.id.open_app).setOnClickListener(v->SettingsUi.show(this));','card.findViewById(R.id.open_app).setOnClickListener(v->scroll.smoothScrollTo(0,card.getBottom()+dp(10)));')
patch('WatchDetails.java','if(mode==1)out.addView(a.text(', 'if(System.currentTimeMillis()-m.optLong("quote_at")>900000)out.addView(a.text("시세가 15분 이상 지났습니다. 매수 검토 전 새로고침하세요.",14,true));\n  if(mode==1)out.addView(a.text(')
for p in [j/'DetailUi.java',res/'layout/widget_market.xml',res/'drawable/panel.xml',res/'drawable/button_bg.xml']:
 if p.exists():p.unlink()
ET.register_namespace('android','http://schemas.android.com/apk/res/android')
ns='{http://schemas.android.com/apk/res/android}';strings=ET.Element('resources')
for p in sorted((res/'layout').glob('*.xml')):
 tree=ET.parse(p)
 for index,e in enumerate(tree.iter()):
  for key in ['text','contentDescription']:
   val=e.get(ns+key)
   if val and not val.startswith('@'):
    name=f'ui_{p.stem}_{index}_{key.lower()}';s=ET.SubElement(strings,'string',{'name':name,'formatted':'false'});s.text=val;e.set(ns+key,'@string/'+name)
 tree.write(p,encoding='unicode')
ET.ElementTree(strings).write(res/'values/ui_strings.xml',encoding='unicode')
print('Polished constants, precise prices, details navigation, and localized resources')
# Keep actions visible at smaller heights, including real two-line FX metadata.
patch('Dashboard.java','height<540?48:72','height<540?30:72')
patch('Dashboard.java','if(height<600)v.setViewVisibility(R.id.dom_meta,View.GONE);','if(height<600)v.setViewVisibility(R.id.dom_meta,View.GONE);\n  if(height<540)v.setViewVisibility(R.id.breadth,View.GONE);\n  if(compact){v.setViewVisibility(R.id.btc_sub,View.GONE);v.setTextViewTextSize(R.id.btc_price,android.util.TypedValue.COMPLEX_UNIT_SP,22);}')
patch('Dashboard.java','PendingIntent app=Renderer.activity(c,10,new Intent(c,MainActivity.class));','PendingIntent app=Renderer.activity(c,10+id*10+mode,new Intent(c,MainActivity.class).setData(Uri.parse("coinwidget://details/"+id+"/"+mode)).putExtra("view_mode",mode));')
patch('MainActivity.java','getSharedPreferences("cache",0).registerOnSharedPreferenceChangeListener(this);','if(getIntent().hasExtra("view_mode"))Dashboard.move(this,0,getIntent().getIntExtra("view_mode",0)==1?1:0);\n  getSharedPreferences("cache",0).registerOnSharedPreferenceChangeListener(this);')
patch('Dashboard.java','int page=c.getSharedPreferences("widget_ui",0).getInt("page"+id,0)%pages;','int savedPage=c.getSharedPreferences("widget_ui",0).getInt("page"+id,0);int page=Math.max(0,savedPage)%pages;\n  if(page!=savedPage)c.getSharedPreferences("widget_ui",0).edit().putInt("page"+id,page).apply();')
patch('WatchDetails.java','일봉 RSI14 "+Dashboard.f','일봉 20일선=금색 / 60일선=청록색\\n일봉 RSI14 "+Dashboard.f')
patch('WatchDetails.java','분석: 24시간 거래대금','전일 등락률은 업비트의 KST 09시 기준 전일 종가 대비 값입니다. 거래대금 24h와는 기간 기준이 다릅니다.\\n\\n분석: 24시간 거래대금')
p=res/'layout/coin_empty.xml';tree=ET.parse(p);tree.getroot().set(ns+'layout_height','40dp');label=next(e for e in tree.iter() if e.tag=='TextView');label.set(ns+'maxLines','2');label.set(ns+'ellipsize','end');tree.write(p,encoding='unicode')
p=root/'app/src/androidTest/java/kr/sejong/coinwidget/UpgradeTest.java';s=p.read_text();s=s.replace('return new JSONObject().put("market",m);','return new JSONObject().put("market",m).put("fx",new JSONObject().put("rate",1343.46).put("date","2026-10-08")).put("dominance",new JSONObject().put("value",56.75).put("time",now).put("source","TEST DATA"));');p.write_text(s)
print('Adjusted small widget bounds, tab handoff, paging and daily-change labels')
# Exercise real PendingIntent -> receiver routing and selected-tab handoff.
p=root/'app/src/androidTest/java/kr/sejong/coinwidget/UpgradeTest.java';s=p.read_text();anchor=' @Test public void measuredWidgetContentFits()throws Exception{';extra=''' @Test public void navigationBroadcastAndDetailTabHandoff()throws Exception{
  c.getSharedPreferences("cache",0).edit().putString("snapshot",fixture().toString()).commit();
  Dashboard.move(c,818,0);Dashboard.nav(c,818,1).send();
  InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(300);
  assertEquals(1,Dashboard.mode(c,818));
  try(androidx.test.core.app.ActivityScenario<MainActivity> scenario=androidx.test.core.app.ActivityScenario.launch(new Intent(c,MainActivity.class).putExtra("view_mode",1))){
   scenario.onActivity(a->{assertEquals(1,Dashboard.mode(a,0));assertNotNull(a.findViewById(R.id.tab_morning));});
  }
 }
''';s=s.replace(anchor,extra+anchor);p.write_text(s)
p=root/'app/src/androidTest/java/kr/sejong/coinwidget/LiveRefreshTest.java';s=p.read_text();anchor='   assertFalse("Upbit failed:';pos=s.index(anchor);extra='''   JSONObject market=r.getJSONObject("market");assertEquals(2,market.getInt("schema"));
   assertTrue(market.getInt("inspected")<=60);assertTrue(market.getJSONArray("watchlist").length()<=30);assertTrue(market.getJSONArray("next_candidates").length()<=15);
   for(int n=0;n<market.getJSONArray("watchlist").length();n++)assertFalse(market.getJSONArray("watchlist").getJSONObject(n).getJSONObject("quote").getString("name").isEmpty());
   for(int viewMode=0;viewMode<2;viewMode++){
    final int selected=viewMode;scenario.onActivity(a->{Dashboard.move(a,0,selected);a.draw();});ins.waitForIdleSync();SystemClock.sleep(250);
    Bitmap shot=ins.getUiAutomation().takeScreenshot();if(shot!=null)try(FileOutputStream out=new FileOutputStream(new File(dir,viewMode==0?"live-today.png":"live-before09.png"))){shot.compress(Bitmap.CompressFormat.PNG,100,out);}
   }
''';s=s[:pos]+extra+s[pos:];p.write_text(s)
