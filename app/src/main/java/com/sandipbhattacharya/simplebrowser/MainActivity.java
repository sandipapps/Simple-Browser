package com.sandipbhattacharya.simplebrowser;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

public class MainActivity extends AppCompatActivity {
    // Declare View object references
    EditText etUrl;
    Button btnGo;
    WebView webView;
    ProgressBar progressBar;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        // Instantiate View objects
        etUrl = findViewById(R.id.etUrl);
        btnGo = findViewById(R.id.btnGo);
        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progressBar);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading Please Wait...");
        // Create a WebSettings object
        WebSettings webSettings = webView.getSettings();
        // Set Zoom Controls
        webSettings.setBuiltInZoomControls(true);
        // JavaScript is by default turned off in WebView. To enable JavaScript we can call
        // setJavaScriptEnabled() method on webSettings object and pass true as parameter.
        webSettings.setJavaScriptEnabled(true);
        /*
         Now, when the user clicks a link from a web page in your WebView, the
         default behavior for Android is to launch an application that handles
         URLs. Typically, system's default web browser opens and loads the
         destination URL. However, you can override this behavior for your
         WebView, so that links open within your WebView and hence within the app.
         You can then allow the user to navigate backward and forward through their web page history
         that's maintained by your WebView. To accomplish this, we need to create a subclass of
         WebViewClient and override it's shouldOverrideUrlLoading() method. Let's do that.
         */
        webView.setWebViewClient(new MyWebViewClient());
        if (!checkConnection()) {
            showDialog();
        }
        // Attach OnClickListener with the Button
        btnGo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // Get the text from EditText
                String url = etUrl.getText().toString().trim();
                // If entered url doesn't start "http://" or "https://" we need to append it
                // before the url.
                if (!url.startsWith("http://") || !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                // To load the webView with the url we need to call loadUrl() method
                // of the WebView class, passing url as parameter.
                webView.loadUrl(url);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                setTitle("Loading...");
                progressBar.setProgress(newProgress);
                progressDialog.show();
                if (newProgress == 100) {
                    setTitle(webView.getTitle());
                    progressDialog.dismiss();
                }
                super.onProgressChanged(view, newProgress);
            }
        });

        webView.setDownloadListener(new MyDownLoadListener());
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // When you return false you're telling Android not to override; let WebView
            // load the page
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        }
    }

    /* Run the application. (pause)
       Let's specify a url in the EditText field and press the ENTER button to launch the website.
       But before that please make sure that you are connected to the internet.
       The site looks good in WebView.
       As you can see, the url loads properly in WebView but if we click the Back button,
       the app gets closed even though we’ve navigated through a few pages within the WebView itself.
       Let's see how we can handle navigation in WebView with Back Button.
       To go through the browsing history on pressing the Back button we need to override
       onBackPressed() method as follows.
     */

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure you want to Exit?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    // The WebView maintains a browsing history just like a normal browser.
    // If there is no history then it will result in the default behavior of the Back button i.e.
    // closing the app.

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navPrev:
                onBackPressed();
                break;
            case R.id.navNext:
                if (webView.canGoForward()) {
                    webView.goForward();
                }
                break;
            case R.id.navReload:
                checkConnection();
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        return true;
                    }
                }
            } else {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Connect to WIFI or Exit")
                .setCancelable(false)
                .setPositiveButton("Connect to WIFI", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                })
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public class MyDownLoadListener implements DownloadListener {
        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {

            if (url != null) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        }
    }
}