package com.arabacheck.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    // Uygulama dosyaları bu sanal https adresinden sunulur (Android'in bu iş için ayırdığı alan adı).
    // Böylece yazı tanıma (Web Worker + WebAssembly) dosya erişimi kısıtlarına takılmaz.
    private static final String HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://" + HOST + "/index.html?app=1";
    private static final int PICK_IMAGES = 41;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    private static String mime(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".wasm")) return "application/wasm";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    /** Sayfadaki "Özeti kopyala" düğmesi için panoya yazma köprüsü. */
    public class Bridge {
        @JavascriptInterface
        public void copy(String text) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("ArabaCheck", text));
        }

        /** Rapor özetini Android paylaşım menüsüyle gönder (WhatsApp, SMS…). */
        @JavascriptInterface
        public void share(String text) {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(i, "Raporu paylaş"));
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#F3F5FA"));

        // Üstte lacivert (uygulama çubuğuyla aynı), altta beyaz (sekme çubuğuyla aynı) sistem çubuğu alanı.
        final View topBar = new View(this);
        topBar.setBackgroundColor(Color.parseColor("#081530"));
        final View bottomBar = new View(this);
        bottomBar.setBackgroundColor(Color.WHITE);
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#081530"));
        root.addView(topBar, new android.widget.LinearLayout.LayoutParams(-1, 0));
        root.addView(web, new android.widget.LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(bottomBar, new android.widget.LinearLayout.LayoutParams(-1, 0));
        setContentView(root);
        getWindow().setStatusBarColor(Color.parseColor("#081530"));
        getWindow().setNavigationBarColor(Color.WHITE);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) c.setSystemBarsAppearance(
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                            | android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        } else if (android.os.Build.VERSION.SDK_INT >= 26) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        // Android 15 kenardan kenara çizim: içeriği durum/gezinme çubuklarının altına sokma.
        root.setOnApplyWindowInsetsListener((View v, WindowInsets insets) -> {
            int top, bottom, left, right;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                top = i.top; bottom = i.bottom; left = i.left; right = i.right;
            } else {
                top = insets.getSystemWindowInsetTop(); bottom = insets.getSystemWindowInsetBottom();
                left = insets.getSystemWindowInsetLeft(); right = insets.getSystemWindowInsetRight();
            }
            v.setPadding(left, 0, right, 0);
            topBar.getLayoutParams().height = top;
            bottomBar.getLayoutParams().height = bottom;
            topBar.requestLayout();
            bottomBar.requestLayout();
            return insets;
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setTextZoom(100);
        web.addJavascriptInterface(new Bridge(), "ArabaCheckApp");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                String scheme = u.getScheme();
                if (HOST.equals(u.getHost())) return false;
                openInBrowser(u);
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (!HOST.equals(u.getHost())) return null;
                String path = u.getPath();
                if (path == null || path.equals("/")) path = "/index.html";
                try {
                    WebResourceResponse r = new WebResourceResponse(mime(path), null, getAssets().open(path.substring(1)));
                    java.util.Map<String, String> h = new java.util.HashMap<>();
                    h.put("Access-Control-Allow-Origin", "*");
                    r.setResponseHeaders(h);
                    return r;
                } catch (java.io.IOException e) {
                    return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found", null, null);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(
                    "(function(){try{var w=function(t){ArabaCheckApp.copy(String(t));return Promise.resolve();};" +
                    "if(navigator.clipboard){navigator.clipboard.writeText=w;}" +
                    "else{Object.defineProperty(navigator,'clipboard',{value:{writeText:w}});}}catch(e){}})();",
                    null);
            }
        });

        // "Ekran görüntüsünden doldur": galeriden bir veya birden çok resim seçtir.
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                        params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                try {
                    startActivityForResult(Intent.createChooser(i, "Ekran görüntüsü seçin"), PICK_IMAGES);
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl(START_URL);
    }

    /**
     * İlan linklerini sahibinden/arabam uygulamasında değil, telefonun tarayıcısında aç:
     * bu uygulamalar linki yakalayıp filtreleri (model, yıl, km, fiyat) yok sayıyor ve ana sayfayı açıyor.
     */
    private void openInBrowser(Uri u) {
        Intent i = new Intent(Intent.ACTION_VIEW, u);
        i.addCategory(Intent.CATEGORY_BROWSABLE);
        String browser = defaultBrowser();
        if (browser != null) i.setPackage(browser);
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (ActivityNotFoundException ignored) { }
        }
    }

    /** Varsayılan tarayıcının paket adı (hiçbir uygulamanın sahiplenmediği bir adresle bulunur). */
    private String defaultBrowser() {
        android.content.pm.PackageManager pm = getPackageManager();
        Intent probe = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com/"));
        probe.addCategory(Intent.CATEGORY_BROWSABLE);
        android.content.pm.ResolveInfo ri = pm.resolveActivity(probe, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
        if (ri != null && ri.activityInfo != null && !"android".equals(ri.activityInfo.packageName)
                && !ri.activityInfo.packageName.equals(getPackageName())) {
            return ri.activityInfo.packageName;
        }
        java.util.List<android.content.pm.ResolveInfo> all = pm.queryIntentActivities(probe, 0);
        for (android.content.pm.ResolveInfo r : all) {
            String pkg = r.activityInfo.packageName;
            if (!pkg.equals(getPackageName())) return pkg;
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PICK_IMAGES || fileCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        Uri[] result = null;
        if (resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int n = data.getClipData().getItemCount();
                result = new Uri[n];
                for (int k = 0; k < n; k++) result[k] = data.getClipData().getItemAt(k).getUri();
            } else if (data.getData() != null) {
                result = new Uri[]{data.getData()};
            }
        }
        fileCallback.onReceiveValue(result);
        fileCallback = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
