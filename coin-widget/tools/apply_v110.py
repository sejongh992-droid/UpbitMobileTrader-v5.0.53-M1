"""Deterministic source/resource migration from the verified 1.0.1 baseline.
Run once before Gradle. Private keys and account credentials are never included.
"""
from pathlib import Path
from xml.sax.saxutils import escape
root=Path(__file__).resolve().parents[1]
j=root/'app/src/main/java/kr/sejong/coinwidget'
r=root/'app/src/main/res'
def edit(p,old,new):
 s=p.read_text()
 if new in s:return
 if old not in s:raise RuntimeError(f'Migration anchor missing: {p} {old[:80]}')
 p.write_text(s.replace(old,new))
edit(root/'app/build.gradle','versionCode 2','versionCode 3')
edit(root/'app/build.gradle',"versionName '1.0.1'","versionName '1.1.0'")
s=(j/'MarketData.java').read_text()
if 'return ResearchData.finish' not in s:
 s=s[:s.index('  String screeningError=')]+'''  return ResearchData.finish(net,quotes,btc,daily,now);
 }
}
''';(j/'MarketData.java').write_text(s)
s=(j/'Renderer.java').read_text()
if 'return Dashboard.build' not in s:
 a=s.index(' static RemoteViews build(');b=s.index(' static String shortCandidates',a)
 s=s[:a]+''' static RemoteViews build(Context c,boolean compact){return Dashboard.build(c,compact?380:600,0);}
'''+s[b:];(j/'Renderer.java').write_text(s)
edit(j/'MarketWidget.java','Renderer.build(c,h<480)','Dashboard.build(c,h,id)')
edit(j/'MarketWidget.java','public final class MarketWidget extends AppWidgetProvider {','''public final class MarketWidget extends AppWidgetProvider {
    @Override public void onReceive(Context c,Intent i){
        if(i!=null&&Dashboard.NAV.equals(i.getAction())){
            int id=i.getIntExtra("widget",0),a=i.getIntExtra("nav",-1);
            if(a>=0&&a<4){Dashboard.move(c,id,a);renderAll(c);}return;
        }
        super.onReceive(c,i);
    }''')
edit(j/'MainActivity.java','v1.0.1','v1.1.0')
edit(j/'MainActivity.java','if(!cached.has("fetched_at")||','if(cached.optInt("schema")<2||!cached.has("fetched_at")||')
edit(j/'MainActivity.java','View card=Renderer.build(this,false).apply(this,content);','View card=Dashboard.build(this,660,0).apply(this,content);')
a='''   for(int id:new int[]{R.id.btc_chart,R.id.dom_chart}){View chart=card.findViewById(id);LinearLayout.LayoutParams cp=(LinearLayout.LayoutParams)chart.getLayoutParams();cp.height=dp(id==R.id.btc_chart?100:62);cp.weight=0;chart.setLayoutParams(cp);}'''
edit(j/'MainActivity.java',a,'''   for(int action=0;action<4;action++){final int which=action;card.findViewById(new int[]{R.id.tab_today,R.id.tab_morning,R.id.page_prev,R.id.page_next}[action]).setOnClickListener(v->{Dashboard.move(this,0,which);draw();});}''')
edit(j/'MainActivity.java','DetailUi.add(this,content,Repository.load(this));','WatchDetails.add(this,content,Repository.load(this));')
edit(j/'Net.java','CoinMarketWidget/1.0.1','CoinMarketWidget/1.1.0')
# Preserve helpers and independent tests, but trigger the new schema only in real activity launches.
for p in (root/'app/src/androidTest').rglob('*.java'):
 s=p.read_text().replace('new JSONObject().put("fetched_at",now)','new JSONObject().put("schema",2).put("fetched_at",now)').replace('new JSONObject().put("fetched_at",System.currentTimeMillis())','new JSONObject().put("schema",2).put("fetched_at",System.currentTimeMillis())')
 p.write_text(s)
# Resource helpers keep dimensions consistent and all text adjustable via sp.
def tx(id,text='',size=12,color='#1C2940',bold=False,extra='',height='wrap_content',width='match_parent'):
 return f'<TextView android:id="@+id/{id}" android:layout_width="{width}" android:layout_height="{height}" android:text="{escape(text)}" android:textSize="{size}sp" android:textColor="{color}" android:includeFontPadding="false"'+(' android:textStyle="bold"' if bold else '')+' '+extra+'/>'
def img(id,height,desc):return f'<ImageView android:id="@+id/{id}" android:layout_width="match_parent" android:layout_height="{height}dp" android:scaleType="fitCenter" android:contentDescription="{desc}"/>'
def box(inner,extra=''):return '<LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" '+extra+'>'+inner+'</LinearLayout>'
def row(inner,extra=''):return '<LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:gravity="center_vertical" '+extra+'>'+inner+'</LinearLayout>'
muted='#738096';purple='#6550CC'
header=row(tx('title','코인 인사이트',17,bold=True,width='0dp',extra='android:layout_weight="1"')+tx('fx','USD/KRW',10,muted,width='wrap_content',extra='android:gravity="end"'))
btc=row(tx('btc_label','비트코인  BTC',12,bold=True,width='0dp',extra='android:layout_weight="1"')+tx('btc_state','조회 전',11,purple,width='wrap_content'))
btc+=tx('btc_price','— 원',26,bold=True,extra='android:layout_marginTop="3dp" android:maxLines="1" android:ellipsize="end"')
btc+=tx('btc_sub','조회 시점 시세',11,muted,extra='android:layout_marginTop="2dp"')
btc+=row(tx('chart_legend','일봉 45개   •  20일선 / 60일선',10,muted,extra='android:layout_marginTop="5dp"'))
btc+=img('btc_chart',82,'비트코인 일봉. 누르면 업비트 차트 열기')
btc+=tx('btc_stats','일봉 기준 지표',10,muted,extra='android:maxLines="1" android:ellipsize="end"')
btc=box(btc,'android:layout_marginTop="10dp"')
dom=tx('dom_title','비트코인 도미넌스',12,bold=True)+img('dom_chart',34,'비트코인과 기타 코인의 시가총액 비중 비교')+tx('dom_meta','동일 공급자 관측 기준',10,muted,extra='android:maxLines="1" android:ellipsize="end"')
dom=box(dom,'android:layout_marginTop="10dp"')
summary=box(tx('market_summary','시장 분석',12,purple,True)+tx('breadth','알트 상승 종목 비율',11,muted,extra='android:layout_marginTop="3dp"'),'android:background="@drawable/soft_card" android:padding="9dp" android:layout_marginTop="9dp"')
tabs=row(tx('tab_today','오늘 후보',12,purple,True,width='0dp',height='36dp',extra='android:layout_weight="1" android:gravity="center" android:background="@drawable/tab_selected"')+tx('tab_morning','09시 전 매수 검토',12,purple,True,width='0dp',height='36dp',extra='android:layout_weight="1" android:gravity="center" android:background="@drawable/tab_idle"'),'android:layout_marginTop="9dp"')
body=tx('today','검증 후 후보 표시',11,bold=True,extra='android:layout_marginTop="6dp"')+tx('next_day','이름을 누르면 개별 차트',10,muted,extra='android:layout_marginTop="3dp"')
body+='<LinearLayout android:id="@+id/coin_rows" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:layout_marginTop="4dp"/>'
nav=row(tx('page_prev','‹',23,purple,width='48dp',height='36dp',extra='android:gravity="center" android:contentDescription="이전 후보 페이지"')+tx('page_label','1 / 1',11,muted,width='0dp',extra='android:layout_weight="1" android:gravity="center"')+tx('page_next','›',23,purple,width='48dp',height='36dp',extra='android:gravity="center" android:contentDescription="다음 후보 페이지"'))
footer=row(tx('updated','2시간 자동 조회',10,muted,width='0dp',extra='android:layout_weight="1" android:maxLines="2"')+tx('open_app','전체·근거',11,purple,True,width='65dp',height='40dp',extra='android:gravity="center"')+tx('refresh','↻ 새로고침',11,'#FFFFFF',True,width='83dp',height='40dp',extra='android:gravity="center" android:background="@drawable/primary_button"'))
xml='<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/panel" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:padding="14dp" android:background="@drawable/panel_new">'+header+btc+dom+summary+tabs+body+nav+tx('warning','점수 ≠ 상승 확률',9,muted,extra='android:maxLines="1" android:ellipsize="end"')+footer+'</LinearLayout>'
(r/'layout/widget_v110.xml').write_text(xml)
coin=row(tx('row_name','코인명 (티커)',12,bold=True,width='0dp',extra='android:layout_weight="1" android:maxLines="1" android:ellipsize="end"')+tx('row_price','—',11,width='wrap_content'))+tx('row_status','조건 확인',10,muted,extra='android:layout_marginTop="3dp" android:maxLines="1" android:ellipsize="end"')
(r/'layout/coin_row.xml').write_text('<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/row_root" android:layout_width="match_parent" android:layout_height="40dp" android:orientation="vertical" android:gravity="center_vertical" android:paddingLeft="2dp" android:paddingRight="2dp">'+coin+'</LinearLayout>')
(r/'layout/coin_empty.xml').write_text('<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent" android:layout_height="70dp" android:gravity="center" android:orientation="vertical">'+tx('empty_label','조회 전',12,muted,extra='android:gravity="center"')+'</LinearLayout>')
for name,color,stroke,radius in [('panel_new','#FFFFFF','#E5E7F0',22),('soft_card','#F3F1FB',None,12),('tab_selected','#EBE7FC',None,9),('tab_idle','#F7F8FB',None,9),('primary_button','#6B55D4',None,12)]:
 (r/f'drawable/{name}.xml').write_text(f'<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="{color}"/><corners android:radius="{radius}dp"/>'+ (f'<stroke android:width="1dp" android:color="{stroke}"/>' if stroke else '')+'</shape>')
edit(r/'xml/market_widget_info.xml','@layout/widget_market','@layout/widget_v110')
p=r/'xml/market_widget_info.xml';p.write_text(p.read_text().replace('android:minHeight="520dp"','android:minHeight="600dp"').replace('android:minHeight="590dp"','android:minHeight="600dp"'))
edit(r/'xml/market_widget_info.xml','android:minResizeHeight="340dp"','android:minResizeHeight="380dp"')
print('Applied v1.1.0 source and design migration')
