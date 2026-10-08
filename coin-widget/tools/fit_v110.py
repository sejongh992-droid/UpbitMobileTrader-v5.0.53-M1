"""Final measured layout and foreground widget-navigation adjustments."""
from pathlib import Path
import xml.etree.ElementTree as ET
r=Path(__file__).resolve().parents[1]
p=r/'app/src/main/java/kr/sejong/coinwidget/Dashboard.java'
s=p.read_text().replace('height>=750?5:height>=680?4:height>=540?3:height>=430?2:1','height>=750?5:height>=700?4:height>=600?3:height>=520?2:1').replace('height<540?30:72','height<540?30:height<640?48:72')
s=s.replace('.setAction(NAV).setData(','.setAction(NAV).addFlags(Intent.FLAG_RECEIVER_FOREGROUND).setData(')
p.write_text(s)
# Reserve the full footer height. A wrap_content row can otherwise be measured
# shorter than its 40dp button and clip the rounded background at the edges.
ET.register_namespace('android','http://schemas.android.com/apk/res/android');ns='{http://schemas.android.com/apk/res/android}'
p=r/'app/src/main/res/layout/widget_v110.xml';tree=ET.parse(p);root=tree.getroot();root.set(ns+'padding','12dp');root[-1].set(ns+'layout_height','42dp')
for e in root.iter():
 margin=e.get(ns+'layout_marginTop');mapping={'10dp':'8dp','9dp':'7dp','6dp':'5dp','5dp':'4dp','4dp':'3dp'}
 if margin in mapping:e.set(ns+'layout_marginTop',mapping[margin])
 if e.get(ns+'padding')=='9dp':e.set(ns+'padding','8dp')
tree.write(p,encoding='unicode')
p=r/'app/src/androidTest/java/kr/sejong/coinwidget/UpgradeTest.java'
s=p.read_text().replace('float den=c.getResources().getDisplayMetrics().density;','float den=c.getResources().getDisplayMetrics().density;StringBuilder boundsErrors=new StringBuilder();')
s=s.replace('assertTrue("Footer clipped at "+h+"dp: "+(bottom/den),bottom<=h*den);','if(bottom>h*den-v.getPaddingBottom() || button.getHeight()<36*den || ((View)button.getParent()).getHeight()<40*den)boundsErrors.append("height=").append(h).append(" bottom=").append(bottom/den).append(" buttonHeight=").append(button.getHeight()/den).append("; ");')
anchor='''   }
  });
 }
}'''
assert anchor in s
s=s.replace(anchor,'''   }
   assertTrue("Widget bounds: "+boundsErrors,boundsErrors.length()==0);
  });
 }
}''')
# PendingIntent.send schedules cross-process delivery. Wait for the actual
# state change with an 8-second ceiling rather than assuming 300ms is enough.
s=s.replace('InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(300);','long navStart=SystemClock.elapsedRealtime();while(Dashboard.mode(c,818)!=1&&SystemClock.elapsedRealtime()-navStart<8000)SystemClock.sleep(50);\n  InstrumentationRegistry.getInstrumentation().waitForIdleSync();android.util.Log.i("WidgetNav","deliveryMs="+(SystemClock.elapsedRealtime()-navStart));')
p.write_text(s)
print('Fitted 1-5 rows; explicit 42dp footer; strict bounds; foreground navigation with bounded delivery test')
