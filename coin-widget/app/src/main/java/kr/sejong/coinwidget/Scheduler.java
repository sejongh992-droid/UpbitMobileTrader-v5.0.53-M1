package kr.sejong.coinwidget;
import android.app.job.*;
import android.content.*;
import android.os.*;
final class Scheduler {
 static final int PERIODIC=8201,ONCE=8202,MORNING=8203,EXPIRE=8204;
 static void ensure(Context c){
  JobScheduler j=c.getSystemService(JobScheduler.class);
  if(!c.getSharedPreferences("settings",0).getBoolean("auto",true)||MarketWidget.ids(c).length==0){j.cancel(PERIODIC);j.cancel(MORNING);return;}
  JobInfo old=j.getPendingJob(PERIODIC);
  if(old==null||old.getIntervalMillis()!=900_000L){
   JobInfo info=new JobInfo.Builder(PERIODIC,new ComponentName(c,MarketJobService.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setPeriodic(900_000L,300_000L).setBackoffCriteria(60_000L,JobInfo.BACKOFF_POLICY_EXPONENTIAL).build();
   if(j.schedule(info)!=JobScheduler.RESULT_SUCCESS)Repository.recordFailure(c,"자동 갱신 예약 실패: 앱 설정 확인 필요");
  }
  scheduleMorning(c);
 }
 static void scheduleMorning(Context c){
  JobScheduler j=c.getSystemService(JobScheduler.class);if(!c.getSharedPreferences("settings",0).getBoolean("auto",true)||MarketWidget.ids(c).length==0){j.cancel(MORNING);return;}
  long now=System.currentTimeMillis(),next=BeforeNine.nextCheck(now);JobInfo old=j.getPendingJob(MORNING);
  if(old!=null&&old.getExtras().getLong("at")==next)return;
  PersistableBundle extras=new PersistableBundle();extras.putLong("at",next);
  j.schedule(new JobInfo.Builder(MORNING,new ComponentName(c,MarketJobService.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setMinimumLatency(Math.max(0,next-now)).setPersisted(true).setExtras(extras).build());
 }
 static void expire(Context c,long quote,long nine){
  if(MarketWidget.ids(c).length==0)return;long due=Math.min(quote+300001,nine);
  c.getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(EXPIRE,new ComponentName(c,MarketJobService.class)).setMinimumLatency(Math.max(0,due-System.currentTimeMillis())).build());
 }
 static void request(Context c){
  JobScheduler j=c.getSystemService(JobScheduler.class);JobInfo.Builder b=new JobInfo.Builder(ONCE,new ComponentName(c,MarketJobService.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
  if(Build.VERSION.SDK_INT>=31)try{if(j.schedule(b.setExpedited(true).build())==JobScheduler.RESULT_SUCCESS)return;}catch(RuntimeException ignored){}
  j.schedule(new JobInfo.Builder(ONCE,new ComponentName(c,MarketJobService.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).build());
 }
 static void cancel(Context c){JobScheduler j=c.getSystemService(JobScheduler.class);for(int id:new int[]{PERIODIC,ONCE,MORNING,EXPIRE})j.cancel(id);}
}
