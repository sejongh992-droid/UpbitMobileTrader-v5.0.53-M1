package kr.sejong.coinwidget;
import org.json.*;
/** Keep a failed lane's old observation and its original time together. */
final class SnapshotState {
 static String lane(int mode){return mode==1?"pre":mode==2?"long":"day";}
 static String watch(int mode){return mode==1?"pre_watchlist":mode==2?"long_watchlist":"watchlist";}
 static String strict(int mode){return mode==1?"next_candidates":mode==2?"long_candidates":"candidates";}
 static long time(JSONObject m,int mode){return m.optLong(lane(mode)+"_at",m.optLong("quote_at"));}
 static long nine(JSONObject m){return m.optLong("pre_recheck_at",m.optLong("recheck_at"));}
 static boolean failed(JSONObject m,int mode){return "error".equals(m.optString(lane(mode)+"_status"))||m.optBoolean(lane(mode)+"_retained");}
 static JSONObject merge(JSONObject previous,JSONObject next)throws JSONException{
  for(int mode=0;mode<3;mode++){
   String lane=lane(mode);next.put(lane+"_retained",false);
   if("error".equals(next.optString(lane+"_status"))&&Renderer.arr(next,watch(mode)).length()==0&&Renderer.arr(previous,watch(mode)).length()>0){
    next.put(watch(mode),new JSONArray(Renderer.arr(previous,watch(mode)).toString()));
    // An old condition must never become a current buy signal.
    next.put(strict(mode),new JSONArray()).put(lane+"_at",time(previous,mode)).put(lane+"_retained",true);
    if(mode==1)next.put("pre_count",0).put("pre_recheck_at",previous.optLong("pre_recheck_at",previous.optLong("recheck_at")));
   }
  }
  return next;
 }
 static String state(JSONObject m,int mode){
  String lane=lane(mode),s=m.optString(lane+"_status","");
  if(m.optBoolean(lane+"_retained"))return "조회 실패 · 이전 목록";
  if("error".equals(s))return "분석 조회 실패";
  if("partial".equals(s))return "일부 종목 분석";
  if("insufficient".equals(s))return "완료 봉 자료 부족";
  return "조회 당시 분석";
 }
}
