package br.com.firetv.webapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.graphics.Canvas;
import android.graphics.Paint;
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
    private CursorView cursor;
    private boolean cursorEnabled = false;
    private float cursorX, cursorY;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
        root = new FrameLayout(this);
        web = new WebView(this);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        cursor = new CursorView();
        root.addView(cursor, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        root.post(() -> { cursorX = root.getWidth() / 2f; cursorY = root.getHeight() / 2f; cursor.invalidate(); });

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
                cursor.bringToFront();
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

    private class CursorView extends View {
        private final Paint paint = new Paint(3);
        CursorView() { super(MainActivity.this); setFocusable(false); setClickable(false); }
        @Override protected void onDraw(Canvas c) {
            if (!cursorEnabled) return;
            paint.setColor(0xFF000000); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(6f);
            c.drawCircle(cursorX, cursorY, 15f, paint);
            paint.setColor(0xFFFFFFFF); paint.setStrokeWidth(3f);
            c.drawCircle(cursorX, cursorY, 15f, paint);
            c.drawLine(cursorX - 25f, cursorY, cursorX + 25f, cursorY, paint);
            c.drawLine(cursorX, cursorY - 25f, cursorX, cursorY + 25f, paint);
        }
    }

    private void clickCursor() {
        View target = fullScreenVideo != null ? fullScreenVideo : web;
        int[] targetPos = new int[2], rootPos = new int[2];
        target.getLocationOnScreen(targetPos);
        root.getLocationOnScreen(rootPos);
        float x = cursorX + rootPos[0] - targetPos[0];
        float y = cursorY + rootPos[1] - targetPos[1];
        long now = android.os.SystemClock.uptimeMillis();
        target.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0));
        target.dispatchTouchEvent(MotionEvent.obtain(now, now + 60, MotionEvent.ACTION_UP, x, y, 0));
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_MENU) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                cursorEnabled = !cursorEnabled;
                if (cursorX == 0 && cursorY == 0) { cursorX = root.getWidth() / 2f; cursorY = root.getHeight() / 2f; }
                cursor.invalidate();
            }
            return true;
        }
        if (cursorEnabled && (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN || key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT || key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_BACK)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (key == KeyEvent.KEYCODE_BACK) { cursorEnabled = false; }
                else if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) { if (event.getRepeatCount() == 0) clickCursor(); }
                else {
                    float step = 30f * getResources().getDisplayMetrics().density;
                    if (key == KeyEvent.KEYCODE_DPAD_UP) cursorY -= step;
                    if (key == KeyEvent.KEYCODE_DPAD_DOWN) cursorY += step;
                    if (key == KeyEvent.KEYCODE_DPAD_LEFT) cursorX -= step;
                    if (key == KeyEvent.KEYCODE_DPAD_RIGHT) cursorX += step;
                    cursorX = Math.max(0, Math.min(root.getWidth() - 1, cursorX));
                    cursorY = Math.max(0, Math.min(root.getHeight() - 1, cursorY));
                }
                cursor.invalidate();
            }
            return true;
        }
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
