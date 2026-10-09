package kr.sejong.coinwidget;
import android.app.AlertDialog;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.json.JSONObject;
final class HelpUi {
 static final String GUIDE = DetailContent.GUIDE;
 static int passes(JSONObject m,int mode){org.json.JSONArray items=Dashboard.list(m,mode);int n=0;for(int i=0;i<items.length();i++)if(items.optJSONObject(i).optBoolean("rule_pass"))n++;return n;}
 static AlertDialog show(MainActivity a){ScrollView scroll=new ScrollView(a);scroll.setPadding(a.dp(20),a.dp(8),a.dp(20),a.dp(8));scroll.addView(a.text(GUIDE,15,false));return new AlertDialog.Builder(a).setTitle("화면 읽는 법 · 쉬운 설명").setView(scroll).setPositiveButton("확인",null).show();}
 static void summary(MainActivity a,LinearLayout out,JSONObject root){
  JSONObject m=Renderer.obj(root,"market"),dom=Renderer.obj(root,"dominance");
  out.addView(a.text("간단 분석 · 지금 볼 세 가지",19,true));
  if(!Renderer.freshMarket(root)||m.optInt("schema")<7){out.addView(a.text("최신 자료를 확인하지 못했습니다. 새로고침 후 분석과 후보를 확인하세요.",14,false));return;}
  String btc=m.optString("btc_regime","판단 보류");double breadth=m.optDouble("breadth",Double.NaN);
  String action=btc.startsWith("판단 보류")||!Signals.finite(breadth)?"자료 부족으로 판단 보류":m.optBoolean("defensive",true)?m.optString("defense_reason","시장 자료 확인 필요")+". 관찰을 우선합니다.":breadth>=65?"상승 종목이 많습니다. 과열 여부를 함께 확인하세요.":"종목별 흐름이 갈립니다. 조건 충족 후보부터 비교하세요.";
  out.addView(a.text("1. BTC 추세: "+btc+"\n2. 알트 상승: "+Dashboard.f(breadth)+"% (09시 기준)\n3. 해석: "+action,14,false));
  out.addView(a.text("차트 통과: 금일 "+passes(m,0)+"개 / 9시 전 "+passes(m,1)+"개 / 중장기 "+passes(m,2)+"개. 차트 통과 수와 수익 검증은 별개입니다. 아래에서 세 분류별 실제 검증 결과를 확인하세요.",13,false));
  long age=System.currentTimeMillis()-m.optLong("quote_at");
  out.addView(a.text("분석 시각 "+Charts.date(m.optLong("quote_at"),"MM/dd HH:mm:ss")+" KST"+(age>300000?" · 이전 분석, 재조회 필요":" · 조회 당시 평가"),12,false));
 }
}
