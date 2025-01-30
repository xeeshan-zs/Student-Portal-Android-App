package com.atrons.studentportal;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private Button reloadButton;
    private static final int FILE_CHOOSER_REQUEST_CODE = 1;
    private float xCoOrdinate, yCoOrdinate;

    // Firebase permission request
    private final ActivityResultLauncher<String> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission Granted, get device token
                    getDeviceToken();
                } else {
                    Log.e("FireBaseLogs", "Notification permission denied");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase
        requestPermission();

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
        webSettings.setSupportMultipleWindows(true); // Needed for printing

        // Handle WebView navigation
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("https://wa.me/") || url.startsWith("https://www.linkedin.com/")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }

                if (url.contains("docs.google.com/forms") || url.contains("drive.google.com")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }

                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        });

        // Handle downloads
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        // Handle printing and file chooser
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        Intent intent = fileChooserParams.createIntent();
                        startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                    } else {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, FILE_CHOOSER_REQUEST_CODE);
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

        // Enable JavaScript printing
        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void printPage() {
                runOnUiThread(() -> {
                    PrintManager printManager = (PrintManager) getSystemService(PRINT_SERVICE);
                    PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter("Print_Document");
                    printManager.print("Print_Document", printAdapter, null);
                });
            }
        }, "AndroidPrint");

        // Set up reload button
        reloadButton.setOnClickListener(v -> webView.reload());

        // Make reload button draggable
        reloadButton.setOnTouchListener((v, event) -> {
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

    // Firebase
    public void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // Permission already granted
                getDeviceToken();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Log.w("FireBaseLogs", "Notification permission needed for better experience.");
            } else {
                // Request permission
                resultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Get device token for older versions
            getDeviceToken();
        }
    }

    public void getDeviceToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful() || task.getResult() == null) {
                    Log.e("FireBaseLogs", "Fetching token failed", task.getException());
                    return;
                }

                // Get Device Token
                String token = task.getResult();
                Log.v("FireBaseLogs", "Device Token: " + token);

                // TODO: Send the token to your server
            }
        });
    }
}
