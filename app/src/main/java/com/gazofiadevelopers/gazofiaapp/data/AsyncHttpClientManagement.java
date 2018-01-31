package com.gazofiadevelopers.gazofiaapp.data;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class AsyncHttpClientManagement {

    private static AsyncHttpClient client = new com.loopj.android.http.AsyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(url, params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(url, params, responseHandler);
    }

    public static void put(String url, RequestParams params, AsyncHttpResponseHandler responseHandler){
        client.put(url, params, responseHandler);
    }

    public static void delete(String url, RequestParams params, AsyncHttpResponseHandler responseHandler){
        client.delete(url, params, responseHandler);
    }

}
