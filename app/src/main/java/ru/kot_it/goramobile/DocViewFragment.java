package ru.kot_it.goramobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class DocViewFragment extends Fragment {

    static final String TAG = "From MainActivity";
    public static final int LOGIN_REQUEST_CODE = 1;
    public static final int INPUT_FILE_REQUEST_CODE = 2;

    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;


    WebView mWebView;
    Boolean mLoggedIn = false;
    String mLink = "";
    String mMetanim = "";
    String cookie;
    String jsCallbacks = "deviceCallbacks";
    boolean scriptsApplied = false;

    private OnDocViewFragmentInteractionListener mListener;

    public DocViewFragment() {
        // Required empty public constructor
    }

    public interface OnDocViewFragmentInteractionListener {
        // TODO: Update argument type and name
        void onDocViewFragmentInteraction(Uri uri);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled"})
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rlContentView = inflater.inflate(R.layout.fragment_doc_view, container, false);
        mLoggedIn = PrefUtils.getBooleanFromPrefs(getActivity(), LoginActivity.PREFS_LOGGED_IN_KEY, false);
        mMetanim = PrefUtils.getFromPrefs(getActivity(), LoginActivity.PREFS_LOGIN_METANIM_KEY, "");
        cookie  = PrefUtils.getFromPrefs(getActivity(), LoginActivity.PREFS_LOGIN_COOKIES_KEY, "");

        mWebView = (WebView) rlContentView.findViewById(R.id.webview);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setUseWideViewPort(true);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                rlContentView.findViewById(R.id.webViewProgressBar).setVisibility(View.GONE);
                // to log in
                String loginStart = "document.getElementById('login').click";
                callJavaScript(mWebView, loginStart);
                setupWorkspace();


                // TODO: add control to log out
                super.onPageFinished(view, url);
            }

        });

        mWebView.setFocusable(true);
        mWebView.setFocusableInTouchMode(true);
        mWebView.requestFocus(View.FOCUS_DOWN|View.FOCUS_UP);


        mWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        if (!v.hasFocus()) {
                            v.requestFocus();
                        }
                        break;
                }
                return false;
            }
        });


        mWebView.addJavascriptInterface(new GoraJavaScriptInterface(this.getActivity()), jsCallbacks);

        if (!mLoggedIn || mMetanim.isEmpty() || cookie.isEmpty() || cookie.equals("")) {
            Log.d(TAG, "DocViewFragment sends intent to LoginActivity");
            Intent loginIntent = new Intent();
            loginIntent.setClass(getActivity(), LoginActivity.class);
            getActivity().startActivityForResult(loginIntent, LOGIN_REQUEST_CODE);
        } else {
            Log.d(TAG, "Enter site with: " + mMetanim + ":" + mLoggedIn + ": and cookie:" + cookie);
            mLink = "http://" + mMetanim + ".gora.online/";
            Log.d(TAG, "Try to get link: " + mLink);
            android.webkit.CookieManager webCookieManager = CookieManager.getInstance();
            webCookieManager.setAcceptCookie(true);
            webCookieManager.setCookie(mLink, cookie);
            mWebView.loadUrl(mLink);
        }

        mWebView.setWebChromeClient(new WebChromeClient() {
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             WebChromeClient.FileChooserParams fileChooserParams) {
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        Log.e(TAG, "Unable to create Image File", ex);
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");

                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

                return true;
            }
        });

        return rlContentView;
    }

    private void setupWorkspace() {
        String removeUserMenu = "document.getElementById('menuuser').remove";
        String removeButton = "document.getElementById('btn_support').remove";
        String loadWorkspace = "javascript:parent.newFrame('system/hreport/hcabinet_director')";
        mWebView.evaluateJavascript("document.getElementById('menuline').style.backgroundColor = '#777';", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d(TAG, "Called changeColor to backgroundColor = '#777'");
            }
        });

        mWebView.evaluateJavascript("document.getElementsByClassName('ml_btn')[0].style.cssFloat = 'right';", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d(TAG, "Called moveMenu to the right");
            }
        });

        mWebView.evaluateJavascript("document.getElementById('menuitem_nav').style.cssFloat = 'left';", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d(TAG, "Called moveNavButton to the left");
            }
        });

        callJavaScript(mWebView, removeUserMenu);
        callJavaScript(mWebView, removeButton);

        mWebView.loadUrl(loadWorkspace);
        scriptsApplied = true;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onDocViewFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDocViewFragmentInteractionListener) {
            mListener = (OnDocViewFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDocViewFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        ActivityResultBus.getInstance().register(mActivityResultSubscriber);
    }

    @Override
    public void onStop() {
        cookie = "";
        PrefUtils.saveToPrefs(getActivity(), LoginActivity.PREFS_LOGIN_COOKIES_KEY, "");
        scriptsApplied = false;
        super.onStop();
        ActivityResultBus.getInstance().unregister(mActivityResultSubscriber);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private Object mActivityResultSubscriber = new Object() {
        @Subscribe
        public void onActivityResultReceived(ActivityResultEvent event) {
            int requestCode = event.getRequestCode();
            int resultCode = event.getResultCode();
            Intent data = event.getData();
            onActivityResult(requestCode, resultCode, data);
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK){
                mMetanim = data.getStringExtra("metanim");
                mLoggedIn = data.getBooleanExtra("loggedIn", false);
                cookie = data.getStringExtra("cookie");

                Log.d(TAG, "LoginActivity passes values:"+mMetanim+":"+mLoggedIn+": and cookie:"+cookie);

                mLink = "http://" + mMetanim + ".gora.online/";
                Log.d(TAG, "Try to get link: " + mLink);

                android.webkit.CookieManager webCookieManager = CookieManager.getInstance();
                webCookieManager.setAcceptCookie(true);
                webCookieManager.setCookie(mLink, cookie);

                mWebView.loadUrl(mLink);
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                // TODO: Do something if there's no result
            }
        }
        if(requestCode == INPUT_FILE_REQUEST_CODE && mFilePathCallback != null) {
            Uri[] results = null;
            // Check that the response is a good one
            if(resultCode == Activity.RESULT_OK) {
                if(data != null) {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                } else {
                    // If there is no data, then we may have taken a photo
                    if(mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        }
    }

    void updateDocViewDisplay(int position) {
        // load new document
    }

    final class GoraJavaScriptInterface {
        Context mContext;

        GoraJavaScriptInterface(Context ctx) {
            mContext = ctx;
        }

        @JavascriptInterface
        public void makePhoto() {
            takePicture();
            sendPictureToServer();
            handleResponse();
        }

        @JavascriptInterface
        public void doSomething() {
            // do something
        }

        @JavascriptInterface
        public void doSomethingElse(){

        }

        @JavascriptInterface
        public void savePreferences(String key, String value) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(key, value);
            editor.apply();
        }

        @JavascriptInterface
        public String loadPreferences(String key) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            return sp.getString(key, "");
        }

        @JavascriptInterface
        public void vibrate(long millisec) {
            Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(millisec);
        }

        @JavascriptInterface
        public void sendSMS(String phoneNumber, String message) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        }

        @JavascriptInterface
        public void showVideo(String videoUrlString) {
            Intent videoIntent = new Intent(Intent.ACTION_VIEW);
            videoIntent.setDataAndType(Uri.parse(videoUrlString), "video/3gpp");
            mContext.startActivity(videoIntent);
        }

    }


    protected void callJavaScript(WebView view, String methodName, Object...params){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("javascript:try{");
        stringBuilder.append(methodName);
        stringBuilder.append("(");
        String separator = "";
        if (params != null) {
            for (Object param : params) {
                stringBuilder.append(separator);
                separator = ",";
                if(param instanceof String){
                    stringBuilder.append("'");
                }
                stringBuilder.append(param);
                if(param instanceof String){
                    stringBuilder.append("'");
                }

            }
        }

        stringBuilder.append(");} catch(error){console.error(error.message);}");
        final String call = stringBuilder.toString();
        Log.i(TAG, "callJavaScript: call = " + call);

        view.loadUrl(call);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    protected void takePicture() {
        // TODO: implement
    }

    protected void sendPictureToServer() {
        // TODO: implement
    }

    protected void handleResponse() {
        // TODO: implement
    }


}
