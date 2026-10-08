package kr.sejong.coinwidget;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Optional CoinGecko Demo key only. No Upbit access/secret keys are collected. */
final class SecureStore {
    private static final String ALIAS="CoinWidgetDemoKeyV1";
    private static SecretKey key()throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(ALIAS))return (SecretKey)ks.getKey(ALIAS,null);
        KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return gen.generateKey();
    }
    static boolean has(Context c){return c.getSharedPreferences("secure",0).contains("cipher");}
    static void save(Context c,String text)throws Exception{
        if(!text.matches("[A-Za-z0-9_-]{5,180}"))throw new IllegalArgumentException("CoinGecko Demo API 키 형식을 확인하세요.");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        String iv=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP);
        String data=Base64.encodeToString(cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
        if(!c.getSharedPreferences("secure",0).edit().putString("iv",iv).putString("cipher",data).commit())throw new IllegalStateException("키 저장 실패");
    }
    static String read(Context c)throws Exception{
        if(!has(c))return "";
        String iv=c.getSharedPreferences("secure",0).getString("iv","");
        String data=c.getSharedPreferences("secure",0).getString("cipher","");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(data,Base64.NO_WRAP)),StandardCharsets.UTF_8);
    }
    static void clear(Context c){c.getSharedPreferences("secure",0).edit().clear().apply();}
}
