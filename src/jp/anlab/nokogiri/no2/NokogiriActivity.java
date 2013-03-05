package jp.anlab.nokogiri.no2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import jp.anlab.nokogiri.no2.R;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.text.InputFilter;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class NokogiriActivity extends Activity implements SensorEventListener {
	private SensorManager sensorManager;
	private String LAvalue;
	private String GYROvalueStr;
	private float[] GYROarray = { 0, 0, 0 };
	private float[] LAarray = { 0, 0, 0, 0 };
	private final int REPEAT_INTERVAL = 200;
	private Handler handler = new Handler();
	private Runnable runnable;
	String roll;
	String sendroll;
	private LinearLayout mLinear;
	boolean StartFlag = false;
	Calendar calendar;
	TextView tv;
	TextView tv2;
	TextView userNameTv;
	AlertDialog.Builder alertDialogBuilder;
	SharedPreferences pref;
	SharedPreferences.Editor editor;
	String filePath;
	FileOutputStream fos = null;
	Button topBtn;
	Button bottomBtn;
	int missGyro0;
	int missGyro1;
	int missGyro2;
	int repeatNokogiriTime;
	boolean repeatNokogiriTimeFlag = false;
	int practicTime;
	final float GYRO_VALUE_LINE = 1.3f;
	MediaPlayer mpGyro = null;
	MediaPlayer mpCount = null;
	MediaPlayer mpPlayer = null;
	boolean rollSDWrite = true;
	int maxLength = 10;
	InputFilter[] FilterArray = new InputFilter[1];
	int minkakudo;
	int maxkakudo;
	// server送信用
	// int[] arr_y = new int[300];
	// int[] arr_gyro = new int[300];
	String value;
	String value_2;
	int buregoukei;
	int flagkaisu;
	boolean flagfinish;
	static ArrayAdapter<String> adapter;
	ArrayList<String> arrayName;
	ArrayList<String> arrayKeyId;
	ArrayList<String> arrayClass;
	ArrayList<String> arrayUserID;
	String userName;

	Editor e;
	int userId;
	android.app.AlertDialog.Builder dialog = null;
	AlertDialog dialog2;
	int userClassNum = 2;
	String[] classStr = { "1", "2", "3", "4" };
	TextView mostTopTV;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.permitAll().build());
		pref = getSharedPreferences("pref", Activity.MODE_PRIVATE);
		e = pref.edit();
		classView();
		tv = (TextView) findViewById(R.id.topTV);
		tv2 = (TextView) findViewById(R.id.secondTV);
		userNameTv = (TextView) findViewById(R.id.userNameTv);
		mostTopTV = (TextView) findViewById(R.id.mostTopTV);
		// SharedPrefernces の取得
		topBtn = (Button) findViewById(R.id.topBtn);
		bottomBtn = (Button) findViewById(R.id.bottomBtn);

		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		mLinear = (LinearLayout) findViewById(R.id.topLinear);
		syokika();
		alertDialogBuilder = new AlertDialog.Builder(this);
		// SD カード/パッケージ名 ディレクトリ生成////////////////////////
		File outDir = new File(Environment.getExternalStorageDirectory(),
				this.getPackageName());
		// パッケージ名のディレクトリが SD カードになければ作成します。
		if (outDir.exists() == false) {
			Log.d("フォルダの作成", "フォルダ作成しました");
			outDir.mkdir();
		}

		// SensorManagerインスタンスを取得
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		if (pref.getString("minkakudo", "") == "") {
			e = pref.edit();
			e.putString("minkakudo", "13");
			e.putString("maxkakudo", "17");
			e.commit();
		}
		minkakudo = Integer.parseInt(pref.getString("minkakudo", ""));
		maxkakudo = Integer.parseInt(pref.getString("maxkakudo", ""));
	}

	// OnClickで呼ばれるメソッド
	public void startLearning(View view) {
		if (StartFlag == false) {// 開始を押下時
			rollSDWrite = true;
			mpPlayer.start();
			writeSD();
			topBtn.setText("練習を終える");
			bottomBtn.setVisibility(View.GONE);
			userNameTv.setVisibility(View.GONE);
			mostTopTV.setText("切り終わった？");
			tv.setText("");
			tv2.setText("");
			StartFlag = true;
		} else {
			finishdo();
			bottomBtn.setVisibility(View.VISIBLE);
			userNameTv.setVisibility(View.VISIBLE);
			mostTopTV.setText("ひき溝つけた？");

		}
	}

	public void changeUser(View view) {
		getuser();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Sensor sensor = sensorManager
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		Sensor sensor2 = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		Sensor sensor3 = sensorManager
				.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		sensorManager.registerListener(this, sensor,
				SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(this, sensor2,
				SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(this, sensor3,
				SensorManager.SENSOR_DELAY_GAME);

	}

	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
		handler.removeCallbacks(runnable);

	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {

		case Sensor.TYPE_LINEAR_ACCELERATION:
			if (StartFlag == true) {
				LAvalue = String.valueOf(event.values[0]) + ","
						+ String.valueOf(event.values[1]) + ","
						+ String.valueOf(event.values[2]);
				LAarray[0] = event.values[0];
				LAarray[1] = event.values[1];
				LAarray[2] = event.values[2];
				if (repeatNokogiriTimeFlag == false && event.values[1] > 3.0f) {
					repeatNokogiriTimeFlag = true;
					repeatNokogiriTime++;
					mpCount.start();
				} else if (repeatNokogiriTimeFlag == true
						&& event.values[1] < 1.0f) {
					repeatNokogiriTimeFlag = false;
				}
			}
			break;

		case Sensor.TYPE_GYROSCOPE:
			if (StartFlag == true) {
				GYROarray[0] = event.values[0];
				GYROarray[1] = event.values[1];
				GYROarray[2] = event.values[2];
				GYROvalueStr = String.valueOf(event.values[0]) + ","
						+ String.valueOf(event.values[1]) + ","
						+ String.valueOf(event.values[2]);
			}
			break;

		case Sensor.TYPE_ORIENTATION:
			if (StartFlag == false) {
				roll = String.valueOf((int) event.values[1]);
				tv.setText(roll + "°");
				if ((int) event.values[1] > maxkakudo
						|| (int) event.values[1] < minkakudo) {
					mLinear.setBackgroundResource(R.drawable.red);
					// topBtn.setVisibility(View.INVISIBLE);
				} else {
					mLinear.setBackgroundResource(R.drawable.blue);
					// topBtn.setVisibility(View.VISIBLE);

				}
			}
			break;
		}
	}

	public void writeSD() {
		calendar = Calendar.getInstance();
		final int hour = calendar.get(Calendar.HOUR_OF_DAY);
		final int minute = calendar.get(Calendar.MINUTE);
		final int second = calendar.get(Calendar.SECOND);
		String prefName = pref.getString("name", "");
		String creatTime = "Nokogiri_" + prefName + "_" + String.valueOf(hour)
				+ "_" + String.valueOf(minute) + "_" + String.valueOf(second);
		filePath = Environment.getExternalStorageDirectory() + "/"
				+ this.getPackageName() + "/" + creatTime + ".csv";

		runnable = new Runnable() {
			public void run() {
				// 2.繰り返し処理
				// ///ここから，ストレージへ記録する////////////////////////////////////////////////////
				try {
					fos = new FileOutputStream(filePath, true);
					calendar = Calendar.getInstance();
					final int hour = calendar.get(Calendar.HOUR_OF_DAY);
					final int minute = calendar.get(Calendar.MINUTE);
					final int second = calendar.get(Calendar.SECOND);
					final int millsecond = calendar.get(Calendar.MILLISECOND);

					String writeTime = String.valueOf(hour) + ":"
							+ String.valueOf(minute) + ":"
							+ String.valueOf(second) + ":"
							+ String.valueOf(millsecond);
					String sensorValue = writeTime + "," + LAvalue + ","
							+ GYROvalueStr + "\n";
					Log.d("SD", String.valueOf(StartFlag));
					if (rollSDWrite == true) {
						sendroll = roll;
						rollSDWrite = false;
						String sensorValues = writeTime + "," + LAvalue + ","
								+ GYROvalueStr + "," + roll + "\n";
						fos.write(sensorValues.getBytes());
					} else {
						fos.write(sensorValue.getBytes());
					}
					fos.close();
					Log.d("書き込みOK", "OK");

					if (practicTime == 0) {
						value = String.valueOf((int) LAarray[1]);
						value_2 = String.valueOf((int) (Math.abs(GYROarray[0])
								+ Math.abs(GYROarray[1]) + Math
								.abs(GYROarray[2])));
					} else {
						value = value + "," + String.valueOf((int) LAarray[1]);
						value_2 = value_2
								+ ","
								+ String.valueOf((int) (Math.abs(GYROarray[0])
										+ Math.abs(GYROarray[1]) + Math
										.abs(GYROarray[2])));
					}
					 buregoukei = (int) (buregoukei + (Math.abs(GYROarray[0])
								+ Math.abs(GYROarray[1]) + Math
								.abs(GYROarray[2])));
					practicTime++;
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				// ///////////////////////////////////////////////////////////////////
				// 3.次回処理をセット
				handler.postDelayed(this, REPEAT_INTERVAL);
				gyroMath();
			}
		};
		// 1.初回実行
		handler.postDelayed(runnable, REPEAT_INTERVAL);
	}

	// メニューが生成される際に起動される。////////////////////////////////////////
	// この中でメニューのアイテムを追加したりする。
	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		super.onCreateOptionsMenu(menu);
		// メニューインフレーターを取得
		MenuInflater inflater = getMenuInflater();
		// xmlのリソースファイルを使用してメニューにアイテムを追加
		inflater.inflate(R.menu.menu, menu);
		// できたらtrueを返す
		return true;
	}

	// メニューのアイテムが選択された際に起動される。
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_dialog1:
			listview();
			break;
		case R.id.menu_dialog2:
			android.app.AlertDialog.Builder dialog = new AlertDialog.Builder(
					this);
			LinearLayout layout = new LinearLayout(this);
			final EditText text1 = new EditText(this);
			text1.setHint("最低角度 例:10");
			text1.setWidth(320);
			final EditText text2 = new EditText(this);
			text2.setHint("最高角度 例:30");
			text2.setWidth(320);
			layout.addView(text1);
			layout.addView(text2);
			dialog.setView(layout);

			dialog.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							String minkakudo = text1.getText().toString();
							String maxkakudo = text2.getText().toString();
							Editor e = pref.edit();
							e.putString("minkakudo", minkakudo);
							e.putString("maxkakudo", maxkakudo);

							e.commit();
							Intent i = new Intent(getApplication(),
									NokogiriActivity.class);
							startActivity(i);
							finish();
						}
					});

			dialog.setNegativeButton("Cancle",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					});
			dialog.show();
			break;
		default:
			break;
		}

		return true;
	}

	// メニューここまで/////////////////////////////////////////////////////////////////////////////////////

	public void showDialog() {
		//挽きの強さ計算
		String strongStr ="";
		if((int) (LAarray[3] / practicTime)>5){
			strongStr = "★★★";
		}else if((int) (LAarray[3] / practicTime)>2){
			strongStr = "★★";
		}else{
			strongStr = "★";
		}

		//ブレの計算
		String bureStr ="";
		if((int) (buregoukei / 10)>30){
			bureStr = "★";
		}else if((int)(buregoukei / 10)>10){
			bureStr = "★★";
		}else{
			bureStr = "★★★";
		}

		// アラートダイアログのタイトルを設定します
		alertDialogBuilder.setTitle("KR情報");
		// アラートダイアログのメッセージを設定します
		String kr = "\n" + mathKR(missGyro0, missGyro1, missGyro2);
		String krRepeat = "切削回数　　　 : " + String.valueOf(repeatNokogiriTime)
				+ "回\n";
		String krTime = "作業時間　　　 : " + String.valueOf(practicTime / 5) + "秒\n";
		String krPower = "ひきの強さ　　 : "
				+ strongStr +"\nのこぎりのブレ : "+bureStr+ "\n\nアドバイス : ";
		if ((int) (LAarray[3] / practicTime) < 4) {
			krPower += "\n"
					+ "ひくときの力が弱いです。\nのこぎりの刃が木をしっかりと削れるよう，刃渡りを大きく使って，引くときに力を込めること。\n";
		}

		alertDialogBuilder.setMessage(krRepeat + krTime + krPower + kr);
		// アラートダイアログの肯定ボタンがクリックされた時に呼び出されるコールバックリスナーを登録
		alertDialogBuilder.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						webView();
					}
				});
		AlertDialog alertDialog = alertDialogBuilder.create();
		// アラートダイアログを表示
		alertDialog.show();
	}

	String mathKR(int a, int b, int c) {
		if (a > b && a > c) {
			return "のこぎりの刃が上下にブレているようです。理想の角度を保ちながらひくことを意識しましょう。";
		} else if (b > a && b > c) {
			return "のこぎりの刃が左右にブレているようです。切削物と刃先は常に垂直になるように意識しましょう。刃の真上に顔がきていますか？";
		} else if (c > a && c > b) {
			return "手首が左右にブレているようです。わきを締めて，一直線上に腕が動くように意識しましょう。";
		} else
			return "理想に近いのこぎりびきですね。その調子！";
	}

	public void gyroMath() {
		if (Math.abs(GYROarray[0]) > GYRO_VALUE_LINE
				|| Math.abs(GYROarray[1]) > GYRO_VALUE_LINE
				|| Math.abs(GYROarray[2]) > GYRO_VALUE_LINE) {// ジャイロが閾値を超えたかどうか

			mLinear.setBackgroundColor(Color.RED);
			mpGyro.start();
		} else {
			mLinear.setBackgroundColor(Color.BLUE);
		}

		if (Math.abs(GYROarray[0]) > GYRO_VALUE_LINE) {
			missGyro0++;
		}
		if (Math.abs(GYROarray[1]) > GYRO_VALUE_LINE) {
			missGyro1++;
		}
		if (Math.abs(GYROarray[2]) > 0.5f) {
			missGyro2++;
		}

		LAarray[3] += Math.abs(LAarray[1]);
		Log.d("LA1", String.valueOf(Math.abs(LAarray[1])));
		Log.d("LA3", String.valueOf(LAarray[3]));
	}

	// //////////ユーザリストの取得//////////////////////////////
	public void getuser() {
		InputStream in = null;
		HttpURLConnection http = null;
		try {
			URL url = new URL("http://160.28.60.103/study/output.txt");
			http = (HttpURLConnection) url.openConnection();
			http.setRequestMethod("GET");
			http.connect();
			// データを取得
			in = http.getInputStream();

			// ソースを読み出す
			byte[] data = new byte[8192];
			in.read(data);
			String src = new String(data);
			// ソースの分割
			String[] strAry = src.split("\n");
			arrayClass = new ArrayList<String>();
			arrayUserID = new ArrayList<String>();
			arrayName = new ArrayList<String>();
			arrayKeyId = new ArrayList<String>();

			for (int i = 0; i < strAry.length - 1; i++) {
				String[] strAryPerson = strAry[i].split(",");
				Log.v("strAryPerson", Arrays.toString(strAryPerson));
				if (Integer.parseInt(strAryPerson[1]) == userClassNum) {
					arrayClass.add(strAryPerson[1]);
					arrayUserID.add(strAryPerson[2]);
					arrayName.add(strAryPerson[3]);
					arrayKeyId.add(strAryPerson[5]);
				}
			}
		} catch (Exception e) {
		} finally {
			try {
				if (http != null)
					http.disconnect();
				if (in != null)
					in.close();
			} catch (Exception e) {
			}
		}
		listview();
	}

	// //////////ユーザリストの取得//////////////////////////////

	// /////////リストビュー////////////////////////////////////////////////////////
	public void listview() {
		ListView LV = new ListView(this);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, arrayUserID);
		LV.setAdapter(adapter);
		LV.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> items, View view,
					int position, long id) {
				e.putString("name", arrayName.get(position).toString());
				e.putString("userid", arrayKeyId.get(position).toString());
				e.commit();
				userId = Integer.parseInt(arrayKeyId.get(position).toString());
				userName = arrayName.get(position).toString();
				userNameTv.setText("学習者 : "
						+ arrayName.get(position).toString());
				dialog2.dismiss();
			}
		});
		dialog = new AlertDialog.Builder(this);
		dialog2 = dialog.create();
		dialog2.setCanceledOnTouchOutside(false);
		dialog2.setTitle("あなたの出席番号を選択してください");
		dialog2.setView(LV);
		dialog2.show();
	}

	// /////////リストビュー//////////////////////////////////////////////////////////

	// /////////クラス選択リスト////////////////////////////////////////////////////////
	public void classView() {
		ListView LV = new ListView(this);

		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, classStr);
		dialog = new AlertDialog.Builder(this);
		dialog2 = dialog.create();
		dialog2.setCanceledOnTouchOutside(false);
		dialog2.setTitle("あなたのクラスを選択してください");
		dialog2.setView(LV);
		dialog2.show();

		LV.setAdapter(adapter);
		LV.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> items, View view,
					int position, long id) {
				userClassNum = Integer.parseInt(classStr[position]);
				Log.v("userClassNum", String.valueOf(userClassNum));
				dialog2.dismiss();
				getuser();
			}
		});
	}

	// /////////クラス選択リスト//////////////////////////////////////////////////////////

	// /////////成績表示ビュー////////////////////////////////////////////////////////
	public void webView() {
		mpPlayer.start();
		final WebView webView = new WebView(this);
		webView.loadUrl("http://160.28.60.103/study/growthRecordMobile.php?userName="
				+ userName);
		dialog = new AlertDialog.Builder(this);
		dialog2 = dialog.create();
		dialog2.setCanceledOnTouchOutside(false);
		dialog2.setTitle("成績表示");
		dialog2.setView(webView);
		// アラートダイアログの否定ボタンがクリックされた時に呼び出されるコールバックリスナーを登録します
		dialog2.setButton("閉じる", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog2.dismiss();
				mpPlayer.start();
			}
		});
		dialog2.show();
	}

	// /////////成績表示ビュー//////////////////////////////////////////////////////////

	// /////////サーバー送信//////////////////////////////////////////////////////////
	public String doGet() {
		try {
			HttpGet method = new HttpGet(
					"http://160.28.60.103/study/senddata.php?userid="
							+ pref.getString("userid", "") + "&acc_y=" + value
							+ "&kaisu=" + repeatNokogiriTime + "&ptime="
							+ practicTime / 5 + "&power="
							+ (int) (LAarray[3] / practicTime) + "&gyro="
							+ value_2 + "&burekaisu=" + buregoukei / 10
							+ "&kakudo=" + sendroll + "&typeid=1");
			DefaultHttpClient client = new DefaultHttpClient();

			HttpResponse response = client.execute(method);
			/*
			 * int status = response.getStatusLine().getStatusCode(); if (
			 * status != HttpStatus.SC_OK ) throw new Exception( "" );
			 */return EntityUtils.toString(response.getEntity(), "UTF-8");
		} catch (Exception e) {
			return null;
		}
	}

	public void finishdo() {
		// 終了を押下時
		StartFlag = false;
		topBtn.setText("練習を始める");
		handler.removeCallbacks(runnable);
		Log.d("miss", String.valueOf(missGyro0));
		Log.d("miss", String.valueOf(missGyro1));
		Log.d("miss", String.valueOf(missGyro2));
		showDialog();
		mpPlayer.start();
		doGet();
		// サーバー送信/////////////////////////////////////////////////////
		syokika();
	}

	public void syokika() {
		// 初期化
		flagfinish = false;
		missGyro0 = 0;
		missGyro1 = 0;
		missGyro2 = 0;
		practicTime = 0;
		repeatNokogiriTime = 0;
		LAarray[3] = 0;
		repeatNokogiriTimeFlag = false;
		mpGyro = MediaPlayer.create(this, R.raw.bure);
		mpCount = MediaPlayer.create(this, R.raw.hiki);
		mpPlayer = MediaPlayer.create(this, R.raw.taiko);
		LAvalue = "0,0,0";
		GYROvalueStr = "0,0,0";
		missGyro0 = 0;
		missGyro1 = 0;
		missGyro2 = 0;
		flagkaisu = 20;
		tv2.setText("");
		buregoukei = 0;
	}
}