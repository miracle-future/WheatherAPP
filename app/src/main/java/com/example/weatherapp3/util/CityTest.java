package com.example.weatherapp3.util;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class CityTest {
    private final static String TAG = "CityTest";
    public static void getProvince() {
        String url = Constant.BASE_URL;
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(TAG, "getProvince: exception " + e.toString());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d(TAG, "getProvince: success=" + response.body().string());
            }
        });
    }

    public static void getCity(int provinceId) {
        String url = Constant.BASE_URL + "/" + provinceId;
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(TAG, "getCity: exception " + e.toString());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d(TAG, "getCity: success=" + response.body().string());
            }
        });
    }

    public static void getCounty(int provinceId, int cityId) {
        String url = Constant.BASE_URL + "/" + provinceId + "/" + cityId;
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(TAG, "getCounty: exception " + e.toString());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d(TAG, "getCounty: success=" + response.body().string());
            }
        });
    }
}
