package com.zeroX;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import android.util.Base64;
import org.apache.http.client.ClientProtocolException;

import javax.net.ssl.HttpsURLConnection;

public class ImageParser {

    public String readFeed(String server, String user, String password) {
        String uri = "https://" + server + "/images";
        try {
            URL url = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            String userPassword = user + ":" + password;
            String encoding = Base64.encodeToString(userPassword.getBytes("UTF-8"), Base64.DEFAULT);
            con.setRequestProperty("Authorization", "Basic " + encoding);
            return read_response(con);

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String read_response(HttpsURLConnection con) {
        String body = "";
        if (con != null) {

            try {
                BufferedReader br =
                        new BufferedReader(
                                new InputStreamReader(con.getInputStream()));

                String input;

                while ((input = br.readLine()) != null) {
                    body += input;
                }
                br.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return body;
    }
}