package com.zeroX;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import javax.net.ssl.*;

public class Main extends Activity {

    private static final int RESULT_SETTINGS = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String action = intent.getAction();
        if (savedInstanceState == null) {
            // if this is from the share menu
            if (Intent.ACTION_SEND.equals(action)) {
                if (extras.containsKey(Intent.EXTRA_STREAM)) {
                    try {
                        // Get resource path from intent callee
                        Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);

                        new SendRequest().execute(uri);
                        Log.i("uploadFile", uri.toString());
                        return;
                    } catch (Exception e) {
                        Log.e("uploadFile", e.toString());
                    }

                } else if (extras.containsKey(Intent.EXTRA_TEXT)) {
                    return;
                }
            }
        }
    }

    private class SendRequest extends AsyncTask<Uri, Integer, Integer> {

        private final ProgressDialog dialog = new ProgressDialog(Main.this);
        int serverResponseCode = 0;

        /**
         * *******  File Path ************
         */
        final String uploadFilePath = "/mnt/sdcard/";
        final String uploadFileName = "service_lifecycle.png";

        protected void onPreExecute() {
            this.dialog.setMessage("Uploading photo...");
            this.dialog.show();
        }

        public String getRealPathFromURI(Uri[] contentUri) {

            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = managedQuery(contentUri[0], proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }

        @Override
        protected Integer doInBackground(Uri... sourceFileUri) {
            String fileName = getRealPathFromURI(sourceFileUri);

            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            File sourceFile = new File(fileName);

            if (!sourceFile.isFile()) {

                dialog.dismiss();

                Log.e("uploadFile", "Source File not exist :"
                        + uploadFilePath + "" + uploadFileName);

                return 0;

            } else {
                try {
                    SharedPreferences sharedPrefs = PreferenceManager
                            .getDefaultSharedPreferences(Main.this);

                    String server = sharedPrefs.getString("prefServer", null);
                    String username = sharedPrefs.getString("prefUsername", null);
                    String password = sharedPrefs.getString("prefPassword", null);
                    if ((null != server && "" != server) && (null != username && "" != username) && (null != password && "" != password)) {
                        String upLoadServerUri = "https://" + server + "/images";

                        // open a URL connection to the Servlet
                        FileInputStream fileInputStream = new FileInputStream(sourceFile);
                        URL url = new URL(upLoadServerUri);
                        trustEveryone();
                        // Open a HTTP  connection to  the URL
                        conn = (HttpsURLConnection) url.openConnection();
                        conn.setDoInput(true); // Allow Inputs
                        conn.setDoOutput(true); // Allow Outputs
                        conn.setUseCaches(false); // Don't use a Cached Copy
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Connection", "Keep-Alive");
                        conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                        conn.setRequestProperty("uploaded_file", fileName);
                        String userPassword = username+":"+password;
                        String encoding = Base64.encodeToString(userPassword.getBytes("UTF-8"), Base64.DEFAULT);
                        conn.setRequestProperty("Authorization", "Basic " + encoding);

                        dos = new DataOutputStream(conn.getOutputStream());

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                                + fileName + "\"" + lineEnd);

                        dos.writeBytes(lineEnd);

                        // create a buffer of  maximum size
                        bytesAvailable = fileInputStream.available();

                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];

                        // read file and write it into form...
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                        while (bytesRead > 0) {

                            dos.write(buffer, 0, bufferSize);
                            bytesAvailable = fileInputStream.available();
                            bufferSize = Math.min(bytesAvailable, maxBufferSize);
                            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                        }

                        // send multipart form data necesssary after file data...
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                        // Responses from the server (code and message)
                        serverResponseCode = conn.getResponseCode();
                        String serverResponseMessage = conn.getResponseMessage();

                        Log.i("uploadFile", "HTTP Response is : "
                                + serverResponseMessage + ": " + serverResponseCode);

                        if (serverResponseCode == 202) {

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(Main.this, "File Upload Complete.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        //close the streams //
                        fileInputStream.close();
                        dos.flush();
                        dos.close();
                    } else {
                        dialog.dismiss();

                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(Main.this, "Server settings are not defined, go to settings...",
                                        Toast.LENGTH_LONG).show();
                            }
                        });

                        Log.e("uploadFile", "error: ");
                    }
                } catch (MalformedURLException ex) {

                    dialog.dismiss();
                    ex.printStackTrace();

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(Main.this, "MalformedURLException",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("uploadFile", "error: " + ex.getMessage(), ex);
                } catch (Exception e) {

                    dialog.dismiss();
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(Main.this, "Got Exception : see logcat ",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("uploadFile", "Exception : "
                            + e.getMessage(), e);
                }
                dialog.dismiss();
                return serverResponseCode;

            } // End else block
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            // Update percentage
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            Log.i("uploadFile", "HTTP Response is : "
                    + result + ": " + serverResponseCode);

            this.dialog.dismiss();
            Toast.makeText(Main.this, result.toString(),
                    Toast.LENGTH_LONG).show();
        }

    }

    private void trustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                    context.getSocketFactory());
        } catch (Exception e) { // should never happen
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_settings:
                Intent i = new Intent(this, Settings.class);
                startActivityForResult(i, RESULT_SETTINGS);
                break;

        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SETTINGS:
                confirmSettings();
                break;

        }

    }

    private void confirmSettings() {
        Toast.makeText(Main.this, "Settings saved",
                Toast.LENGTH_LONG).show();
    }

}