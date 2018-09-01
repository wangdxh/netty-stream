package com.wangdxh.ssl;

// C:\Users\Administrator>keytool -genkey -keysize 2048 -validity 365 -keyalg RSA -dname "CN=localhost" -keypass hsc123
// -storepass hsc123 -keystore c:\local.jks

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.security.KeyStore;

public class SSLContextFactory
{
    public static SSLContext getSslContext() throws Exception
    {
        char[] passArray = "hsc123".toCharArray();
        SSLContext sslContext = SSLContext.getInstance("TLSv1");

        KeyStore ks = KeyStore.getInstance("JKS");
        //加载keytool 生成的文件
        FileInputStream inputStream = new FileInputStream("c:\\local.jks");
        ks.load(inputStream, passArray);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, passArray);
        sslContext.init(kmf.getKeyManagers(), null, null);
        inputStream.close();
        return sslContext;
    }
}
