package kr.sejong.coinwidget;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;

/** HTTPS GET only, fixed upstream hosts, bounded time and response size. */
final class Net {
    private static final Set<String> HOSTS=new HashSet<>(Arrays.asList(
        "api.upbit.com","api.coinpaprika.com","api.coingecko.com","api.frankfurter.dev"));
    private final long deadline=System.nanoTime()+120_000_000_000L;
    private long lastUpbit;
    String get(String address) throws IOException {return get(address,null);}
    String get(String address,String demoKey) throws IOException {
        check();
        URL url=new URL(address);
        if(!"https".equals(url.getProtocol())||!HOSTS.contains(url.getHost()))throw new IOException("허용되지 않은 데이터 주소");
        if(demoKey!=null&&!url.getHost().equals("api.coingecko.com"))throw new IOException("API 키 전송 대상 오류");
        if(url.getHost().equals("api.upbit.com")){
            long wait=170-(System.nanoTime()-lastUpbit)/1_000_000L;
            if(wait>0)try{Thread.sleep(wait);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new InterruptedIOException("조회 취소");}
            lastUpbit=System.nanoTime();
        }
        int timeout=(int)Math.min(10_000,Math.max(1,(deadline-System.nanoTime())/1_000_000));
        HttpsURLConnection conn=(HttpsURLConnection)url.openConnection();
        conn.setConnectTimeout(timeout);conn.setReadTimeout(timeout);conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(false);conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept","application/json");
        conn.setRequestProperty("Accept-Encoding","gzip");
        conn.setRequestProperty("User-Agent","CoinMarketWidget/1.2.1 (Android; read-only)");
        if(demoKey!=null&&!demoKey.isEmpty())conn.setRequestProperty("x-cg-demo-api-key",demoKey);
        try {
            int code=conn.getResponseCode();
            if(code!=200)throw new IOException("HTTP "+code+(code==429?" (호출 제한: 잠시 후 재조회)":""));
            InputStream raw=conn.getInputStream();
            try(InputStream in="gzip".equalsIgnoreCase(conn.getContentEncoding())?new GZIPInputStream(raw):raw;
                ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[]buf=new byte[8192];int n;
                while((n=in.read(buf))!=-1){check();out.write(buf,0,n);if(out.size()>5_000_000)throw new IOException("응답 용량 제한 초과");}
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } finally {conn.disconnect();}
    }
    private void check()throws IOException{
        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("조회 취소");
        if(System.nanoTime()>deadline)throw new IOException("전체 조회 시간 제한 초과");
    }
}
