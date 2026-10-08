"""Post-migration fixes and resource normalization; no lint suppression."""
from pathlib import Path
import re,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1];j=root/'app/src/main/java/kr/sejong/coinwidget';res=root/'app/src/main/res'
def patch(file,a,b):
 p=j/file;s=p.read_text();p.write_text(s.replace(a,b))
patch('MiniCharts.java','Typeface.create("sans-serif",0)','Typeface.create("sans-serif",Typeface.NORMAL)')
patch('MiniCharts.java','new Paint(3)','new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG)')
patch('WatchDetails.java','card.setOrientation(1)','card.setOrientation(LinearLayout.VERTICAL)')
patch('Charts.java','new DecimalFormat(v>=100?"#,##0":v>=1?"#,##0.00":"0.####")','new DecimalFormat("#,##0.########")')
patch('MainActivity.java','card.findViewById(R.id.open_app).setOnClickListener(v->SettingsUi.show(this));','card.findViewById(R.id.open_app).setOnClickListener(v->scroll.smoothScrollTo(0,card.getBottom()+dp(10)));')
patch('WatchDetails.java','if(mode==1)out.addView(a.text(', 'if(System.currentTimeMillis()-m.optLong("quote_at")>900000)out.addView(a.text("시세가 15분 이상 지났습니다. 매수 검토 전 새로고침하세요.",14,true));\n  if(mode==1)out.addView(a.text(')
# Remove superseded, unreachable UI and old preview-only resources.
for p in [j/'DetailUi.java',res/'layout/widget_market.xml',res/'drawable/panel.xml',res/'drawable/button_bg.xml']:
 if p.exists():p.unlink()
# Store visible labels in string resources instead of hard-coded XML.
ET.register_namespace('android','http://schemas.android.com/apk/res/android')
ns='{http://schemas.android.com/apk/res/android}';strings=ET.Element('resources')
for p in sorted((res/'layout').glob('*.xml')):
 tree=ET.parse(p)
 for index,e in enumerate(tree.iter()):
  for key in ['text','contentDescription']:
   val=e.get(ns+key)
   if val and not val.startswith('@'):
    name=f'ui_{p.stem}_{index}_{key.lower()}';s=ET.SubElement(strings,'string',{'name':name,'formatted':'false'});s.text=val;e.set(ns+key,'@string/'+name)
 tree.write(p,encoding='unicode')
ET.ElementTree(strings).write(res/'values/ui_strings.xml',encoding='unicode')
print('Polished constants, precise prices, details navigation, and localized resources')
