package kr.sejong.coinwidget;

import android.app.job.*;
import android.os.*;
import java.util.concurrent.*;

public final class MarketJobService extends JobService {
    private final ExecutorService executor=Executors.newCachedThreadPool();
    private final ConcurrentHashMap<Integer,Run> runs=new ConcurrentHashMap<>();
    private static final class Run {volatile Future<?>future;}
    @Override public boolean onStartJob(JobParameters params){
        if(Repository.RUNNING.get())return false;
        Run run=new Run();runs.put(params.getJobId(),run);
        run.future=executor.submit(()->{
            boolean retry=false;
            try{retry=!Repository.refresh(getApplicationContext());}
            catch(Exception e){if(!Thread.currentThread().isInterrupted()){Repository.recordFailure(this,"자동 조회 처리 실패: 다음 연결에서 재시도");retry=true;}}
            final boolean again=retry;
            new Handler(Looper.getMainLooper()).post(()->{if(runs.remove(params.getJobId(),run))jobFinished(params,again);});
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters params){Run r=runs.remove(params.getJobId());if(r!=null&&r.future!=null)r.future.cancel(true);return true;}
    @Override public void onDestroy(){for(Run r:runs.values())if(r.future!=null)r.future.cancel(true);runs.clear();executor.shutdownNow();super.onDestroy();}
}
