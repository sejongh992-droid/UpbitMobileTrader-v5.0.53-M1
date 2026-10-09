package kr.sejong.coinwidget;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public final class RecommendationPolicyTest {
 final Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
 @Test public void eachLaneHasItsOwnExpiry()throws Exception{
  long now=System.currentTimeMillis();JSONObject root=new UpgradeTest().fixture(),m=root.getJSONObject("market");m.put("quote_at",now-120001).put("recheck_at",now+3600000);
  assertTrue(Dashboard.freshRecommendation(root,0,now));assertFalse(Dashboard.freshRecommendation(root,1,now));assertTrue(Dashboard.freshRecommendation(root,2,now));
  m.put("quote_at",now-300001);assertFalse(Dashboard.freshRecommendation(root,0,now));assertFalse(Dashboard.freshRecommendation(root,2,now));
  m.put("quote_at",now).put("recheck_at",now+120000);assertFalse(Dashboard.freshRecommendation(root,1,now));
 }
 @Test public void passingChartNeverOverridesFailedProfitValidation()throws Exception{
  long now=System.currentTimeMillis();JSONObject root=new UpgradeTest().fixture(),m=root.getJSONObject("market");m.put("recheck_at",now+3600000);JSONObject row=m.getJSONArray("watchlist").getJSONObject(0);row.put("rule_pass",true).put("qualified",true).put("pre_qualified",true);
  for(int mode=0;mode<3;mode++)if(!ValidationPolicy.approved(mode)){
   assertFalse(Dashboard.qualified(m,row,mode));assertEquals("차트 충족 · 검증 보류",Dashboard.label(root,row,mode,now));Dashboard.move(c,960+mode,Dashboard.COIN,"KRW-LPT");DetailContent.Result d=DetailContent.forScreen(c,960+mode,Dashboard.COIN,root,mode,now);assertTrue(d.text.contains("수익 검증은 보류"));assertFalse(d.text.contains("검토 기준가"));
  }
 }
 @Test public void chartRankIsSeparateFromPermissionAndAtMostFiveQualify()throws Exception{
  List<JSONObject>a=new ArrayList<>();for(int i=0;i<9;i++)a.add(new JSONObject().put("quote",new JSONObject().put("market","KRW-T"+i)).put("rule_pass",i>0).put("score",99-i).put("qualified",i>0));
  JSONArray[]r=ResearchData.ranked(a,15,0);assertEquals(9,r[0].length());assertEquals(5,r[1].length());assertEquals("KRW-T1",r[0].getJSONObject(0).getJSONObject("quote").getString("market"));assertFalse(r[0].getJSONObject(5).getBoolean("qualified"));assertTrue(r[0].getJSONObject(5).getBoolean("rule_pass"));assertEquals("KRW-T0",r[0].getJSONObject(8).getJSONObject("quote").getString("market"));
 }
 @Test public void serializedPriceReferenceIsTheActualQuote()throws Exception{
  long now=System.currentTimeMillis();Signals.Quote q=new Signals.Quote("KRW-TEST","TEST",100,1,2e10,false,now);StrategyV130.Result s=new StrategyV130.Result();s.pass=true;s.support=98;s.resistance=105;s.atr=2;StrategyV130.geometry(s,100);
  for(int mode=0;mode<3;mode++){JSONObject r=ResearchData.record(q,s,mode);assertEquals(100,r.getDouble("entry_low"),0);assertEquals(100,r.getDouble("entry_high"),0);assertEquals(s.rr,r.getDouble("reference_rr"),0);assertEquals(ValidationPolicy.approved(mode),r.getBoolean(mode==1?"pre_qualified":"qualified"));}
 }
}
