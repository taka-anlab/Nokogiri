package jp.anlab.nokogiri.no2;

import java.io.File;
import java.io.FileOutputStream;

import jp.anlab.nokogiri.no2.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Window;
import android.widget.EditText;

public class SplashActivity extends Activity {

	private static final String LOG_TAG = "SplashActivity";
	Handler mHandler;
	Runnable mRunnable;
	private EditText edittext;
	AlertDialog.Builder alertDialogBuilder;
	SharedPreferences pref;
	SharedPreferences.Editor editor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.splash);

		alertDialogBuilder = new AlertDialog.Builder(this);
		edittext = new EditText(this);
		// SharedPrefernces の取得
		pref = getSharedPreferences("pref", Activity.MODE_PRIVATE);

		// 起動遅延処理
		mHandler = new Handler();
		mRunnable = new splashHandler();
		mHandler.postDelayed(mRunnable, 500);
	}

	class splashHandler implements Runnable {
		public void run() {
				Intent i = new Intent(getApplication(), NokogiriActivity.class);
				startActivity(i);
				finish();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (isFinishing()) {
			Log.i(LOG_TAG, "Finish");
			mHandler.removeCallbacks(mRunnable);
		}
	}

	@Override
	protected void onDestroy() {
		Log.i(LOG_TAG, "onDestroy");
		super.onDestroy();
	}
}