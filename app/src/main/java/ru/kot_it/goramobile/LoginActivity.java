package ru.kot_it.goramobile;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends Activity {

    private static final String TAG = "LoginActivity";

    public static final String PREFS_LOGIN_METANIM_KEY = "ru.kot_it.goramobileassistant.METANIM";
    public static final String PREFS_LOGIN_USERNAME_KEY = "ru.kot_it.goramobileassistant.USER";
    public static final String PREFS_LOGIN_PASSWORD_KEY = "ru.kot_it.goramobileassistant.PASS";
    public static final String PREFS_LOGIN_COOKIES_KEY = "ru.kot_it.goramobileassistant.COOKIES";
    public static final String PREFS_LOGGED_IN_KEY = "ru.kot_it.goramobileassistant.LOGGED";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    private AutoCompleteTextView mMetanimView;
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;


    public String mCookie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mMetanimView = (AutoCompleteTextView) findViewById(R.id.metanim);
        mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mGoraSignInButton = (Button) findViewById(R.id.gora_sign_in_button);
        mGoraSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    attemptLogin();
                } else {
                    Toast.makeText(getApplicationContext(), "No network connection available!", Toast.LENGTH_LONG).show();
                }
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        mMetanimView.setText(PrefUtils.getFromPrefs(LoginActivity.this, PREFS_LOGIN_METANIM_KEY, ""));
        mUsernameView.setText(PrefUtils.getFromPrefs(LoginActivity.this, PREFS_LOGIN_USERNAME_KEY, ""));
        mPasswordView.setText(PrefUtils.getFromPrefs(LoginActivity.this, PREFS_LOGIN_PASSWORD_KEY, ""));
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid metanim, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mMetanimView.setError(null);
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String metanim = mMetanimView.getText().toString();
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for non-empty username
        if (!TextUtils.isEmpty(username) && !isUsernameValid(username)) {
            mUsernameView.setError(getString(R.string.error_invalid_username));
            focusView = mUsernameView;
            cancel = true;
        }

        // Check for a valid metanim.
        if (TextUtils.isEmpty(metanim)) {
            mMetanimView.setError(getString(R.string.error_field_required));
            focusView = mMetanimView;
            cancel = true;
        } else if (!isMetanimValid(metanim)) {
            mMetanimView.setError(getString(R.string.error_invalid_metanim));
            focusView = mMetanimView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(metanim, username, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isMetanimValid(String metanim) {
        //TODO: Replace this with proper logic
        //return metanim.contains("@");
        return metanim.length() > 4;
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with proper logic
        return password.length() > 4;
    }

    private boolean isUsernameValid(String username) {
        //TODO: Replace this with proper logic
        return username.length() > 3;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mMetanim;
        private final String mUsername;
        private final String mPassword;

        UserLoginTask(String metanim, String username, String password) {
            mMetanim = metanim;
            mUsername = username;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            // Attempt authentication
            String requestURL = "http://" + mMetanim + ".gora.online" ;
            HashMap<String, String> postDataParams = new HashMap<>();
            postDataParams.put("system", mMetanim);
            postDataParams.put("user", mUsername);
            postDataParams.put("password", mPassword);

            performPostCall(requestURL, postDataParams);

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                // Pass data back to MainActivity
                Intent returnIntent = new Intent();
                returnIntent.putExtra("metanim", mMetanim);
                returnIntent.putExtra("loggedIn", true);
                returnIntent.putExtra("cookie", mCookie);
                setResult(RESULT_OK, returnIntent);

                // Saving user credentials on success
                PrefUtils.saveToPrefs(LoginActivity.this, PREFS_LOGIN_METANIM_KEY, mMetanim);
                PrefUtils.saveToPrefs(LoginActivity.this, PREFS_LOGIN_USERNAME_KEY, mUsername);
                PrefUtils.saveToPrefs(LoginActivity.this, PREFS_LOGIN_PASSWORD_KEY, mPassword);
                PrefUtils.saveToPrefs(LoginActivity.this, PREFS_LOGIN_COOKIES_KEY, mCookie);
                PrefUtils.saveBooleanToPrefs(LoginActivity.this, PREFS_LOGGED_IN_KEY, true);

                Log.d("LoginActivity", "Put back the intent: " + mMetanim);
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    public String  performPostCall(String requestURL, HashMap<String, String> postDataParams) {

        URL url;
        String response = "";
        String contentLength = "";

        try {
            contentLength = Integer.toString(getPostDataString(postDataParams).length());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            url = new URL(requestURL+"/system/logon.php");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", contentLength);
            conn.setRequestProperty("Connection", "close");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                final String COOKIES_HEADER = "Set-Cookie";
                mCookie = conn.getHeaderField(COOKIES_HEADER);
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            }
            else {
                response="";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Response from performPostCall: " + response);
        return response;
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }
}
