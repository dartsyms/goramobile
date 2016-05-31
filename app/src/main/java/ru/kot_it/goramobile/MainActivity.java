package ru.kot_it.goramobile;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ValueCallback;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

public class MainActivity extends AppCompatActivity
        implements DocViewFragment.OnDocViewFragmentInteractionListener,
        NavigationView.OnNavigationItemSelectedListener, KeyboardWatcher.OnKeyboardToggleListener {

    private static final String TAG = "GoraMobileMainActivity";
    private static final String DOC_VIEW_FRAGMENT_TAG = "document_view_fragment";
    public static final int LOGIN_REQUEST_CODE = 1;

    private FloatingActionButton rightLowerButton;
    private DocViewFragment mDocViewFragment;
    private KeyboardWatcher keyboardWatcher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Entering MainActivity onCreate.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mDocViewFragment = (DocViewFragment) getSupportFragmentManager().getFragment(savedInstanceState, DOC_VIEW_FRAGMENT_TAG);
        } else {
            mDocViewFragment = new DocViewFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, mDocViewFragment, DOC_VIEW_FRAGMENT_TAG)
                    .commit();
        }

        keyboardWatcher = new KeyboardWatcher(this);
        keyboardWatcher.setListener(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        rightLowerButton = (FloatingActionButton) findViewById(R.id.fab);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if (drawer != null) {
            drawer.setDrawerListener(toggle);
        }

        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        setFabButtonMenu();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ActivityResultBus.getInstance().postQueue(new ActivityResultEvent(requestCode, resultCode, data));
    }

    @Override
    public void onBackPressed() {
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
//            drawer.closeDrawer(GravityCompat.START);
//        } else {
//            super.onBackPressed();
//        }
    }


    private void setFabButtonMenu() {
        SubActionButton.Builder rLSubBuilder = new SubActionButton.Builder(this);
        ImageView infoIcon = new ImageView(this);
        ImageView cameraIcon = new ImageView(this);
        ImageView logOutIcon = new ImageView(this);
        ImageView techSupportIcon = new ImageView(this);
        ImageView changeSettingsIcon = new ImageView(this);

        infoIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_chat_light));
        cameraIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_camera_light));
        logOutIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_video_light));
        techSupportIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_place_light));
        changeSettingsIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_send_now_light));

        infoIcon.setLongClickable(true);
        infoIcon.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), "Перейти в витрину", Toast.LENGTH_SHORT).show();
                String showcase_func = "open_showcase";
                mDocViewFragment.callJavaScript(mDocViewFragment.mWebView, showcase_func);
                return true;
            }
        });

        cameraIcon.setLongClickable(true);
        cameraIcon.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), "Сделать снимок (будет реализовано в ближайшем будущем)", Toast.LENGTH_SHORT).show();
                mDocViewFragment.takePicture();
                return true;
            }
        });

        logOutIcon.setLongClickable(true);
        logOutIcon.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Clear cookies in saved preferences, set logged-in key to false

                // call logout function in webview
                mDocViewFragment.mWebView.evaluateJavascript("system_logout(true)", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        Toast.makeText(getApplicationContext(), "Выйти из системы", Toast.LENGTH_SHORT).show();
                        PrefUtils.saveToPrefs(getApplicationContext(), LoginActivity.PREFS_LOGIN_COOKIES_KEY, "");
                        PrefUtils.saveBooleanToPrefs(getApplicationContext(), LoginActivity.PREFS_LOGGED_IN_KEY, false);
                        mDocViewFragment.scriptsApplied = false;
                        Intent loginIntent = new Intent();
                        loginIntent.setClass(MainActivity.this, LoginActivity.class);
                        startActivityForResult(loginIntent, LOGIN_REQUEST_CODE);
                    }
                });

                // restart login activity

                return true;
            }
        });

        techSupportIcon.setLongClickable(true);
        techSupportIcon.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), "Техподдержка", Toast.LENGTH_SHORT).show();
                String techSup_func = "newFrame";
                String techSup_arg = "system/hreport/hcabinet_support";
                mDocViewFragment.callJavaScript(mDocViewFragment.mWebView, techSup_func, techSup_arg);
                return true;
            }
        });

        changeSettingsIcon.setLongClickable(true);
        changeSettingsIcon.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), "Сменить пароль", Toast.LENGTH_SHORT).show();
                String chpass_func = "newFrame";
                String chpass_args = "system/hreport/hreport_changepass";
                PrefUtils.saveToPrefs(getApplicationContext(), LoginActivity.PREFS_LOGIN_PASSWORD_KEY, "");
                mDocViewFragment.callJavaScript(mDocViewFragment.mWebView, chpass_func, chpass_args);
                return true;
            }
        });



        // Build the menu with default options: light theme, 90 degrees, 72dp radius.
        // Set 4 default SubActionButtons
        final FloatingActionMenu rightLowerMenu = new FloatingActionMenu.Builder(this)
                .addSubActionView(rLSubBuilder.setContentView(infoIcon).build())
                .addSubActionView(rLSubBuilder.setContentView(cameraIcon).build())
                .addSubActionView(rLSubBuilder.setContentView(logOutIcon).build())
                .addSubActionView(rLSubBuilder.setContentView(techSupportIcon).build())
                .addSubActionView(rLSubBuilder.setContentView(changeSettingsIcon).build())
                .attachTo(rightLowerButton)
                .build();

        // Listen menu open and close events to animate the button content view
        rightLowerMenu.setStateChangeListener(new FloatingActionMenu.MenuStateChangeListener() {
            @Override
            public void onMenuOpened(FloatingActionMenu menu) {
                // Rotate the icon of rightLowerButton 45 degrees clockwise
                if (rightLowerButton != null) {
                    rightLowerButton.setRotation(0);
                    PropertyValuesHolder pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, 45);
                    ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(rightLowerButton, pvhR);
                    animation.start();
                }
            }

            @Override
            public void onMenuClosed(FloatingActionMenu menu) {
                // Rotate the icon of rightLowerButton 45 degrees counter-clockwise
                if (rightLowerButton != null) {
                    rightLowerButton.setRotation(45);
                    PropertyValuesHolder pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, 0);
                    ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(rightLowerButton, pvhR);
                    animation.start();
                }
            }
        });

    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        String func = "parent.newFrame";

        if (id == R.id.nav_camera) {
            // learn cabinet
            String learnCabinet = "system/hreport/hcabinet_tutorial";
            mDocViewFragment.callJavaScript(mDocViewFragment.mWebView,func, learnCabinet);
        } else if (id == R.id.nav_gallery) {
            String principalCabinet = "system/hreport/hcabinet_director";
            mDocViewFragment.callJavaScript(mDocViewFragment.mWebView,func, principalCabinet);
        } else if (id == R.id.nav_slideshow) {
            String settings = "system/hreport/hcabinet_setting";
            mDocViewFragment.callJavaScript(mDocViewFragment.mWebView,func, settings);
        } else if (id == R.id.nav_manage) {
            String modules = "system/hreport/hcabinet_paymodule";
            mDocViewFragment.callJavaScript(mDocViewFragment.mWebView,func, modules);
        } else if (id == R.id.nav_share) {
            String techsup = "system/hreport/hcabinet_support";
            mDocViewFragment.callJavaScript(mDocViewFragment.mWebView,func, techsup);
        } else if (id == R.id.nav_send) {
            String userManagment = "system/hreport/hcabinet_users";
            mDocViewFragment.callJavaScript(mDocViewFragment.mWebView,func, userManagment);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }

        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mDocViewFragment.mWebView.canGoBack()) {
            mDocViewFragment.mWebView.goBack();
            return true;
        }
        return onKeyDown(keyCode, event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
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

    // document view fragments callbacks
    @Override
    public void onDocViewFragmentInteraction(Uri uri) {

    }

//    @Override
//    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
//        super.onSaveInstanceState(outState, outPersistentState);
//        getSupportFragmentManager().putFragment(outState, "mDocViewFragment", mDocViewFragment);
//    }
//
//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        keyboardWatcher.destroy();
        super.onDestroy();
    }

    @Override
    public void onKeyboardShown(int keyboardSize) {

    }

    @Override
    public void onKeyboardClosed() {

    }

}
