package com.example.imagesender.utils.network;

import static android.service.controls.ControlsProviderService.TAG;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.imagesender.enums.ServiceEnum;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpsServerUtil {

    private Context context;

    public HttpsServerUtil(Context context) {
        this.context = context;
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    OkHttpClient client = new OkHttpClient();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public void connectToHTTPSServer() {
        executorService.submit(() -> {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(ServiceEnum.HTTPS_SERVER.value);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                handler.post(() -> Toast.makeText(context, "Connected successfully", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> Toast.makeText(context, "Failed to connect", Toast.LENGTH_SHORT).show());
            } finally {
                if (urlConnection != null) {
                    Log.d(TAG, "Release connection");
                    urlConnection.disconnect();
                }
            }
        });
    }


    public String sendBase64ToServer(String base64Image) {
        String cmd = "dither";
        Map<String, Object> data = new HashMap<>();
        data.put(".command", cmd);
        data.put(".msgid", "1");
        data.put("key", "01Az8nB8mB4cCV");
        data.put("data", base64Image);
        Future<String> futureString = executorService.submit(() -> {
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(data);
            RequestBody body = RequestBody.create(jsonPayload, JSON);
            Request request = new Request.Builder()
                    .url(ServiceEnum.HTTPS_SERVER.value)
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            return response.body().string();
        });
        return getResultFromFutreString(futureString);

    }

    public boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    private String getResultFromFutreString(Future<String> future) {
        String result = null;
        try {
            result = future.get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error getting result from server");
        }
        return result;
    }

}
