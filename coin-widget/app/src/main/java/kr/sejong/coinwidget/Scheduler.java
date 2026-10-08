package kr.sejong.coinwidget;

import android.app.job.*;
import android.content.*;
import android.os.Build;

final class Scheduler {
    static final int PERIODIC=8201,ONCE=8202;
    static void ensure(Context c){
        JobScheduler j=c.getSystemService(JobScheduler.class);
        if(!c.getSharedPreferences("settings",0).getBoolean("auto",true)||MarketWidget.ids(c).length==0){j.cancel(PERIODIC);return;}
        if(j.getPendingJob(PERIODIC)!=null)return;
        JobInfo info=new JobInfo.Builder(PERIODIC,new ComponentName(c,MarketJobService.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true)
            .setPeriodic(7_200_000L,600_000L).setBackoffCriteria(60_000L,JobInfo.BACKOFF_POLICY_EXPONENTIAL).build();
        if(j.schedule(info)!=JobScheduler.RESULT_SUCCESS)Repository.recordFailure(c,"자동 갱신 예약 실패: 앱 설정 확인 필요");
    }
    static void request(Context c){
        JobScheduler j=c.getSystemService(JobScheduler.class);
        JobInfo.Builder b=new JobInfo.Builder(ONCE,new ComponentName(c,MarketJobService.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        if(Build.VERSION.SDK_INT>=31){try{if(j.schedule(b.setExpedited(true).build())==JobScheduler.RESULT_SUCCESS)return;}catch(RuntimeException ignored){} }
        j.schedule(new JobInfo.Builder(ONCE,new ComponentName(c,MarketJobService.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).build());
    }
    static void cancel(Context c){JobScheduler j=c.getSystemService(JobScheduler.class);j.cancel(PERIODIC);j.cancel(ONCE);}
}
