package com.example.irapid_gateway;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GetAPIData extends AsyncTask<Void, Void, String> {
    private String apiURL;
//    private String accessToken;

    public GetAPIData(String apiURL) {
        this.apiURL = apiURL;
//        this.accessToken = accessToken;
    }

    @Override
    protected String doInBackground(Void... voids) {
        String bodyData = "";
        try {
            URL url = new URL(apiURL);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

//            connection.setRequestProperty("Authorization", "Bearer " + accessToken);

            InputStream inputStream = connection.getInputStream();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            String responseJson = stringBuilder.toString();

            JSONObject jsonObject = new JSONObject(responseJson);
            JSONArray bodyArray = jsonObject.getJSONArray("body");
            for (int i = 0; i < bodyArray.length(); i++) {
                bodyData = bodyArray.getString(i);
                Log.i("api: ", bodyData);
            }
            inputStream.close();
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bodyData;
    }

    @Override
    protected void onPostExecute(String bodyData) {
    }

}
