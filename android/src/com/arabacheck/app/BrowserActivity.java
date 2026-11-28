package com.arabacheck.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Uygulama içi ilan tarayıcısı. sahibinden / arabam ilanını kullanıcı kendisi açar;
 * "Bu ilanı analiz et" ile sayfanın görünen yazısı (document.body.innerText) ArabaCheck'e aktarılır.
 * Paylaşımla (link) açıldıysa sayfa yüklenince ilan kendiliğinden okunur.
 */
public class BrowserActivity extends Activity {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_AUTO = "auto";
    private static final String NAVY = "#081530";

    private WebView web;
    private TextView title;
    private Button analyze;
    private boolean auto, sent;
    private int autoTries;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra(EXTRA_URL);
        auto = getIntent().getBooleanExtra(EXTRA_AUTO, false);
        if (TextUtils.isEmpty(url)) url = "https://www.sahibinden.com/kategori/otomobil";

        // Üst çubuk: kapat + sayfa başlığı
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Color.parseColor(NAVY));
        Button close = new Button(this);
        close.setText("✕");
        close.setTextColor(Color.WHITE);
        close.setTextSize(20);
        close.setBackgroundColor(Color.TRANSPARENT);
        close.setOnClickListener(v -> finish());
        bar.addView(close, new LinearLayout.LayoutParams(dp(56), dp(56)));
        title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setText("İlan yükleniyor…");
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String sc = req.getUrl().getScheme();
                // http(s) sayfalar burada kalsın; "uygulamada aç" (intent://, market://) yönlendirmelerini yok say
                return !("http".equals(sc) || "https".equals(sc));
            }

            @Override
            public void onPageFinished(WebView view, String u) {
                String t = view.getTitle();
                title.setText(TextUtils.isEmpty(t) ? u : t);
                if (auto && !sent) { autoTries = 0; handler.postDelayed(BrowserActivity.this::tryAuto, 1500); }
            }
        });

        // Alt düğme: "Bu ilanı analiz et"
        analyze = new Button(this);
        analyze.setText("Bu ilanı analiz et");
        analyze.setAllCaps(false);
        analyze.setTextSize(16);
        analyze.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(NAVY));
        bg.setCornerRadius(dp(16));
        analyze.setBackground(bg);
        analyze.setElevation(dp(6));
        analyze.setOnClickListener(v -> extract(true));

        FrameLayout content = new FrameLayout(this);
        content.addView(web, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, dp(56), Gravity.BOTTOM);
        lp.setMargins(dp(16), 0, dp(16), dp(16));
        content.addView(analyze, lp);

        final View topSpace = new View(this);
        topSpace.setBackgroundColor(Color.parseColor(NAVY));
        final View bottomSpace = new View(this);
        bottomSpace.setBackgroundColor(Color.WHITE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(topSpace, new LinearLayout.LayoutParams(-1, 0));
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(56)));
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(bottomSpace, new LinearLayout.LayoutParams(-1, 0));
        setContentView(root);
        getWindow().setStatusBarColor(Color.parseColor(NAVY));
        root.setOnApplyWindowInsetsListener((View v, WindowInsets insets) -> {
            int top, bottom;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                top = i.top; bottom = i.bottom;
            } else {
                top = insets.getSystemWindowInsetTop(); bottom = insets.getSystemWindowInsetBottom();
            }
            topSpace.getLayoutParams().height = top;
            bottomSpace.getLayoutParams().height = bottom;
            topSpace.requestLayout();
            bottomSpace.requestLayout();
            return insets;
        });

        if (auto) analyze.setText("İlan okunuyor…");
        web.loadUrl(url);
    }

    /** Paylaşımla açıldıysa: ilan içeriği yüklenene kadar birkaç kez dene. */
    private void tryAuto() {
        if (sent || isFinishing()) return;
        autoTries++;
        web.evaluateJavascript("(function(){return document.body?document.body.innerText:''})()", value -> {
            String text = decode(value);
            if (looksLikeListing(text)) { deliver(text); return; }
            if (autoTries < 6) handler.postDelayed(this::tryAuto, 1500);
            else {
                auto = false;
                analyze.setText("Bu ilanı analiz et");
                Toast.makeText(this, "İlan otomatik okunamadı. Sayfa açılınca \"Bu ilanı analiz et\"e basın.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void extract(boolean manual) {
        web.evaluateJavascript("(function(){return document.body?document.body.innerText:''})()", value -> {
            String text = decode(value);
            if (text.trim().length() < 80) {
                Toast.makeText(this, "Sayfada okunacak ilan bilgisi bulunamadı.", Toast.LENGTH_LONG).show();
                return;
            }
            if (manual && !looksLikeListing(text))
                Toast.makeText(this, "Bu bir ilan sayfası gibi görünmüyor; yine de deneniyor.", Toast.LENGTH_SHORT).show();
            deliver(text);
        });
    }

    private static boolean looksLikeListing(String t) {
        if (t == null || t.length() < 300) return false;
        boolean money = t.contains("TL") || t.contains("₺");
        boolean fields = t.contains("İlan No") || t.contains("İlan Bilgileri") || t.contains("Kilometre") || t.contains("KM");
        return money && fields;
    }

    private static String decode(String jsValue) {
        try {
            Object o = new org.json.JSONTokener(jsValue).nextValue();
            return o instanceof String ? (String) o : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void deliver(String text) {
        if (sent) return;
        sent = true;
        if (text.length() > 60000) text = text.substring(0, 60000);
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(MainActivity.EXTRA_LISTING_TEXT, text);
        i.putExtra(MainActivity.EXTRA_LISTING_URL, web.getUrl());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        web.destroy();
        super.onDestroy();
    }
}
