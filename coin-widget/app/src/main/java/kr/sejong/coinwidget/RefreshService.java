package kr.sejong.coinwidget;

import android.app.*;
import android.content.*;
import android.os.*;
import java.util.concurrent.*;

/** Only user taps start this short-lived foreground service; periodic work uses JobScheduler. */
public final class RefreshService extends Service {
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private Future<?>task;
    private final Handler main=new Handler(Looper.getMainLooper());
    @Override public IBinder onBind(Intent i){return null;}
    @Override public int onStartCommand(Intent i,int flags,int startId){
        NotificationManager nm=getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("refresh","시세 새로고침",NotificationManager.IMPORTANCE_LOW));
        Notification note=new Notification.Builder(this,"refresh").setSmallIcon(R.drawable.ic_refresh)
            .setContentTitle("코인 시장 자료 조회 중").setContentText("완료되면 이 알림은 사라집니다.")
            .setContentIntent(Renderer.activity(this,14,new Intent(this,MainActivity.class)))
            .setOngoing(true).setOnlyAlertOnce(true).build();
        startForeground(400,note);
        if(task!=null&&!task.isDone())return START_NOT_STICKY;
        if(Repository.RUNNING.get()){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
        task=executor.submit(()->{
            try{Repository.refresh(getApplicationContext());}
            catch(Exception e){if(!Thread.currentThread().isInterrupted())Repository.recordFailure(this,"수동 조회 실패: 네트워크와 설정을 확인하세요.");}
            finally{main.post(()->{stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();});}
        });
        return START_NOT_STICKY;
    }
    @Override public void onTimeout(int startId,int fgsType){
        if(task!=null)task.cancel(true);Repository.recordFailure(this,"새로고침이 시간 제한으로 중단되었습니다.");stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
    }
    @Override public void onDestroy(){if(task!=null)task.cancel(true);executor.shutdownNow();super.onDestroy();}
}
