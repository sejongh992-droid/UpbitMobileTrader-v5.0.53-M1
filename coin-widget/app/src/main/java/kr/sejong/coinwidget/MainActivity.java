package kr.sejong.coinwidget;
import android.Manifest;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
public final class MainActivity extends Activity implements android.content.SharedPreferences.OnSharedPreferenceChangeListener {
 private LinearLayout content;private ScrollView scroll;
 private final Handler main=new Handler(Looper.getMainLooper());
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);scroll=new ScrollView(this);scroll.setBackgroundColor(Color.rgb(241,243,245));
  content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(14),dp(14),dp(14),dp(20));
  scroll.addView(content,new ScrollView.LayoutParams(-1,-2));setContentView(scroll);
  if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);scroll.setOnApplyWindowInsetsListener((view,insets)->{Insets p=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());view.setPadding(p.left,p.top,p.right,p.bottom);return insets;});}else scroll.setFitsSystemWindows(true);
  if(getIntent().hasExtra("view_mode"))Dashboard.move(this,0,getIntent().getIntExtra("view_mode",0)==1?1:0);
  getSharedPreferences("cache",0).registerOnSharedPreferenceChangeListener(this);getSharedPreferences("widget_ui",0).registerOnSharedPreferenceChangeListener(this);Scheduler.ensure(this);draw();
 }
 @Override protected void onResume(){
  super.onResume();if(content!=null)draw();
  JSONObject cached=Renderer.obj(Repository.load(this),"market");
  if(cached.optInt("schema")<6||System.currentTimeMillis()-cached.optLong("quote_at",cached.optLong("fetched_at"))>5*60_000L)
   main.postDelayed(()->{if(!isFinishing()&&!isDestroyed()&&!Repository.RUNNING.get())refresh();},350);
 }

 @Override public void onSharedPreferenceChanged(android.content.SharedPreferences p,String key){if("snapshot".equals(key)||(!key.startsWith("pages")&&key.endsWith("0")))main.post(()->{if(!isDestroyed())draw();});}
 @Override protected void onDestroy(){getSharedPreferences("cache",0).unregisterOnSharedPreferenceChangeListener(this);getSharedPreferences("widget_ui",0).unregisterOnSharedPreferenceChangeListener(this);main.removeCallbacksAndMessages(null);super.onDestroy();}
 TextView text(String s,int size,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(Charts.INK);v.setPadding(dp(4),dp(7),dp(4),dp(7));if(bold)v.setTypeface(null,android.graphics.Typeface.BOLD);return v;}
 Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(13);b.setOnClickListener(v->r.run());return b;}
 void draw(){
  int oldY=scroll.getScrollY();content.removeAllViews();content.addView(text("코인 시장 위젯",23,true));
  content.addView(text("조회 전용 · 자동매매 프로그램과 별개 · v1.2.2",12,false));
  LinearLayout row=new LinearLayout(this);
  row.addView(button("홈 화면에 추가",this::pin),new LinearLayout.LayoutParams(0,dp(50),1));
  row.addView(button("API / 갱신 설정",()->SettingsUi.show(this)),new LinearLayout.LayoutParams(0,dp(50),1));content.addView(row);
  try{
   int width=(int)(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density)-28;
   View card=Dashboard.build(this,width,720,0).apply(this,content);
   card.findViewById(R.id.refresh).setOnClickListener(v->refresh());
   LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(720));p.topMargin=dp(10);content.addView(card,p);
  }catch(RuntimeException e){content.addView(text("위젯 미리보기를 표시하지 못했습니다. 새로고침 후 다시 확인하세요.",14,false));}
  content.addView(button("화면 읽는 법 · 쉬운 설명",()->HelpUi.show(this)));
  WatchDetails.add(this,content,Repository.load(this));
  content.addView(button("앱·배터리 설정 열기",()->{try{startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}catch(Exception e){toast("휴대폰 설정에서 이 앱을 찾아주세요.");}}));
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)content.addView(button("새로고침 진행 알림 허용",()->requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},40)));
  main.post(()->scroll.scrollTo(0,oldY));
 }
 void refresh(){
  if(Repository.RUNNING.get()){toast("이미 조회 중입니다.");return;}
  try{startForegroundService(new Intent(this,RefreshService.class));toast("시세와 분석을 다시 조회합니다.");main.postDelayed(()->{if(!isDestroyed())draw();},500);}
  catch(RuntimeException e){Scheduler.request(this);toast("즉시 조회 시작이 제한되어 갱신 작업을 예약했습니다.");}
 }
 private void pin(){AppWidgetManager m=AppWidgetManager.getInstance(this);if(m.isRequestPinAppWidgetSupported())m.requestPinAppWidget(new ComponentName(this,MarketWidget.class),null,null);else toast("홈 화면 길게 누르기 → 위젯 → 코인 시장 위젯을 추가하세요.");}
 void openChart(String market){if(!market.matches("KRW-[A-Z0-9]+"))return;try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://upbit.com/exchange?code=CRIX.UPBIT."+market)));}catch(ActivityNotFoundException e){toast("링크를 열 브라우저가 없습니다.");}}
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
