package bg.avtodnevnik.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

/**
 * АвтоДневник — приложението за Android.
 * Показва платформата на цял екран (без адресна лента), поддържа качване на снимка
 * от формата за запис и бутон „назад". Ако адресът на платформата се смени,
 * приложението предлага да се въведе новият (пази се в настройките на телефона).
 */
public class MainActivity extends Activity {

    private static final String DEFAULT_URL = "https://antonio-seemed-defense-excluded.trycloudflare.com/";
    private static final int FILE_REQ = 1001;

    private WebView web;
    private SharedPreferences prefs;
    private ValueCallback<Uri[]> filePathCallback;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences("avtodnevnik", Context.MODE_PRIVATE);

        web = new WebView(this);
        setContentView(web, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String host = Uri.parse(url()).getHost();
                if (host != null && host.equalsIgnoreCase(u.getHost())) {
                    return false;                       // вътрешна връзка — остава в приложението
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));   // външна — в браузъра
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) {
                    showAddressDialog("Няма връзка с платформата. Провери интернета на телефона. "
                            + "Ако адресът се е сменил, въведи новия:");
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb,
                                             FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = cb;
                try {
                    startActivityForResult(params.createIntent(), FILE_REQ);
                    return true;
                } catch (Exception ex) {
                    filePathCallback = null;
                    return false;
                }
            }
        });

        web.loadUrl(url());
    }

    private String url() {
        return prefs.getString("url", DEFAULT_URL);
    }

    private void showAddressDialog(String message) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(url());
        new AlertDialog.Builder(this)
                .setTitle("АвтоДневник")
                .setMessage(message)
                .setView(input)
                .setPositiveButton("Зареди", (d, w) -> {
                    String u = input.getText().toString().trim();
                    if (u.isEmpty()) {
                        return;
                    }
                    if (!u.startsWith("http")) {
                        u = "https://" + u;
                    }
                    prefs.edit().putString("url", u).apply();
                    web.loadUrl(u);
                })
                .setNegativeButton("Отказ", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_REQ) {
            if (filePathCallback != null) {
                Uri[] results = null;
                if (resultCode == RESULT_OK && data != null) {
                    if (data.getClipData() != null) {
                        int n = data.getClipData().getItemCount();
                        results = new Uri[n];
                        for (int i = 0; i < n; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    } else if (data.getData() != null) {
                        results = new Uri[]{data.getData()};
                    }
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && web != null && web.canGoBack()) {
            web.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
