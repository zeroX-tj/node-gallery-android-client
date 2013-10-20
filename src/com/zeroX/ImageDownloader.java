package com.zeroX;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Base64;
import android.util.Log;
import com.nostra13.universalimageloader.core.assist.FlushedInputStream;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import javax.net.ssl.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class ImageDownloader extends BaseImageDownloader implements com.nostra13.universalimageloader.core.download.ImageDownloader {
    public static final String TAG = ImageDownloader.class.getName();

    private int connectTimeout;
    private int readTimeout;


    // always verify the host - dont check for certificate
    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    public ImageDownloader(Context context) {
        super(context);
    }

    public static String encodeCredentials(SharedPreferences sharedPrefs) {
        try {
            //Configuration configuration = Configuration.getInstance();
            String auth = Base64.encodeToString((sharedPrefs.getString("prefUsername", null)+":"+sharedPrefs.getString("prefPassword", null)).getBytes("UTF-8"), Base64.DEFAULT);

            return auth;
        } catch (Exception ignored) {
            Log.e(TAG, ignored.getMessage(), ignored);
        }
        return "";
    }

    /**
     * Trust every server - dont check for any certificate
     */
    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] x509Certificates,
                    String s) throws java.security.cert.CertificateException {
            }

            @Override
            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] x509Certificates,
                    String s) throws java.security.cert.CertificateException {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }
        } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public InputStream getStreamFromNetwork(String imageUri, Object extra) throws IOException {
        URL url = null;
        try {
            url = new URL(imageUri);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        HttpURLConnection http = null;

        String credentials = encodeCredentials((SharedPreferences) extra);
        if (url.getProtocol().toLowerCase().equals("https")) {
            trustAllHosts();
            HttpsURLConnection https = (HttpsURLConnection) url
                    .openConnection();
            https.setHostnameVerifier(DO_NOT_VERIFY);
            https.setRequestProperty("Authorization", "Basic "
                    + credentials);
            http = https;
            http.connect();
        } else {
            http = (HttpURLConnection) url.openConnection();
        }
        // URLConnection conn = imageUri.toURL().openConnection();
        http.setConnectTimeout(connectTimeout);
        http.setReadTimeout(readTimeout);
        return new FlushedInputStream(new BufferedInputStream(
                http.getInputStream()));
    }
}