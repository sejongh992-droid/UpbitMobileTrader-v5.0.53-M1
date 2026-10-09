package kr.sejong.coinwidget;
import org.json.*;
import java.io.IOException;
import java.time.*;
import java.util.Locale;
/** Daily reference date is deliberately separate from the successful HTTP check time. */
final class FxData {
 static JSONObject parse(JSONObject raw,long now)throws Exception{
  double rate=raw.getDouble("rate");String date=raw.getString("date");
  if(!Signals.finite(rate)||rate<=0||!date.matches("\\d{4}-\\d{2}-\\d{2}"))throw new IOException("환율 응답 형식 오류");
  if(LocalDate.parse(date).isAfter(Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).toLocalDate()))throw new IOException("미래 기준일 환율 응답");
  if(raw.has("base")&&!raw.optString("base").equals("USD")||raw.has("quote")&&!raw.optString("quote").equals("KRW"))throw new IOException("환율 통화 불일치");
  return new JSONObject().put("rate",rate).put("date",date).put("fetched_at",now).put("source","ECB / Frankfurter");
 }
 static String state(JSONObject root,long now){
  JSONObject fx=Renderer.obj(root,"fx");long checked=fx.optLong("fetched_at");
  if(!fx.has("rate"))return "조회 전";
  if(root.optBoolean("fx_failed"))return "조회 실패 · 이전 값";
  if(checked<=0||now-checked>24*Signals.HOUR||now<checked-60000)return "재조회 필요";
  try{if(LocalDate.parse(fx.getString("date")).isBefore(Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).toLocalDate().minusDays(7)))return "발표값 지연";}catch(Exception e){return "날짜 확인 필요";}
  return "조회 정상";
 }
 static String header(JSONObject root,long now){
  JSONObject fx=Renderer.obj(root,"fx");if(!fx.has("rate"))return "USD 환율 조회 전\n새로고침으로 확인";
  String date=fx.optString("date");if(date.length()==10)date=date.substring(5).replace('-','/');
  return String.format(Locale.KOREA,"USD %,.2f원",fx.optDouble("rate"))+"\n발표 "+date+" · ECB\n확인 "+Charts.date(fx.optLong("fetched_at"),"MM/dd HH:mm")+" · "+state(root,now);
 }
 static String settings(JSONObject root,long now,boolean compact){
  JSONObject fx=Renderer.obj(root,"fx");String date=fx.optString("date","—");
  if(compact)return "발표 "+(date.length()==10?date.substring(5).replace('-','/'):date)+" · ECB 일일값\n확인 "+Charts.date(fx.optLong("fetched_at"),"MM/dd HH:mm");
  return "자동 15분 · 08:30/08:50 추가\n절전·통신 상태에 따라 지연\n환율 "+state(root,now)+"\n발표 "+date+"\n확인 "+Charts.date(fx.optLong("fetched_at"),"MM/dd HH:mm:ss")+" KST\nECB 일일값 · 새 발표 전 유지\n위쪽 분류 → 종목 → 아래 목록\n매수 전 새로고침";
 }
 static String explanation(JSONObject root,long now){JSONObject f=Renderer.obj(root,"fx");return "환율 "+state(root,now)+"\n발표 기준일 "+f.optString("date","—")+"\n조회 "+Charts.date(f.optLong("fetched_at"),"MM/dd HH:mm:ss")+" KST\nECB 일일 기준환율입니다. 새로 조회해도 새 발표 전에는 날짜·값이 같습니다. 실시간 환전가는 아닙니다.";}
}
