"""Final height budget adjustment; checks every target size before asserting."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
p=r/'app/src/main/java/kr/sejong/coinwidget/Dashboard.java'
s=p.read_text().replace('height>=750?5:height>=680?4:height>=540?3:height>=430?2:1','height>=750?5:height>=700?4:height>=600?3:height>=520?2:1').replace('height<540?30:72','height<540?30:height<640?48:72')
p.write_text(s)
p=r/'app/src/androidTest/java/kr/sejong/coinwidget/UpgradeTest.java'
s=p.read_text().replace('float den=c.getResources().getDisplayMetrics().density;','float den=c.getResources().getDisplayMetrics().density;StringBuilder boundsErrors=new StringBuilder();')
s=s.replace('assertTrue("Footer clipped at "+h+"dp: "+(bottom/den),bottom<=h*den);','if(bottom>h*den || button.getHeight()<36*den)boundsErrors.append("height=").append(h).append(" bottom=").append(bottom/den).append(" buttonHeight=").append(button.getHeight()/den).append("; ");')
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
p.write_text(s)
print('Adaptive 1-5 row capacity fitted to actual measured layout; retained strict footer tests')
