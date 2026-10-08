package kr.sejong.coinwidget;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;
import org.json.*;
import java.util.*;
final class Renderer {
 static JSONObject obj(JSONObject o,String key){JSONObject r=o==null?null:o.optJSONObject(key);return r==null?new JSONObject():r;}
 static JSONArray arr(JSONObject o,String key){JSONArray r=o==null?null:o.optJSONArray(key);return r==null?new JSONArray():r;}
 static boolean freshMarket(JSONObject r){JSONObject m=obj(r,"market");long t=m.optLong("quote_at",m.optLong("fetched_at")),age=System.currentTimeMillis()-t;return t>0&&age>=-60_000&&age<=3*Signals.HOUR&&!r.optBoolean("market_failed",false);}
 static PendingIntent activity(Context c,int id,Intent i){
  int flags=PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE;
  if(Build.VERSION.SDK_INT>=35){ActivityOptions o=ActivityOptions.makeBasic();o.setPendingIntentCreatorBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);return PendingIntent.getActivity(c,id,i,flags,o.toBundle());}
  return PendingIntent.getActivity(c,id,i,flags);
 }
 static RemoteViews build(Context c,boolean compact){return Dashboard.build(c,compact?380:600,0);}
 static String shortCandidates(JSONArray a,int max){
  if(a.length()==0)return "조건 충족 없음";StringBuilder s=new StringBuilder();
  for(int i=0;i<Math.min(max,a.length());i++){JSONObject item=a.optJSONObject(i);if(item==null)continue;JSONObject q=obj(item,"quote");if(s.length()>0)s.append(" / ");s.append(q.optString("market").replace("KRW-","")).append(" ").append(Math.round(item.optDouble("score"))).append("점");}
  return s.toString();
 }
}
