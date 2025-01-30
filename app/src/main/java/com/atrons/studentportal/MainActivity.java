package com.atrons.studentportal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.os.Build;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private Button reloadButton;

    private static final int FILE_CHOOSER_REQUEST_CODE = 1;
    private float xCoOrdinate, yCoOrdinate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize WebView, ProgressBar, and Reload Button
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        reloadButton = findViewById(R.id.reloadButton);

        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Handle WebView navigation
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Handle phone links (tel:)
                if (url.startsWith("tel:")) {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                    startActivity(dialIntent);
                    return true;  // Prevent WebView from handling this URL
                }

                // Handle email links (mailto:)
                if (url.startsWith("mailto:")) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                    startActivity(emailIntent);
                    return true;  // Prevent WebView from handling this URL
                }

                // Handle WhatsApp links (https://wa.me/)
                if (url.startsWith("https://wa.me/")) {
                    Intent waIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(waIntent);
                    return true;  // Prevent WebView from handling this URL
                }

                // Handle LinkedIn links (https://www.linkedin.com/)
                if (url.startsWith("https://www.linkedin.com/")) {
                    Intent linkedInIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(linkedInIntent);
                    return true;  // Prevent WebView from handling this URL
                }

                // Handle Google Docs or Drive links in external browser
                if (url.contains("docs.google.com/forms") || url.contains("drive.google.com")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true; // Prevent WebView from loading this URL
                }

                // Otherwise, load the URL in WebView
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        });

        // Handle progress bar visibility
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        Intent intent = fileChooserParams.createIntent();
                        startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                    } else {
                        requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, FILE_CHOOSER_REQUEST_CODE);
                    }
                } else {
                    Intent intent = fileChooserParams.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                }
                return true;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        // Load the initial page
        webView.loadUrl("https://students.atrons.net");

        // Set up reload button
        reloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
            }
        });

        // Make reload button draggable
        reloadButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        xCoOrdinate = event.getRawX() - v.getX();
                        yCoOrdinate = event.getRawY() - v.getY();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        v.animate()
                                .x(event.getRawX() - xCoOrdinate)
                                .y(event.getRawY() - yCoOrdinate)
                                .setDuration(0)
                                .start();
                        break;

                    default:
                        return false;
                }
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, intent);
            if (results != null && results.length > 0) {
                // Handle file chosen by user
            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }
}
