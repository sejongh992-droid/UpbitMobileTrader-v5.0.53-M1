package kr.sejong.coinwidget;
import android.content.Context;
import org.json.*;
import java.io.IOException;
import java.util.*;
final class DominanceData {
 static JSONObject fetch(Context c,Net net,JSONObject old)throws Exception{
  String source=c.getSharedPreferences("settings",0).getString("dominance_source","CoinPaprika");double value,capChange;long time;
  if(source.equals("CoinGecko")){
   String key=SecureStore.read(c);if(key.isEmpty())throw new IOException("CoinGecko Demo API 키를 입력하세요");
   JSONObject d=new JSONObject(net.get("https://api.coingecko.com/api/v3/global",key)).getJSONObject("data");
   value=d.getJSONObject("market_cap_percentage").getDouble("btc");capChange=d.optDouble("market_cap_change_percentage_24h_usd",Double.NaN);time=d.getLong("updated_at")*1000L;
  }else{
   source="CoinPaprika";JSONObject d=new JSONObject(net.get("https://api.coinpaprika.com/v1/global"));
   value=d.getDouble("bitcoin_dominance_percentage");capChange=d.optDouble("market_cap_change_24h",Double.NaN);time=d.getLong("last_updated")*1000L;
  }
  if(!Signals.finite(value)||value<=0||value>=100||time<=0||time>System.currentTimeMillis()+60_000L||System.currentTimeMillis()-time>3*Signals.HOUR)throw new IOException("도미넌스 응답 값 오류");
  JSONArray previous=(old!=null&&source.equals(old.optString("source")))?old.optJSONArray("history"):null;
  TreeMap<Long,double[]> points=new TreeMap<>();
  if(previous!=null)for(int i=0;i<previous.length();i++){
   JSONArray p=previous.optJSONArray(i);if(p==null||p.length()<2)continue;long t=p.optLong(0);double v=p.optDouble(1);
   if(t>time-14*24*Signals.HOUR&&t<=time&&Signals.finite(v)&&v>0&&v<100)points.put(t/(2*Signals.HOUR),new double[]{t,v});
  }
  points.put(time/(2*Signals.HOUR),new double[]{time,value});
  JSONArray history=new JSONArray();List<double[]> list=new ArrayList<>();
  for(double[]p:points.values()){history.put(new JSONArray().put((long)p[0]).put(p[1]));list.add(p);}
  double delta=Signals.dominanceDelta24(time,value,list);
  return new JSONObject().put("value",value).put("source",source).put("time",time).put("fetched_at",System.currentTimeMillis()).put("history",history).put("delta24",Signals.finite(delta)?delta:JSONObject.NULL).put("global_cap_24",Signals.finite(capChange)?capChange:JSONObject.NULL);
 }
}
