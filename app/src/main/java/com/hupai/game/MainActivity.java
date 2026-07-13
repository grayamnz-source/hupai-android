package com.hupai.game;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private WebView webView;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#0d2818"));
        window.setNavigationBarColor(Color.parseColor("#0d2818"));

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setGeolocationEnabled(true);

        WebView.setWebContentsDebuggingEnabled(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(android.webkit.WebView view, String url, String message, final JsResult result) {
                new android.app.AlertDialog.Builder(view.getContext())
                        .setMessage(message)
                        .setPositiveButton("确定", (d, w) -> result.confirm())
                        .setNegativeButton("取消", (d, w) -> result.cancel())
                        .setOnCancelListener(d -> result.cancel())
                        .show();
                return true;
            }
            @Override
            public boolean onJsAlert(android.webkit.WebView view, String url, String message, final JsResult result) {
                new android.app.AlertDialog.Builder(view.getContext())
                        .setMessage(message)
                        .setPositiveButton("确定", (d, w) -> result.confirm())
                        .setOnCancelListener(d -> result.cancel())
                        .show();
                return true;
            }
        });
        webView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        webView.setBackgroundColor(Color.parseColor("#0d2818"));

        webView.addJavascriptInterface(new AndroidInterface(), "Android");
        webView.loadUrl("file:///android_asset/game.html");
    }

    public class AndroidInterface {
        @android.webkit.JavascriptInterface
        public void finishActivity() {
            finish();
        }

        @android.webkit.JavascriptInterface
        public void httpRequest(String requestId, String method, String urlStr, String headersJson, String bodyStr) {
            executor.execute(() -> {
                try {
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(20000);
                    conn.setDoInput(true);

                    JSONObject headers = new JSONObject(headersJson);
                    Iterator<String> keys = headers.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        conn.setRequestProperty(key, headers.getString(key));
                    }

                    if (bodyStr != null && !bodyStr.isEmpty() && (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT"))) {
                        conn.setDoOutput(true);
                        try (OutputStream os = conn.getOutputStream()) {
                            byte[] input = bodyStr.getBytes(StandardCharsets.UTF_8);
                            os.write(input, 0, input.length);
                        }
                    }

                    int status = conn.getResponseCode();
                    BufferedReader reader;
                    if (status >= 200 && status < 300) {
                        reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    } else {
                        reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                    }

                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject result = new JSONObject();
                    result.put("ok", status >= 200 && status < 300);
                    result.put("status", status);
                    result.put("body", response.toString());

                    runOnUiThread(() -> {
                        String jsonStr = result.toString();
                        String js = "androidHttpCallback('" + requestId + "', '" + jsonStr.replace("'", "\\'") + "')";
                        webView.loadUrl("javascript:" + js);
                    });
                } catch (Exception e) {
                    try {
                        JSONObject result = new JSONObject();
                        result.put("ok", false);
                        result.put("status", 0);
                        result.put("error", e.getMessage());

                        runOnUiThread(() -> {
                            String jsonStr = result.toString();
                            String js = "androidHttpCallback('" + requestId + "', '" + jsonStr.replace("'", "\\'") + "')";
                            webView.loadUrl("javascript:" + js);
                        });
                    } catch (Exception ignored) {
                    }
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
