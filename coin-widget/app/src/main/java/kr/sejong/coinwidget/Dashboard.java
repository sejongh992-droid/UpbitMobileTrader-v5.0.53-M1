package kr.sejong.coinwidget;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.text.*;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import org.json.*;
import java.util.*;
final class Dashboard {
 static final int INK=0xff1c2940,PURPLE=0xff6550cc,MUTED=0xff738096;
 static final String NAV="kr.sejong.coinwidget.NAV";
 static final int MARKET=4,HELP=5,SETTINGS=6,BACK=7,AUTO=8,BTC=9,DOM=10,LONG=11,COIN=20;
 static String f(double v){return Signals.finite(v)?String.format(Locale.KOREA,"%.1f",v):"—";}
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("widget_ui",0);}
 static int mode(Context c,int id){return prefs(c).getInt("mode"+id,0);}
 static int screen(Context c,int id){return prefs(c).getInt("screen"+id,0);}
 static void move(Context c,int id,int action){move(c,id,action,"");}
 static void move(Context c,int id,int action,String market){
  SharedPreferences p=prefs(c);SharedPreferences.Editor e=p.edit();
  if(action==0||action==1||action==LONG)e.putInt("mode"+id,action==LONG?2:action).putInt("screen"+id,0).putInt("page"+id,0);
  else if(action==2||action==3){String key=(screen(c,id)==0?"page":"detailPage")+id;int count=Math.max(1,p.getInt("pages"+id,1));int target=(p.getInt(key,0)+(action==2?count-1:1))%count;if(market.matches("page:[0-9]{1,4}"))target=Integer.parseInt(market.substring(5));e.putInt(key,target);}
  else if(action==BACK)e.putInt("screen"+id,0);
  else if(action==AUTO){boolean auto=c.getSharedPreferences("settings",0).getBoolean("auto",true);c.getSharedPreferences("settings",0).edit().putBoolean("auto",!auto).apply();Scheduler.ensure(c);}
  else if(action==HELP||action==SETTINGS||action==BTC||action==DOM||action==MARKET)e.putInt("screen"+id,action).putInt("detailPage"+id,0);
  else if(action==COIN&&market.matches("KRW-[A-Z0-9]+"))e.putInt("screen"+id,COIN).putString("coin"+id,market).putInt("detailPage"+id,0);
  e.apply();
 }
 static PendingIntent nav(Context c,int id,int action){return nav(c,id,action,"");}
 static PendingIntent nav(Context c,int id,int action,String market){
  Intent i=new Intent(c,MarketWidget.class).setAction(NAV).addFlags(Intent.FLAG_RECEIVER_FOREGROUND).setData(Uri.parse("coinwidget://nav/"+id+"/"+action+"/"+Uri.encode(market))).putExtra("widget",id).putExtra("nav",action).putExtra("market",market);
  return PendingIntent.getBroadcast(c,100+action,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 static boolean freshRecommendation(JSONObject root,int mode,long now){
  JSONObject m=Renderer.obj(root,"market");long age=now-m.optLong("quote_at");
  return Renderer.freshMarket(root)&&m.optInt("schema")>=4&&age>=-60000&&age<=5*60_000L&&(mode!=1||now<m.optLong("recheck_at"));
 }
 static JSONArray list(JSONObject m,int mode){
  JSONArray source=Renderer.arr(m,mode==1?"next_candidates":mode==2?"long_candidates":"candidates"),result=new JSONArray();
  if(m.optBoolean(mode==2?"long_defensive":"defensive",true))return result;
  for(int i=0;i<source.length();i++){JSONObject a=source.optJSONObject(i);if(a!=null&&a.optBoolean(mode==1?"pre_qualified":"qualified"))result.put(a);}return result;
 }
 static void visible(RemoteViews v,int id,boolean show){v.setViewVisibility(id,show?View.VISIBLE:View.GONE);}
 static String summary(JSONObject m){return m.optBoolean("defensive",true)?"매수 관망 · "+m.optString("defense_reason","시장 자료 확인 필요"):"BTC "+m.optString("btc_regime","판단 보류")+" · 알트 상승 "+f(m.optDouble("breadth",Double.NaN))+"%";}
 static int bodyHeight(int height){return Math.max(60,height-250);}
 static int rows(Context c,int height){int body=bodyHeight(height);int banner=body>=250?56:0;float font=Math.max(1f,c.getResources().getConfiguration().fontScale);return Math.max(1,Math.min(8,(int)((body-banner-24)/(76*font))));}
 static RemoteViews build(Context c,int height,int id){return build(c,340,height,id);}
 static RemoteViews build(Context c,int width,int height,int id){
  long now=System.currentTimeMillis();int mode=mode(c,id),screen=screen(c,id);JSONObject root=Repository.load(c),m=Renderer.obj(root,"market"),btc=Renderer.obj(m,"btc"),t=Renderer.obj(m,"btc_technical"),dom=Renderer.obj(root,"dominance"),fx=Renderer.obj(root,"fx");
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget_v120);
  v.setTextViewText(R.id.fx,fx.has("rate")?"USD "+Charts.number(fx.optDouble("rate"))+"원\n"+fx.optString("date")+(root.optBoolean("fx_failed")?" · 이전":" 기준"):"환율 조회 전");
  int[]tabs={R.id.tab_today,R.id.tab_morning,R.id.tab_long};int[]actions={0,1,LONG};
  for(int j=0;j<tabs.length;j++){v.setTextColor(tabs[j],mode==j?PURPLE:MUTED);v.setInt(tabs[j],"setBackgroundResource",mode==j?R.drawable.tab_selected:R.drawable.tab_idle);v.setOnClickPendingIntent(tabs[j],nav(c,id,actions[j]));}
  visible(v,R.id.market_panel,screen==MARKET);visible(v,R.id.recommendations,screen==0);visible(v,R.id.detail_panel,screen!=0&&screen!=MARKET);
  boolean fresh=freshRecommendation(root,mode,now);JSONArray items=fresh?list(m,mode):new JSONArray();
  int pages=1,page=0;v.setOnClickPendingIntent(R.id.page_label,nav(c,id,BACK));
  if(screen==0){
   int count=rows(c,height);pages=Math.max(1,(items.length()+count-1)/count);page=Math.floorMod(prefs(c).getInt("page"+id,0),pages);
   visible(v,R.id.breadth,bodyHeight(height)>=250);v.setTextViewText(R.id.breadth,summary(m));
   String phase=BeforeNine.entryWindow(now)?"9시 전 매수 검토":Charts.date(m.optLong("recheck_at"),"MM/dd")+" 09시 예비";
   v.setTextViewText(R.id.today,mode==0?"금일 상승 조건 · "+items.length()+"종목":mode==2?"1~3개월 일봉 조건 · "+items.length()+"종목":phase+" · "+items.length()+"종목");v.removeAllViews(R.id.coin_rows);
   if(items.length()==0){RemoteViews empty=new RemoteViews(c.getPackageName(),R.layout.coin_empty);String msg;
    if(Repository.RUNNING.get())msg="최신 시세 분석 중\n잠시 후 이 화면에 표시됩니다.";
    else if(!fresh)msg=mode==1&&m.optLong("recheck_at")>0&&now>=m.optLong("recheck_at")?"이 목록의 9시 진입 마감\n장중 추천 또는 새로고침을 선택하세요.":"최신 분석이 필요합니다\n새로고침을 눌러 주세요.\n추천 유효시간은 조회 후 5분입니다.";
    else msg="지금은 매수 관망\n"+(m.optBoolean(mode==2?"long_defensive":"defensive",true)?(mode==2?"BTC 일봉 하락 또는 자료 부족":m.optString("defense_reason","시장 방어 조건")):"상승 추세·거래 증가·가격 조건을\n함께 통과한 종목이 없습니다.");
    empty.setTextViewText(R.id.empty_label,msg);v.addView(R.id.coin_rows,empty);
   }
   for(int j=page*count;j<Math.min(items.length(),(page+1)*count);j++){
    JSONObject a=items.optJSONObject(j),q=Renderer.obj(a,"quote");String code=q.optString("market"),symbol=code.replace("KRW-","");RemoteViews row=new RemoteViews(c.getPackageName(),R.layout.coin_row);
    row.setTextViewText(R.id.row_name,(j+1)+"  "+q.optString("name",symbol)+" ("+symbol+")");
    row.setTextViewText(R.id.row_price,Charts.number(q.optDouble("price"))+"원  "+Charts.pct(q.optDouble("day_pct")));
    row.setTextColor(R.id.row_price,q.optDouble("day_pct")>=0?Charts.UP:Charts.DOWN);
    row.setTextViewText(R.id.row_status,mode==2?"30일 "+Charts.pct(a.optDouble("return30",Double.NaN))+" · 거래 "+f(a.optDouble("volume_ratio",Double.NaN))+"배  ›":"15분 "+Charts.pct(a.optDouble("pulse_change",Double.NaN))+" · 거래 "+f(a.optDouble("pulse_ratio",Double.NaN))+"배  ›");
    row.setOnClickPendingIntent(R.id.row_root,nav(c,id,COIN,code));v.addView(R.id.coin_rows,row);
   }
   v.setTextViewText(R.id.page_label,(page+1)+" / "+pages+"\n"+(mode==1&&!BeforeNine.entryWindow(now)?"예비 · 08시 재검토":"종목을 눌러 분석"));
  }else if(screen==MARKET){
   v.setTextViewText(R.id.btc_price,btc.has("price")?Charts.number(btc.optDouble("price"))+"원":"— 원");
   v.setTextViewText(R.id.btc_state,m.optString("btc_regime","조회 전"));
   v.setTextViewText(R.id.btc_sub,"09시 기준 "+Charts.pct(btc.optDouble("day_pct",Double.NaN))+" · 6h "+Charts.pct(t.optDouble("return6h",Double.NaN)));
   v.setImageViewBitmap(R.id.btc_chart,MiniCharts.btc(Renderer.arr(m,"daily")));
   v.setTextViewText(R.id.btc_stats,"일봉 · 금색 20일 / 청록 60일 평균\n완료 일봉 RSI "+f(t.optDouble("rsi",Double.NaN))+" · 차트를 누르면 확대");
   v.setTextViewText(R.id.dom_title,"BTC 도미넌스 "+(dom.has("value")?String.format(Locale.KOREA,"%.2f%%",dom.optDouble("value")):"—"));v.setImageViewBitmap(R.id.dom_chart,MiniCharts.share(dom));
   v.setTextViewText(R.id.market_summary,summary(m)+(Renderer.freshMarket(root)?"":" · 이전 자료"));
   visible(v,R.id.btc_label,height>=480);visible(v,R.id.btc_state,height>=480);visible(v,R.id.btc_chart,height>=480);visible(v,R.id.btc_stats,height>=600);visible(v,R.id.dom_group,height>=540);
   v.setOnClickPendingIntent(R.id.btc_chart,nav(c,id,BTC));v.setOnClickPendingIntent(R.id.dom_group,nav(c,id,DOM));
   v.setTextViewText(R.id.page_label,"시장 요약\n차트는 눌러 확대");v.setOnClickPendingIntent(R.id.page_prev,nav(c,id,BTC));v.setOnClickPendingIntent(R.id.page_next,nav(c,id,DOM));
   v.setTextViewText(R.id.page_prev,"BTC 차트");v.setTextViewText(R.id.page_next,"비중 분석");
  }else{
   DetailContent.Result d=DetailContent.forScreen(c,id,screen,root,mode,now);v.setTextViewText(R.id.detail_title,d.title);v.setTextViewText(R.id.detail_price,d.price);visible(v,R.id.detail_price,!d.price.isEmpty());
   visible(v,R.id.detail_action,screen==SETTINGS);v.setTextViewText(R.id.detail_action,c.getSharedPreferences("settings",0).getBoolean("auto",true)?"자동 조회 켜짐 · 누르면 끄기":"자동 조회 꺼짐 · 누르면 켜기");v.setOnClickPendingIntent(R.id.detail_action,nav(c,id,AUTO));
   if(d.chart!=null&&height>=480){visible(v,R.id.detail_chart,true);v.setImageViewBitmap(R.id.detail_chart,d.chart);visible(v,R.id.detail_text,false);visible(v,R.id.chart_caption,true);v.setTextViewText(R.id.chart_caption,d.text);}
   else{
    int available=bodyHeight(height)-48-(d.price.isEmpty()?0:36)-(screen==SETTINGS?52:0);
    List<String>chunks=split(c,d.text,Math.max(160,width-20),Math.max(24,available));pages=chunks.size();page=Math.floorMod(prefs(c).getInt("detailPage"+id,0),pages);v.setTextViewText(R.id.detail_text,chunks.get(page));
   }
   v.setTextViewText(R.id.page_label,(page+1)+" / "+pages+"\n↩ 목록으로");
  }
  prefs(c).edit().putInt("pages"+id,pages).apply();
  if(screen!=MARKET){v.setOnClickPendingIntent(R.id.page_prev,nav(c,id,2,"page:"+((page+pages-1)%pages)));v.setOnClickPendingIntent(R.id.page_next,nav(c,id,3,"page:"+((page+1)%pages)));}
  String status=Repository.RUNNING.get()?"조회 중…":root.optBoolean("market_failed")?"조회 실패 · 이전 자료":fresh?"추천 유효 ~ "+Charts.date(Math.min(m.optLong("quote_at")+5*60_000L,mode==1?m.optLong("recheck_at"):Long.MAX_VALUE),"HH:mm"):"추천 재조회 필요";
  v.setTextViewText(R.id.updated,"분석 "+Charts.date(m.optLong("quote_at"),"MM/dd HH:mm:ss")+" KST · "+status+"\n"+(c.getSharedPreferences("settings",0).getBoolean("auto",true)?"15분 자동 · 절전 시 지연":"수동 조회")+" · 점수는 상승 확률 아님");
  v.setOnClickPendingIntent(R.id.market_home,nav(c,id,screen==MARKET?BACK:MARKET));v.setTextViewText(R.id.market_home,screen==MARKET?"목록":"시장");
  v.setTextViewText(R.id.open_app,screen==HELP?"설정":screen==SETTINGS?"목록":"설명");v.setOnClickPendingIntent(R.id.open_app,nav(c,id,screen==HELP?SETTINGS:screen==SETTINGS?BACK:HELP));
  v.setTextViewText(R.id.refresh,Repository.RUNNING.get()?"조회 중…":"↻ 갱신");v.setOnClickPendingIntent(R.id.refresh,PendingIntent.getForegroundService(c,12,new Intent(c,RefreshService.class).setAction("kr.sejong.coinwidget.REFRESH"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  return v;
 }
 static List<String> split(Context c,String text,int widthDp,int heightDp){
  float density=c.getResources().getDisplayMetrics().density;
  // Measure with the same TextView defaults as RemoteViews: Korean fallback fonts
  // and high-quality line breaking can be taller than a default StaticLayout.
  android.widget.TextView measure=new android.widget.TextView(c);measure.setTextSize(TypedValue.COMPLEX_UNIT_SP,15);measure.setIncludeFontPadding(false);measure.setLineSpacing(2*density,1);
  List<String>pages=new ArrayList<>();int width=Math.max(80,(int)(widthDp*density)),height=Math.max(1,(int)((heightDp-4)*density));String rest=text;measure.setLayoutParams(new android.view.ViewGroup.LayoutParams(width,android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
  while(!rest.isEmpty()){
   measure.setText(rest);measure.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));android.text.Layout layout=measure.getLayout();int line=0;
   while(line+1<layout.getLineCount()&&layout.getLineBottom(line+1)<=height)line++;
   int end=layout.getLineEnd(line);if(end<=0)end=Math.min(1,rest.length());
   // A truncated paragraph can wrap differently; verify the exact page text too.
   while(end>1){measure.setText(rest.substring(0,end).trim());measure.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));if(measure.getLayout().getHeight()<=height)break;int previous=end;int cut=measure.getLayout().getLineStart(Math.max(0,measure.getLayout().getLineCount()-1));end=cut>0?Math.min(end-1,cut):end-1;if(end>=previous)end=previous-1;}
   pages.add(rest.substring(0,end).trim());rest=rest.substring(end).replaceFirst("^\\s+","");
  }
  if(pages.isEmpty())pages.add("자료가 없습니다. 새로고침하세요.");return pages;
 }
}
