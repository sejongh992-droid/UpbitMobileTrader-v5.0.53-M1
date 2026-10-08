package kr.sejong.coinwidget;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.*;
import android.widget.*;
final class SettingsUi {
 static AlertDialog show(MainActivity a){
  a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  SharedPreferences pref=a.getSharedPreferences("settings",0);
  LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(a.dp(18),a.dp(8),a.dp(18),a.dp(8));
  CheckBox auto=new CheckBox(a);auto.setText("2시간 주기 자동 갱신");auto.setChecked(pref.getBoolean("auto",true));body.addView(auto);
  body.addView(a.text("업비트 현재가·일봉은 공개 API로 조회합니다. 업비트 Access Key / Secret Key를 입력하지 마세요.",13,false));
  RadioGroup sources=new RadioGroup(a);
  RadioButton paprika=new RadioButton(a);paprika.setId(View.generateViewId());paprika.setText("CoinPaprika 도미넌스 (기본·키 없음)");sources.addView(paprika);
  RadioButton gecko=new RadioButton(a);gecko.setId(View.generateViewId());gecko.setText("CoinGecko 도미넌스 (Demo 키 사용)");sources.addView(gecko);
  sources.check("CoinGecko".equals(pref.getString("dominance_source","CoinPaprika"))?gecko.getId():paprika.getId());body.addView(sources);
  EditText key=new EditText(a);key.setSingleLine();key.setTextSize(13);key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
  key.setHint(SecureStore.has(a)?"Demo 키 저장됨 · 변경할 때만 입력":"CoinGecko Demo API 키 (선택)");key.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);body.addView(key);
  body.addView(a.text("키는 기기 내 암호화 저장됩니다. 입력한 값은 CoinGecko에만 전송됩니다. 공급원 변경 시 도미넌스 기록은 새로 수집합니다. Pro 키·업비트 키는 사용하지 마세요.",12,false));
  ScrollView wrap=new ScrollView(a);wrap.addView(body);
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle("API / 갱신 설정").setView(wrap).setPositiveButton("저장 후 조회",null).setNegativeButton("닫기",null).setNeutralButton("저장 키 삭제",null).create();
  dialog.setOnDismissListener(d->a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE));
  dialog.setOnShowListener(d->{
   dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
   dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
    try{
     String entered=key.getText().toString().trim();boolean cg=sources.getCheckedRadioButtonId()==gecko.getId();
     if(!entered.isEmpty())SecureStore.save(a,entered);if(cg&&!SecureStore.has(a)){key.setError("CoinGecko Demo 키가 필요합니다.");return;}
     pref.edit().putBoolean("auto",auto.isChecked()).putString("dominance_source",cg?"CoinGecko":"CoinPaprika").apply();
     Scheduler.ensure(a);MarketWidget.renderAll(a);dialog.dismiss();a.draw();a.refresh();
    }catch(Exception e){key.setError("키 형식 또는 암호화 저장을 확인하세요.");}
   });
   dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{SecureStore.clear(a);pref.edit().putString("dominance_source","CoinPaprika").apply();dialog.dismiss();a.toast("저장 키를 삭제하고 기본 공급원으로 변경했습니다.");a.draw();a.refresh();});
  });dialog.show();return dialog;
 }
}
