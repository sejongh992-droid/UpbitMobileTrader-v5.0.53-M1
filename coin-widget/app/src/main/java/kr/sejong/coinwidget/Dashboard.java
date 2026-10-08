package kr.sejong.coinwidget;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import org.json.*;
import java.util.*;
final class Dashboard {
 static final int INK=0xff1c2940,PURPLE=0xff6550cc,MUTED=0xff738096;
 static final String NAV="kr.sejong.coinwidget.NAV";
 static String f(double v){return Signals.finite(v)?String.format(Locale.KOREA,"%.1f",v):"—";}
 static int mode(Context c,int id){return c.getSharedPreferences("widget_ui",0).getInt("mode"+id,0);}
 static void move(Context c,int id,int action){android.content.SharedPreferences p=c.getSharedPreferences("widget_ui",0);if(action<2)p.edit().putInt("mode"+id,action).putInt("page"+id,0).apply();else p.edit().putInt("page"+id,Math.max(0,p.getInt("page"+id,0)+(action==2?-1:1))).apply();}
 static PendingIntent nav(Context c,int id,int action){Intent i=new Intent(c,MarketWidget.class).setAction(NAV).addFlags(Intent.FLAG_RECEIVER_FOREGROUND).setData(Uri.parse("coinwidget://nav/"+id+"/"+action)).putExtra("widget",id).putExtra("nav",action);return PendingIntent.getBroadcast(c,100+action,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static JSONArray list(JSONObject market,int mode){return Renderer.arr(market,mode==1?"next_candidates":"watchlist");}
 static RemoteViews build(Context c,int height,int id){
  height=(int)(height/Math.max(1f,c.getResources().getConfiguration().fontScale));
  boolean compact=height<480;int rows=height>=750?5:height>=700?4:height>=600?3:height>=520?2:1;
  JSONObject r=Repository.load(c),m=Renderer.obj(r,"market"),btc=Renderer.obj(m,"btc"),t=Renderer.obj(m,"btc_technical"),dom=Renderer.obj(r,"dominance"),fx=Renderer.obj(r,"fx");
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget_v110);int mode=mode(c,id);boolean fresh=Renderer.freshMarket(r)&&m.optInt("schema")>=3;
  v.setTextViewText(R.id.fx,fx.has("rate")?"USD  "+Charts.number(fx.optDouble("rate"))+"원\n"+fx.optString("date")+(r.optBoolean("fx_failed")?" · 이전":" 기준"):"환율 조회 전");
  v.setTextViewText(R.id.btc_price,btc.has("price")?Charts.number(btc.optDouble("price"))+"원":"— 원");
  v.setTextViewText(R.id.btc_sub,"09시 기준 "+Charts.pct(btc.optDouble("day_pct",Double.NaN))+"  ·  6시간 "+Charts.pct(t.optDouble("return6h",Double.NaN)));
  v.setTextColor(R.id.btc_sub,btc.optDouble("day_pct")>=0?Charts.UP:Charts.DOWN);v.setTextViewText(R.id.btc_state,m.optString("btc_regime","조회 전"));
  v.setImageViewBitmap(R.id.btc_chart,MiniCharts.btc(Renderer.arr(m,"daily")));
  v.setTextViewText(R.id.btc_stats,"완료 일봉 RSI "+f(t.optDouble("rsi",Double.NaN))+"  ·  20일 저점 "+MiniCharts.won(t.optDouble("low20",Double.NaN))+" / 고점 "+MiniCharts.won(t.optDouble("high20",Double.NaN)));
  v.setTextViewText(R.id.dom_title,"비트코인 도미넌스  "+(dom.has("value")?String.format(Locale.KOREA,"%.2f%%",dom.optDouble("value")):"—"));
  v.setImageViewBitmap(R.id.dom_chart,MiniCharts.share(dom));
  double delta=dom.optDouble("delta24",Double.NaN);boolean domFresh=!r.optBoolean("dom_failed")&&dom.optLong("time")>0&&System.currentTimeMillis()-dom.optLong("time")>=-60000&&System.currentTimeMillis()-dom.optLong("time")<=3*Signals.HOUR;
  String change=Signals.finite(delta)?String.format(Locale.KOREA,"약24시간 %+.2f%%p",delta):"24h 비교 기록 수집 중";

  v.setTextViewText(R.id.dom_meta,dom.optString("source")+" · "+Charts.date(dom.optLong("time"),"HH:mm")+" · "+change+(domFresh?"":" · 이전 자료"));
  v.setTextViewText(R.id.breadth,"09시 기준 상승 "+f(m.optDouble("breadth",Double.NaN))+"%  ·  "+m.optInt("advancing")+"/"+m.optInt("alt_count")+"종목");
  String regime=Signals.altRegime(m.optDouble("breadth",Double.NaN),m.optString("btc_regime"),domFresh?delta:Double.NaN);
  if(m.optBoolean("defensive"))regime="방어 관찰 · "+m.optString("defense_reason");
  v.setTextViewText(R.id.market_summary,regime+(fresh?"":" · 재조회 필요"));
  String deadline=Charts.date(m.optLong("recheck_at"),"MM/dd HH:mm");boolean expired=mode==1&&System.currentTimeMillis()>=m.optLong("recheck_at");
  JSONArray items=fresh&&!expired?list(m,mode):new JSONArray();int total=items.length(),pages=Math.max(1,(total+rows-1)/rows);
  int savedPage=c.getSharedPreferences("widget_ui",0).getInt("page"+id,0);int page=Math.max(0,savedPage)%pages;
  if(page!=savedPage)c.getSharedPreferences("widget_ui",0).edit().putInt("page"+id,page).apply();
  v.setTextColor(R.id.tab_today,mode==0?PURPLE:MUTED);v.setTextColor(R.id.tab_morning,mode==1?PURPLE:MUTED);
  v.setInt(R.id.tab_today,"setBackgroundResource",mode==0?R.drawable.tab_selected:R.drawable.tab_idle);v.setInt(R.id.tab_morning,"setBackgroundResource",mode==1?R.drawable.tab_selected:R.drawable.tab_idle);
  v.setTextViewText(R.id.today,!fresh?"자료 재조회 필요":mode==0?"조건 충족 "+Renderer.arr(m,"candidates").length()+" · 관심 목록 "+total+"개":deadline+" 전 · 조건부 검토 "+m.optInt("pre_count")+"개");
  v.setTextViewText(R.id.next_day,mode==0?"조회 당시 평가 · 이름을 누르면 차트":"다음 09시 전 재점검 · 상승 예측 아님");
  v.removeAllViews(R.id.coin_rows);
  if(total==0){RemoteViews empty=new RemoteViews(c.getPackageName(),R.layout.coin_empty);empty.setTextViewText(R.id.empty_label,expired?"09시가 지났습니다. 새로고침하세요.":fresh?"검증 가능한 후보가 없습니다.\n조건을 충족하지 않은 종목은 채우지 않습니다.":"새로고침 후 후보와 근거가 표시됩니다.");v.addView(R.id.coin_rows,empty);}
  for(int j=page*rows;j<Math.min(total,(page+1)*rows);j++){
   JSONObject a=items.optJSONObject(j),q=Renderer.obj(a,"quote");RemoteViews row=new RemoteViews(c.getPackageName(),R.layout.coin_row);
   String symbol=q.optString("market").replace("KRW-","");String name=q.optString("name",symbol);
   row.setTextViewText(R.id.row_name,(j+1)+"  "+name+" ("+symbol+")");
   row.setTextViewText(R.id.row_price,Charts.number(q.optDouble("price"))+"원  "+Charts.pct(q.optDouble("day_pct")));
   row.setTextColor(R.id.row_price,q.optDouble("day_pct")>=0?Charts.UP:Charts.DOWN);
   boolean qualified=a.optBoolean(mode==1?"pre_qualified":"qualified");
   row.setTextViewText(R.id.row_status,Math.round(a.optDouble(mode==1?"morning_score":"score"))+"점 · "+(qualified?"조건 충족":a.optString("state").equals("추격 주의")?"추격 주의":"관찰 대기")+" · 상대6h "+Charts.pct(a.optDouble("relative6h",Double.NaN)).replace("%","%p"));row.setTextColor(R.id.row_status,qualified?PURPLE:MUTED);
   Intent chart=new Intent(Intent.ACTION_VIEW,Uri.parse("https://upbit.com/exchange?code=CRIX.UPBIT."+q.optString("market")));row.setOnClickPendingIntent(R.id.row_root,Renderer.activity(c,300+j+id*100,chart));
   v.addView(R.id.coin_rows,row);
  }
  v.setTextViewText(R.id.page_label,(page+1)+" / "+pages+"   ·   "+total+"종목");
  for(int action=0;action<4;action++)v.setOnClickPendingIntent(new int[]{R.id.tab_today,R.id.tab_morning,R.id.page_prev,R.id.page_next}[action],nav(c,id,action));
  v.setTextViewText(R.id.updated,(c.getSharedPreferences("settings",0).getBoolean("auto",true)?"2시간 자동":"수동 조회")+" · 조회 시세\n"+Charts.date(Renderer.obj(m,"btc").optLong("time",m.optLong("quote_at")),"MM/dd HH:mm")+" KST");
  JSONArray errors=Renderer.arr(r,"errors");v.setTextViewText(R.id.warning,errors.length()>0?errors.optString(0):!m.optString("screening_error").isEmpty()?m.optString("screening_error"):"점수는 확률 아님 · 매수 전 최신 조회");
  v.setTextViewText(R.id.refresh,Repository.RUNNING.get()?"조회 중…":"↻ 새로고침");
  v.setOnClickPendingIntent(R.id.refresh,PendingIntent.getForegroundService(c,12,new Intent(c,RefreshService.class).setAction("kr.sejong.coinwidget.REFRESH"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  PendingIntent app=Renderer.activity(c,10+id*10+mode,new Intent(c,MainActivity.class).setData(Uri.parse("coinwidget://details/"+id+"/"+mode)).putExtra("view_mode",mode));v.setOnClickPendingIntent(R.id.open_app,app);v.setOnClickPendingIntent(R.id.title,app);
  v.setOnClickPendingIntent(R.id.btc_chart,Renderer.activity(c,11,new Intent(Intent.ACTION_VIEW,Uri.parse("https://upbit.com/exchange?code=CRIX.UPBIT.KRW-BTC")))) ;
  v.setOnClickPendingIntent(R.id.dom_title,Renderer.activity(c,15,new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.tradingview.com/symbols/BTC.D/"))));
  if(height<680){v.setViewVisibility(R.id.next_day,View.GONE);v.setViewVisibility(R.id.chart_legend,View.GONE);v.setViewVisibility(R.id.btc_stats,View.GONE);}
  if(height<600)v.setViewVisibility(R.id.dom_meta,View.GONE);
  if(height<540)v.setViewVisibility(R.id.breadth,View.GONE);
  if(height<360)v.setViewVisibility(R.id.warning,View.GONE);
  if(compact){v.setViewVisibility(R.id.btc_sub,View.GONE);v.setTextViewTextSize(R.id.btc_price,android.util.TypedValue.COMPLEX_UNIT_SP,22);}
  if(android.os.Build.VERSION.SDK_INT>=31)v.setViewLayoutHeight(R.id.btc_chart,height<540?30:height<640?48:72,android.util.TypedValue.COMPLEX_UNIT_DIP);
  if(compact){v.setViewVisibility(R.id.btc_chart,View.GONE);v.setViewVisibility(R.id.breadth,View.GONE);v.setViewVisibility(R.id.dom_chart,View.GONE);v.setViewVisibility(R.id.dom_meta,View.GONE);v.setViewVisibility(R.id.btc_stats,View.GONE);v.setViewVisibility(R.id.next_day,View.GONE);v.setViewVisibility(R.id.chart_legend,View.GONE);v.setViewVisibility(R.id.dom_title,View.GONE);}
  return v;
 }
}
