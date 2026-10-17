package br.com.firetv.webapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.widget.FrameLayout;
import android.content.Intent;
import android.net.Uri;

public class MainActivity extends Activity {
    private static final String HOME = "https://bingebang.st/explore";
    private WebView web;
    private FrameLayout root;
    private View fullScreenVideo;
    private WebChromeClient.CustomViewCallback videoCallback;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
        root = new FrameLayout(this);
        web = new WebView(this);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportMultipleWindows(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if (scheme != null && (scheme.equals("https") || scheme.equals("http"))) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) { }
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullScreenVideo != null) { callback.onCustomViewHidden(); return; }
                fullScreenVideo = view;
                videoCallback = callback;
                web.setVisibility(View.GONE);
                root.addView(view, new FrameLayout.LayoutParams(-1, -1));
                view.requestFocus();
            }
            @Override public void onHideCustomView() { closeFullScreen(); }
        });
        if (savedInstanceState == null) web.loadUrl(HOME);
        else web.restoreState(savedInstanceState);
        web.requestFocus();
    }

    private void closeFullScreen() {
        if (fullScreenVideo == null) return;
        root.removeView(fullScreenVideo);
        fullScreenVideo = null;
        web.setVisibility(View.VISIBLE);
        if (videoCallback != null) { videoCallback.onCustomViewHidden(); videoCallback = null; }
        web.requestFocus();
    }

    @Override public void onBackPressed() {
        if (fullScreenVideo != null) closeFullScreen();
        else if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            web.evaluateJavascript("(function(){var v=document.querySelector('video');if(v){if(v.paused)v.play();else v.pause();}})()", null);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        web.saveState(outState);
        super.onSaveInstanceState(outState);
    }
    @Override protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }
}
