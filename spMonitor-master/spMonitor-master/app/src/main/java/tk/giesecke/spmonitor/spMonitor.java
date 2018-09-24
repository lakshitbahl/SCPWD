package tk.giesecke.spmonitor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** spMonitor - Main UI activity
 *
 * Shows life or logged data from the spMonitor
 *
 */
public class spMonitor extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {

	/** Access to shared preferences of application*/
	public static SharedPreferences mPrefs;
	/** Application context */
	public static Context appContext;
	/** The view of the main UI */
	public static View appView;
	/** Flag for last month update request */
	private static boolean needLastMonth = false;

	/** The url to access the spMonitor device */
	private String url = "";
	/** The ip address to access the spMonitor device */
	public static String deviceIP = "no IP saved";
	/** A HTTP client to access the spMonitor device */
	public static final OkHttpClient client = new OkHttpClient();
	/** Flag for external network access */
	private boolean isWAN = false;
	/** Flag for external network access */
	private boolean isWANonStart = false;
	/** SSID of WiFi */
	private String connSSID = null;

	/** Pointer to text view for results */
	private static TextView resultTextView;
	/** Flag if UI auto refresh is on or off */
	private static boolean autoRefreshOn = true;

	/** MPAndroid chart view for the current chart */
	private static LineChart lineChart;
	/** LineData for the plot */
	private LineData plotData;
	/** List to hold the timestamps for the chart from a log file */
	private static final ArrayList<String> timeSeries = new ArrayList<>();
	/** List to hold the measurements of the solar panel for the chart from a log file */
	private static final ArrayList<Entry> solarSeries = new ArrayList<>();
	/** List to hold the measurement of the consumption for the chart from a log file */
	private static final ArrayList<Entry> consPSeries = new ArrayList<>();
	/** List to hold the measurement of the consumption for the chart from a log file */
	private static final ArrayList<Entry> consMSeries = new ArrayList<>();
	/** List to hold the measurement of the light for the chart from a log file */
	private static final ArrayList<Entry> lightSeries = new ArrayList<>();
	/** List to hold the timestamps for a continuously updated chart */
	public static final ArrayList<String> timeStamps = new ArrayList<>();
	/** List to hold the measurements of the solar panel for a continuously updated chart */
	public static final ArrayList<Float> solarPower = new ArrayList<>();
	/** List to hold the measurement of the consumption for a continuously updated chart */
	public static final ArrayList<Float> consumPPower = new ArrayList<>();
	/** List to hold the measurement of the consumption for a continuously updated chart */
	public static final ArrayList<Float> consumMPower = new ArrayList<>();
	/** List to hold the measurement of the light for a continuously updated chart */
	public static final ArrayList<Long> lightValue = new ArrayList<>();
	/** List to hold the timestamps for a chart from logged data */
	public static final ArrayList<String> timeStampsCont = new ArrayList<>();
	/** List to hold the measurements of the solar panel for a chart from logged data */
	public static final ArrayList<Float> solarPowerCont = new ArrayList<>();
	/** List to hold the measurement of the consumption for a chart from logged data */
	public static final ArrayList<Float> consumPPowerCont = new ArrayList<>();
	/** List to hold the measurement of the consumption for a chart from logged data */
	public static final ArrayList<Float> consumMPowerCont = new ArrayList<>();
	/** List to hold the measurement of the light for a chart from logged data */
	public static final ArrayList<Long> lightValueCont = new ArrayList<>();
	/** Line data set for solar data */
	private LineDataSet solar;
	/** Line data set for consumption data */
	private LineDataSet consP;
	/** Line data set for consumption data */
	private LineDataSet consM;
	/** Line data set for light data */
	private LineDataSet light;

	/** Array with existing log dates on the Arduino */
	private static final List<String> logDates = new ArrayList<>();
	/** Pointer to current displayed log in logDates array */
	private static int logDatesIndex = 0;
	/** Array with existing log dates on the Arduino */
	private static final List<String> lastLogDates = new ArrayList<>();
	/** Pointer to current displayed log in logDates array */
	private static int lastLogDatesIndex = 0;
	/** Flag for showing last month */
	private static boolean showingLast = false;
	/** Flag for showing a log */
	public static boolean showingLog = false;

	/** Day stamp of data */
	public static String dayToShow;

	/** Today's year-month database name */
	private static String[] dbNamesList = new String[2];

	/** Solar power received from spMonitor device as minute average */
	private static Float solarPowerMin = 0.0f;
	/** Solar energy generated up to now on the displayed day */
	public static float solarEnergy = 0.0f;
	/** Consumption received from spMonitor device as minute average */
	private static Float consPowerMin = 0.0f;
	/** Consumed energy generated up to now on the displayed day */
	public static float consEnergy = 0.0f;
	/** Light received from spMonitor device as minute average */
	private long lightValMin = 0;
	/** Solar power received from spMonitor device as minute average */
	private static Float lastSolarPowerMin = 0.0f;
	/** Consumption received from spMonitor device as minute average */
	private static Float lastConsPowerMin = 0.0f;
	/** Light received from spMonitor device as minute average */
	private long lastLightValMin = 0;
	/** Solar power received from spMonitor device as 5 seconds average */
	private static Float solarPowerSec = 0.0f;
	/** Consumption received from spMonitor device as 5 seconds average */
	private static Float consPowerSec = 0.0f;
	/** Light received from spMonitor device as 5 seconds average */
	private long lightValSec = 0;
	/** Flag for showing solar power data */
	private static boolean showSolar = true;
	/** Flag for showing consumption data */
	private static boolean showCons = true;
	/** Flag for showing light data */
	private static boolean showLight = false;

	/** Instance of dialog */
	private Dialog menuDialog;
	/** Flag for simple UI layout */
	private boolean isSimpleUI = false;

	/** Array list with available alarm names */
	private ArrayList<String> notifNames = new ArrayList<>();
	/** Array list with available alarm uri's */
	private ArrayList<String> notifUri = new ArrayList<>();
	/** Selected alarm name */
	private String notifNameSel = "";
	/** Selected alarm uri */
	private String notifUriSel = "";

	/** Receiver for result from SyncService */
	public static DataServiceResponse syncServiceReceiver = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPrefs = getSharedPreferences("spMonitor", 0);
		if (!mPrefs.getBoolean("simpleUI",false)) {
			setContentView(R.layout.sp_monitor);
			isSimpleUI = false;
			mPrefs.edit().putBoolean("simpleUI",false).apply();
		} else {
			setContentView(R.layout.sp_simple);
			isSimpleUI = true;
			mPrefs.edit().putBoolean("simpleUI",true).apply();
		}

		// Enable access to internet
		if (android.os.Build.VERSION.SDK_INT > 9) {
			/** ThreadPolicy to get permission to access internet */
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		appContext = this;
		appView = getWindow().getDecorView().findViewById(android.R.id.content);

		mPrefs = getSharedPreferences("spMonitor", 0);
		resultTextView = (TextView) findViewById(R.id.tv_result);
		deviceIP = mPrefs.getString("spMonitorIP", "no IP saved");
		isWAN = mPrefs.getBoolean("access_type", false);
		connSSID = mPrefs.getString("SSID", "none");

		// Check if broadcast receiver and timers are already initialized
		if (BroadcastRegisterService.mReceiver == null) {
			if (BuildConfig.DEBUG) Log.d("spMonitor","EventReceiver was not registered");
			// Start service to register BroadcastRegisterService
			this.startService(new Intent(this, BroadcastRegisterService.class));
		} else {
			if (BuildConfig.DEBUG) Log.d("spMonitor","EventReceiver already registered");
		}

		/** Pointer to text views showing the consumed / produced energy */
		TextView energyText = (TextView) findViewById(R.id.tv_cons_energy);
		energyText.setVisibility(View.INVISIBLE);
		energyText = (TextView) findViewById(R.id.tv_solar_energy);
		energyText.setVisibility(View.INVISIBLE);

		/** Button to stop/start continuous UI refresh */
		Button btStop = (Button) findViewById(R.id.bt_stop);
		if (showingLog) {
			btStop.setTextColor(getResources().getColor(android.R.color.holo_green_light));
			btStop.setText(getResources().getString(R.string.start));
		}

		/** Button to close app when long click => daydream) */
		Button btClose = (Button) findViewById(R.id.bt_close);
		btClose.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				Utilities.startDayDreaming(appContext);
				return true;
			}
		});

		notifUriSel = "android.resource://"
				+ this.getPackageName() + "/"
				+ R.raw.alert;

		// Get today's day for the online database name
		dbNamesList = Utilities.getDateStrings();
		if (BuildConfig.DEBUG) Log.d("spMonitor","This month = " + dbNamesList[0]);
		if (BuildConfig.DEBUG) Log.d("spMonitor","Last month = " + dbNamesList[1]);

		// In case the database is not yet existing, open it once
		/** Instance of DataBaseHelper */
		DataBaseHelper dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME);
		/** Instance of data base */
		SQLiteDatabase dataBase = dbHelper.getReadableDatabase();
		dataBase.close();
		dbHelper.close();
		/** Instance of DataBaseHelper */
		dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME_LAST);
		/** Instance of data base */
		dataBase = dbHelper.getReadableDatabase();
		dataBase.close();
		dbHelper.close();

		// In case the database is not yet existing, open it once
		/** Instance of DataBaseHelper */
		dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME_LAST);
		/** Instance of data base */
		dataBase = dbHelper.getReadableDatabase();
		dataBase.close();
		dbHelper.close();

		if (!deviceIP.equalsIgnoreCase(getResources().getString(R.string.no_device_ip)) && !isWAN) {
			/** Buttons to be enabled when on wifi */
			Button uiButton = (Button) findViewById(R.id.bt_sync);
			uiButton.setEnabled(true);
			uiButton.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
			uiButton = (Button) findViewById(R.id.bt_status);
			uiButton.setEnabled(true);
			uiButton.setTextColor(getResources().getColor(android.R.color.holo_blue_light));

			Utilities.startRefreshAnim();
			String syncedMonth = mPrefs.getString("synced_month", "");
			if (!syncedMonth.equalsIgnoreCase(dbNamesList[0])) {
				deleteDatabase(DataBaseHelper.DATABASE_NAME);
				deleteDatabase(DataBaseHelper.DATABASE_NAME_LAST);
				mPrefs.edit().putString("synced_month", dbNamesList[0]).apply();
				/** Instance of DataBaseHelper */
				dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME);
				/** Instance of data base */
				dataBase = dbHelper.getReadableDatabase();
				dataBase.close();
				dbHelper.close();
				/** Instance of DataBaseHelper */
				dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME_LAST);
				/** Instance of data base */
				dataBase = dbHelper.getReadableDatabase();
				dataBase.close();
				dbHelper.close();
			}
//			Utilities.startStopUpdates(appContext,false);

			// Get missing values from spMonitor device
			new syncDBtoDB().execute(dbNamesList[0]);

			// Check if we have already synced the last month
			/** Instance of DataBaseHelper */
			dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME_LAST);
			/** Instance of data base */
			dataBase = dbHelper.getReadableDatabase();
			/** Cursor with data from database */
			Cursor dbCursor = DataBaseHelper.getLastRow(dataBase);
			if (dbCursor != null) {
				if (dbCursor.getCount() == 0) { // local database is empty, need to sync all data
					needLastMonth = true;
				} else { // fill last log file array
					lastLogDates.clear();
					/** List with years in the database */
					ArrayList<Integer> yearsAvail = DataBaseHelper.getEntries(dataBase, "year", 0, 0);
					for (int year = 0; year < yearsAvail.size(); year++) {
						/** List with months of year in the database */
						ArrayList<Integer> monthsAvail = DataBaseHelper.getEntries(dataBase, "month",
								0, yearsAvail.get(year));
						for (int month = 0; month < monthsAvail.size(); month++) {
							/** List with days of month of year in the database */
							ArrayList<Integer> daysAvail = DataBaseHelper.getEntries(dataBase, "day",
									monthsAvail.get(month),
									yearsAvail.get(year));
							for (int day = 0; day < daysAvail.size(); day++) {
								lastLogDates.add(("00" + String.valueOf(yearsAvail.get(year)))
										.substring(String.valueOf(yearsAvail.get(year)).length()) +
										"-" + ("00" + String.valueOf(monthsAvail.get(month)))
										.substring(String.valueOf(monthsAvail.get(month)).length()) +
										"-" + ("00" + String.valueOf(daysAvail.get(day)))
										.substring(String.valueOf(daysAvail.get(day)).length()));
							}
						}
					}
					lastLogDatesIndex = lastLogDates.size() - 1;
				}
			}
			if (dbCursor != null) {
				dbCursor.close();
			}
			dataBase.close();
			dbHelper.close();
		} else {
			if (isWAN) {
				resultTextView.setText(getString(R.string.on_WAN));
				/** Buttons to be disabled when on mobile network */
				Button uiButton = (Button) findViewById(R.id.bt_sync);
				uiButton.setEnabled(false);
				uiButton.setTextColor(getResources().getColor(android.R.color.darker_gray));
				uiButton = (Button) findViewById(R.id.bt_status);
				uiButton.setEnabled(false);
				uiButton.setTextColor(getResources().getColor(android.R.color.darker_gray));

				// Get an initial value
				Utilities.startRefreshAnim();
				/** String list with parts of the URL */
				String[] ipValues = deviceIP.split("/");
				url = "http://"+ipValues[2]+"/data/get";
				isWANonStart = true;
				// Queue up 2 readings from the spMonitor device to have some initial data
				new callArduino().execute(url);
				new callArduino().execute(url);
			} else {
				resultTextView.setText(getString(R.string.err_no_device));
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(syncServiceReceiver);
		syncServiceReceiver = null;
	}

	@Override
	public void onResume() {
		super.onResume();

		IntentFilter filter = new IntentFilter(DataServiceResponse.ACTION_RESP);
		filter.addCategory(Intent.CATEGORY_DEFAULT);
		syncServiceReceiver = new DataServiceResponse();
		registerReceiver(syncServiceReceiver, filter);

		// Initiate first UI update
		Intent serviceIntent = new Intent(this, UpdateService.class);
		serviceIntent.putExtra("InitialCall","InitialCall");
		startService(serviceIntent);	}

	@Override
	public void onClick(View v) {
		url = "";
		client.setConnectTimeout(30, TimeUnit.SECONDS); // connect timeout
		client.setReadTimeout(30, TimeUnit.SECONDS);    // socket timeout
		/** Button to go to previous  log */
		Button prevButton  = (Button) findViewById(R.id.bt_prevLog);
		/** Button to go to next log */
		Button nextButton  = (Button) findViewById(R.id.bt_nextLog);

		switch (v.getId()) {
			case R.id.bt_prevLog:
				if (logDatesIndex == 0) {
					if ((lastLogDatesIndex == lastLogDates.size()-1) && !showingLast) {
						lastLogDatesIndex++;
					}
					showingLast = true;
				} else {
					showingLast = false;
				}
				if (!showingLast) { // use this months database
					if (logDatesIndex > 0) {
						Utilities.startRefreshAnim();
						logDatesIndex--;
						/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = (Button) findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/** String list with requested date info */
						String[] requestedDate = logDates.get(logDatesIndex).substring(0, 8).split("-");
						/** Instance of DataBaseHelper */
						DataBaseHelper dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME);
						/** Instance of data base */
						SQLiteDatabase dataBase = dbHelper.getReadableDatabase();

						/** Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						Utilities.fillSeries(newDataSet);
						initChart(false);
						newDataSet.close();
						dataBase.close();
						dbHelper.close();

						nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
						Utilities.stopRefreshAnim();
					}
				} else { // use last months database
					if (lastLogDatesIndex > 0) {
						Utilities.startRefreshAnim();
						lastLogDatesIndex--;
						/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = (Button) findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/** String list with requested date info */
						String[] requestedDate = lastLogDates.get(lastLogDatesIndex).substring(0, 8).split("-");
						/** Instance of DataBaseHelper */
						DataBaseHelper dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME_LAST);
						/** Instance of data base */
						SQLiteDatabase dataBase = dbHelper.getReadableDatabase();

						/** Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						Utilities.fillSeries(newDataSet);
						initChart(false);
						newDataSet.close();
						dataBase.close();
						dbHelper.close();

						if (lastLogDatesIndex == 0) {
							prevButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
						} else {
							prevButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
						}
						nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
						Utilities.stopRefreshAnim();
					}
				}
				break;
			case R.id.bt_nextLog:
				if (lastLogDatesIndex == lastLogDates.size()-1) {
					if ((logDatesIndex == 0) && showingLast) {
						logDatesIndex--;
					}
					showingLast = false;
				} else {
					showingLast = true;
				}
				if (!showingLast) { // use this months database
					if (logDatesIndex < logDates.size()-1) {
						Utilities.startRefreshAnim();
						logDatesIndex++;
						/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = (Button) findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/** String list with requested date info */
						String[] requestedDate = logDates.get(logDatesIndex).substring(0, 8).split("-");
						/** Instance of DataBaseHelper */
						DataBaseHelper dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME);
						/** Instance of data base */
						SQLiteDatabase dataBase = dbHelper.getReadableDatabase();

						/** Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						Utilities.fillSeries(newDataSet);
						initChart(false);
						newDataSet.close();
						dataBase.close();
						dbHelper.close();

						if (logDatesIndex == logDates.size()-1) {
							nextButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
						} else {
							nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
						}
						prevButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
						Utilities.stopRefreshAnim();
					}
				} else { // use last months database
					if (lastLogDatesIndex < lastLogDates.size()-1) {
						Utilities.startRefreshAnim();
						lastLogDatesIndex++;
						/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = (Button) findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/** String list with requested date info */
						String[] requestedDate = lastLogDates.get(lastLogDatesIndex).substring(0, 8).split("-");
						/** Instance of DataBaseHelper */
						DataBaseHelper dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME_LAST);
						/** Instance of data base */
						SQLiteDatabase dataBase = dbHelper.getReadableDatabase();

						/** Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						Utilities.fillSeries(newDataSet);
						initChart(false);
						newDataSet.close();
						dataBase.close();
						dbHelper.close();

						nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
						Utilities.stopRefreshAnim();
					}
				}
				break;
			case R.id.bt_stop:
				if (autoRefreshOn) {
					/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
					Button stopButton = (Button) findViewById(R.id.bt_stop);
					stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
					stopButton.setText(getResources().getString(R.string.start));
					autoRefreshOn = false;
				} else {
					if (showingLog) {
						showingLog = false;
						if (!isWAN) {
							Utilities.startRefreshAnim();
							//new syncDBtoDB().execute(dbNamesList[0]);
							Intent msgIntent = new Intent(this, SyncService.class);
							msgIntent.putExtra("START_FROM_UI", "true");
							startService(msgIntent);
						}

						/** Pointer to text views showing the consumed / produced energy */
						TextView energyText = (TextView) findViewById(R.id.tv_cons_energy);
						energyText.setVisibility(View.INVISIBLE);
						energyText = (TextView) findViewById(R.id.tv_solar_energy);
						energyText.setVisibility(View.INVISIBLE);

						logDatesIndex = logDates.size()-1;
						nextButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
					}
					/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
					Button stopButton = (Button) findViewById(R.id.bt_stop);
					stopButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
					stopButton.setText(getResources().getString(R.string.stop));
					autoRefreshOn = true;
				}
				break;
			case R.id.bt_status:
				// TODO Maybe this can be removed now complete
				client.setConnectTimeout(5, TimeUnit.MINUTES); // connect timeout
				client.setReadTimeout(5, TimeUnit.MINUTES);    // socket timeout
				url = deviceIP + "e";
				break;
			case R.id.bt_close:
				finish();
				break;
			case R.id.bt_sync:
				// TODO change to refresh the database complete
				// => delete current local databases
				// => read all data for this and last month again
				if (!showingLog && !isWAN) {
					Utilities.startRefreshAnim();
					Intent msgIntent = new Intent(this, SyncService.class);
					msgIntent.putExtra("START_FROM_UI", "true");
					startService(msgIntent);
				}
				break;
			case R.id.cb_solar:
				/** Checkbox to show or hide solar graph */
				CheckBox cbSolar = (CheckBox)findViewById(R.id.cb_solar);
				if (cbSolar.isChecked()) {
					solar.setVisible(true);
					showSolar = true;
				} else {
					solar.setVisible(false);
					showSolar = false;
				}
				// let the chart know it's data has changed
				lineChart.notifyDataSetChanged();
				lineChart.invalidate();
				break;
			case R.id.cb_cons:
				/** Checkbox to show or hide consumption graph */
				CheckBox cbCons = (CheckBox)findViewById(R.id.cb_cons);
				if (cbCons.isChecked()) {
					consP.setVisible(true);
					consM.setVisible(true);
					showCons = true;
				} else {
					consP.setVisible(false);
					consM.setVisible(false);
					showCons = false;
				}
				// let the chart know it's data has changed
				lineChart.notifyDataSetChanged();
				lineChart.invalidate();
				break;
			case R.id.cb_light:
				/** Checkbox to show or hide light graph */
				CheckBox cbLight = (CheckBox)findViewById(R.id.cb_light);
				if (cbLight.isChecked()) {
					light.setVisible(true);
					showLight = true;
					TextView tvToHide = (TextView)findViewById(R.id.tv_light);
					tvToHide.setVisibility(View.VISIBLE);
					tvToHide = (TextView)findViewById(R.id.tv_light_value);
					tvToHide.setVisibility(View.VISIBLE);
				} else {
					light.setVisible(false);
					showLight = false;
					TextView tvToHide = (TextView)findViewById(R.id.tv_light);
					tvToHide.setVisibility(View.INVISIBLE);
					tvToHide = (TextView)findViewById(R.id.tv_light_value);
					tvToHide.setVisibility(View.INVISIBLE);
				}
				// let the chart know it's data has changed
				lineChart.notifyDataSetChanged();
				lineChart.invalidate();
				break;
			case R.id.bt_backup:
				// TODO just call backupDataBase directly for backup
				client.setConnectTimeout(5, TimeUnit.MINUTES); // connect timeout
				client.setReadTimeout(5, TimeUnit.MINUTES);    // socket timeout
				url = deviceIP + "b";
				menuDialog.dismiss();
				break;
			case R.id.bt_dream:
				Utilities.startDayDreaming(appContext);
				menuDialog.dismiss();
				break;
			case R.id.bt_switch_net:
				if (isWAN) {
					isWAN = false;
					/** Buttons to be disabled when on mobile network */
					Button uiButton = (Button) findViewById(R.id.bt_sync);
					uiButton.setEnabled(true);
					uiButton.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
					uiButton = (Button) findViewById(R.id.bt_status);
					uiButton.setEnabled(true);
					uiButton.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
				} else {
					isWAN = true;
					/** Buttons to be disabled when on mobile network */
					Button uiButton = (Button) findViewById(R.id.bt_sync);
					uiButton.setEnabled(false);
					uiButton.setTextColor(getResources().getColor(android.R.color.darker_gray));
					uiButton = (Button) findViewById(R.id.bt_status);
					uiButton.setEnabled(false);
					uiButton.setTextColor(getResources().getColor(android.R.color.darker_gray));
				}
				mPrefs.edit().putBoolean("access_type",isWAN).apply();
				menuDialog.dismiss();
				break;
			case R.id.bt_set_alarm:
				notifNames = new ArrayList<>();
				notifUri = new ArrayList<>();
				notifNames.add(getString(R.string.no_alarm_sel));
				notifUri.add("");
				notifNames.add(getString(R.string.dev_alarm_sel));
				notifUri.add("android.resource://"
						+ this.getPackageName() + "/"
						+ R.raw.alert);
				/** Index of last user selected alarm tone */
				int uriIndex = Utilities.getNotifSounds(this, notifNames, notifUri) + 2;
				menuDialog.dismiss();

				/** Builder for alarm sound selection dialog */
				AlertDialog.Builder alarmSelBuilder = new AlertDialog.Builder(appContext);
				/** Inflater for restore file selection dialog */
				LayoutInflater alarmSelInflater = (LayoutInflater) appContext.getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);
				/** View for restore file selection dialog */
				@SuppressLint("InflateParams") View alarmListView = alarmSelInflater
						.inflate(R.layout.alarm_dialog, null);
				alarmSelBuilder.setView(alarmListView);
				/** Pointer to restore file selection dialog */
				AlertDialog alarmList = alarmSelBuilder.create();
				alarmList.setTitle(appContext.getString(R.string.alarm_diag_title));

				alarmList.setButton(AlertDialog.BUTTON_POSITIVE, appContext.getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								if (!notifNameSel.equalsIgnoreCase("")) {
									mPrefs.edit().putString("alarmUri", notifUriSel).apply();
								}
								dialog.dismiss();
							}
						});

				alarmList.setButton(AlertDialog.BUTTON_NEGATIVE, appContext.getString(android.R.string.cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								// nothing to do here, just close the dialog
								dialog.dismiss();
							}
						});
				alarmList.show();

				/** Pointer to list view with the alarms */
				ListView lvAlarmList = (ListView) alarmListView.findViewById(R.id.lv_AlarmList);
				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
						appContext,
						android.R.layout.simple_list_item_single_choice,
						notifNames );
				lvAlarmList.setAdapter(arrayAdapter);
				lvAlarmList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
					public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					                               int pos, long id) {
						/** Instance of media player */
						MediaPlayer mMediaPlayer = new MediaPlayer();
						try {
							mMediaPlayer.setDataSource(appContext, Uri.parse(notifUri.get(pos)));
							final AudioManager audioManager = (AudioManager) appContext
									.getSystemService(Context.AUDIO_SERVICE);
							if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
								mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
								mMediaPlayer.prepare();
								mMediaPlayer.start();
							}
						} catch (IOException e) {
							if (BuildConfig.DEBUG) Log.d("spMonitor", "Cannot play alarm");
						}
						return true;
					}
				});
				lvAlarmList.setOnItemClickListener(this);
				lvAlarmList.setItemChecked(uriIndex, true);
				lvAlarmList.setSelection(uriIndex);
				break;
			case R.id.bt_enable_notif:
				boolean isNotifOn = mPrefs.getBoolean("notif",true);
				if (isNotifOn) {
					// Instance of notification manager to cancel the existing notification */
					NotificationManager nMgr = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
					nMgr.cancel(1);
					mPrefs.edit().putBoolean("notif",false).apply();
//					Utilities.startStopUpdates(this, false);
				} else {
					mPrefs.edit().putBoolean("notif",true).apply();
//					Utilities.startStopUpdates(this, true);
				}
				menuDialog.dismiss();
				break;
			case R.id.bt_sw_ui:
				if (mPrefs.getBoolean("simpleUI",false)) {
					mPrefs.edit().putBoolean("simpleUI", false).apply();
				} else {
					mPrefs.edit().putBoolean("simpleUI", true).apply();
				}
				menuDialog.dismiss();
				recreate();
				break;
			case R.id.bt_menu_cancel:
				menuDialog.dismiss();
				break;
		}

		if (!url.isEmpty()) {
			new callArduino().execute(url);
		}
	}

	/**
	 * Handle hardware key events to catch press of menu and back keys
	 * which are handled within this app (since menu key will not open side bar)
	 *
	 * @param keyCode
	 *            Code of pressed key
	 * @param event
	 *            context
	 *
	 * @return boolean
	 *            Result of super.onKeyUp(keyCode, event)
	 */
	public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
		switch (keyCode) {
			//*******************************************************
			// Use hardware menu key to open menu dialog
			//*******************************************************
			case KeyEvent.KEYCODE_MENU:
				/** Alert dialog builder to show dialog for manual IP address input */
				menuDialog = new Dialog(this);
				menuDialog.setContentView(R.layout.menu_dialog);
				/** Button inside the dialog */
				Button dialogButton = (Button) menuDialog.findViewById(R.id.bt_backup);
				dialogButton.setOnClickListener(this);
				dialogButton.setEnabled(true);
				dialogButton = (Button) menuDialog.findViewById(R.id.bt_dream);
				dialogButton.setOnClickListener(this);
				dialogButton.setEnabled(true);
				dialogButton = (Button) menuDialog.findViewById(R.id.bt_sw_ui);
				dialogButton.setOnClickListener(this);
				if (!mPrefs.getBoolean("simpleUI",false)) {
					dialogButton.setText(getString(R.string.bt_simple_UI));
				} else {
					dialogButton.setText(getString(R.string.bt_graph_UI));
				}
				dialogButton.setEnabled(true);
				dialogButton = (Button) menuDialog.findViewById(R.id.bt_menu_cancel);
				dialogButton.setOnClickListener(this);
				dialogButton = (Button) menuDialog.findViewById(R.id.bt_switch_net);
				dialogButton.setOnClickListener(this);
				if (isWAN) {
					dialogButton.setText(getString(R.string.bt_switch_LAN_txt));
				} else {
					dialogButton.setText(getString(R.string.bt_switch_WAN_txt));
				}
				dialogButton = (Button) menuDialog.findViewById(R.id.bt_enable_notif);
				dialogButton.setOnClickListener(this);
				if (mPrefs.getBoolean("notif",true)) {
					dialogButton.setText(getString(R.string.bt_disable_notif_txt));
				} else {
					dialogButton.setText(getString(R.string.bt_enable_notif_txt));
				}
				dialogButton = (Button) menuDialog.findViewById(R.id.bt_set_alarm);
				dialogButton.setOnClickListener(this);
				if (isWAN) {
					dialogButton = (Button) menuDialog.findViewById(R.id.bt_backup);
					dialogButton.setEnabled(false);
					dialogButton = (Button) menuDialog.findViewById(R.id.bt_dream);
					dialogButton.setEnabled(false);
					dialogButton = (Button) menuDialog.findViewById(R.id.bt_sw_ui);
					dialogButton.setEnabled(false);
				}
				menuDialog.show();
				break;
			//*******************************************************
			// Catch return key event to close UI activity
			//*******************************************************
			case KeyEvent.KEYCODE_BACK:
				finish();
				break;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		notifNameSel = notifNames.get(position);
		notifUriSel = notifUri.get(position);
	}

	/**
	 * Async task class to contact Arduino part of the spMonitor device
	 */
	private class callArduino extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {

			/** URL to be called */
			String urlString=params[0]; // URL to call
			/** Response from the spMonitor device or error message */
			String resultToDisplay = "";

			connSSID = Utilities.getSSID(getApplicationContext());

			if (isWAN) {
				if ((connSSID != null) && (connSSID.equalsIgnoreCase(mPrefs.getString("SSID","none")))) {
					isWAN= false;
					mPrefs.edit().putBoolean("access_type",isWAN).apply();
				} else {
					urlString = "http://www.spmonitor.giesecke.tk/l.php";
				}
			}
			if (BuildConfig.DEBUG) Log.d("spMonitor","callArduino = " + urlString);

			/** Request to spMonitor device */
			Request request = new Request.Builder()
					.url(urlString)
					.build();

			if (request != null) {
				try {
					/** Response from spMonitor device */
					Response response = client.newCall(request).execute();
					if (response != null) {
						resultToDisplay = response.body().string();
					}
				} catch (IOException e) {
					resultToDisplay = e.getMessage();
					isWAN = true;
					mPrefs.edit().putBoolean("access_type",isWAN).apply();
					try {
						if (resultToDisplay.contains("EHOSTUNREACH")) {
							resultToDisplay = getApplicationContext().getString(R.string.err_arduino);
						}
						if (resultToDisplay.equalsIgnoreCase("")) {
							resultToDisplay = getApplicationContext().getString(R.string.err_arduino);
						}
						return resultToDisplay;
					} catch (NullPointerException en) {
						resultToDisplay = getResources().getString(R.string.err_no_device);
						return resultToDisplay;
					}
				}
			}

			if (resultToDisplay.equalsIgnoreCase("")) {
				resultToDisplay = getApplicationContext().getString(R.string.err_arduino);
			}
			return resultToDisplay;
		}

		protected void onPostExecute(String result) {
			updateUI(result);
		}
	}

	/**
	 * Wrapper to send 2 parameters to onPostExecute of AsyncTask
	 */
	public class onPostExecuteWrapper {
		public String taskResult;
		public String syncMonth;
	}

	/**
	 * Async task class to contact Linino part of the spMonitor device
	 * and sync spMonitor database with local Android database
	 */
	private class syncDBtoDB extends AsyncTask<String, String, onPostExecuteWrapper> {

		@Override
		protected onPostExecuteWrapper doInBackground(String... params) {

			/** Return values for onPostExecute */
			onPostExecuteWrapper result = new onPostExecuteWrapper();

			/** Which month to sync */
			result.syncMonth = params[0];

			/** Response from the spMonitor device or error message */
			result.taskResult = result.syncMonth + " " + getResources().getString(R.string.filesSyncFail);

			// Make call only if valid url is given
			if (deviceIP.startsWith("No")) {
				result.taskResult = getResources().getString(R.string.err_no_device);
			} else {
				/** String list with parts of the URL */
				String[] ipValues = deviceIP.split("/");
				/** URL to be called */
				String urlString = "http://"+ipValues[2]+"/sd/spMonitor/query2.php"; // URL to call

				// Check for last entry in the local database
				/** Instance of DataBaseHelper */
				DataBaseHelper dbHelper;
				if (result.syncMonth.equalsIgnoreCase(dbNamesList[0])) {
					dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME);
				} else {
					dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME_LAST);
				}
				/** Instance of data base */
				SQLiteDatabase dataBase = dbHelper.getReadableDatabase();
				/** Cursor with data from database */
				Cursor dbCursor = DataBaseHelper.getLastRow(dataBase);
				/** Flag for database access type */
				boolean splitAccess = false;
				if (dbCursor != null) {
					if (dbCursor.getCount() != 0) { // local database not empty, need to sync only missing
						dbCursor.moveToFirst();

						int lastMinute =  dbCursor.getInt(4);
						int lastHour = dbCursor.getInt(3);
						int lastDay = dbCursor.getInt(2);

						urlString += "?date=" + dbCursor.getString(0); // add year
						urlString += "-" + ("00" +
								dbCursor.getString(1)).substring(dbCursor.getString(1).length()); // add month
						urlString += "-" + ("00" +
								String.valueOf(lastDay))
								.substring(String.valueOf(lastDay).length()); // add day
						urlString += "-" + ("00" +
								String.valueOf(lastHour))
								.substring(String.valueOf(lastHour).length()); // add hour
						urlString += ":" + ("00" +
								String.valueOf(lastMinute))
								.substring(String.valueOf(lastMinute).length()); // add minute
						urlString += "&get=all";
					} else { // local database is empty, need to sync all data
						splitAccess = true;
						urlString += "?date=" + result.syncMonth;
					}
				} else { // something went wrong with the database access
					result.taskResult = result.syncMonth + " " + getResources().getString(R.string.filesSyncFail);
					dataBase.close();
					dbHelper.close();
					return result;
				}
				dbCursor.close();
				dataBase.close();
				dbHelper.close();

				/** Repeat counter used when full database needs to be synced */
				int loopCnt = 0;
				/** URL used for access */
				String thisURL = urlString;
				if (splitAccess) {
					loopCnt = 3;
				}

				for (int loop = 0; loop <= loopCnt; loop++) {
					if (splitAccess) {
						urlString = thisURL + "-" + String.valueOf(loop);
						if (BuildConfig.DEBUG) Log.d("spMonitor","URL = " + urlString);
					}
					// Set timeout to 5 minutes in case we have a lot of data to load
					client.setConnectTimeout(5, TimeUnit.MINUTES); // connect timeout
					client.setReadTimeout(5, TimeUnit.MINUTES);    // socket timeout
					/** Request to spMonitor device */
					Request request = new Request.Builder()
							.url(urlString)
							.build();

					if (request != null) {
						try {
							/** Response from spMonitor device */
							Response response = client.newCall(request).execute();
							if (response != null) {
								result.taskResult = response.body().string();
							}
						} catch (IOException e) {
							e.printStackTrace();
							result.taskResult = e.getMessage();
							try {
								if (result.taskResult.contains("EHOSTUNREACH")) {
									result.taskResult = getApplicationContext().getString(R.string.err_arduino);
								}
								if (result.taskResult.equalsIgnoreCase("")) {
									result.taskResult = getApplicationContext().getString(R.string.err_arduino);
								}
								return result;
							} catch (NullPointerException en) {
								result.taskResult = getResources().getString(R.string.err_no_device);
								return result;
							}
						}

						if (BuildConfig.DEBUG) Log.d("spMonitor","JSON size = " + result.taskResult.length());

						try {
							/** JSON array with the data received from spMonitor device */
							JSONArray jsonFromDevice = new JSONArray(result.taskResult);
							if (result.syncMonth.equalsIgnoreCase(dbNamesList[0])) {
								dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME);
							} else {
								dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME_LAST);
							}
							/** Instance of data base */
							dataBase = dbHelper.getWritableDatabase();
							// Get received data into local database
							/** Data string for insert into database */
							String record = "";
							try {
								dataBase.beginTransactionNonExclusive();
								for (int i=0; i<jsonFromDevice.length(); i++) {
									// skip first data record from device if we are just updating the database
									if (i == 0 && !splitAccess) i++;
									/** JSONObject with a single record */
									JSONObject jsonRecord = jsonFromDevice.getJSONObject(i);
									record = jsonRecord.getString("d");
									record = record.replace("-",",");
									record += ","+jsonRecord.getString("l");
									record += ","+jsonRecord.getString("s");
									record += ","+jsonRecord.getString("c");
									if (BuildConfig.DEBUG && i <= 1) Log.d("spMonitor","DB insert: " + record);
									DataBaseHelper.addDay(dataBase, record);
								}
								dataBase.setTransactionSuccessful();
								dataBase.endTransaction();
								if (BuildConfig.DEBUG) Log.d("spMonitor","DB insert: " + record);
								result.taskResult = result.syncMonth + " " + getResources().getString(R.string.filesSynced);
							} catch (SQLiteDatabaseLockedException e) {
								result.taskResult = result.syncMonth + " " + getResources().getString(R.string.filesSyncFail1);
								dataBase.close();
								dbHelper.close();
							}
						} catch (JSONException e) {
							result.taskResult = result.syncMonth + " " + getResources().getString(R.string.filesSyncFail);
							//dataBase.endTransaction();
							dataBase.close();
							dbHelper.close();
						}
						dataBase.close();
						dbHelper.close();
					}
				}
			}
			return result;
		}

		protected void onPostExecute(onPostExecuteWrapper result) {
			updateSynced(result.taskResult, result.syncMonth);
			if (needLastMonth) {
				new syncDBtoDB().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dbNamesList[1]);
				needLastMonth = false;
//			} else {
//				isCommunicating = false;
//				Utilities.startStopUpdates(appContext,true);
			}
		}
	}

	/**
	 * Async task class to contact Linino part of the spMonitor device
	 */
	private class backupDataBase extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {

			// TODO need to change to get all existing databases from the spMonitor device
			// and backup all of them
			/** URL to be called */
			String urlString = params[0]; // URL to call

			/** Result message */
			String ioResult = getResources().getString(R.string.backupSaved);

			// Check if external SD card is available
			if (Utilities.isExternalStorageWritable()) {
				// Make call only if valid url is given
				if (!urlString.startsWith("No")) {
					File backupFile = Utilities.getExFileDir("spMonitor", true);
					if (backupFile != null) {
						backupFile = Utilities.getExFileDir("/spMonitor/s.bu.gz",false);
						if (backupFile != null) {
							/** IP address to connect to */
							String ip[] = urlString.split("/");
							/** Sftp session instance */
							Session session = null;

							try{
								/** Jsch instance */
								JSch sshChannel = new JSch();
								session = sshChannel.getSession("root", ip[2], 22);
								session.setPassword("spMonitor");
								session.setConfig("StrictHostKeyChecking", "no");
								session.connect();

								if (session.isConnected()) {
									/** Channel for session */
									Channel channel = session.openChannel("sftp");
									channel.connect();

									if (channel.isConnected()) {
										/** SFTP channel */
										ChannelSftp sftp = (ChannelSftp) channel;

										if (sftp.isConnected()) {
											sftp.cd("/mnt/sda1/");
											sftp.get("s.bu.gz", backupFile.getAbsolutePath());

											sftp.disconnect();
										}
										channel.disconnect();
									}
									session.disconnect();
								}

							} catch(JSchException | SftpException e){
								ioResult = e.getMessage();
								if (session != null) {
									session.disconnect();
								}
							}
						} else {
							ioResult = getResources().getString(R.string.noExtStorage);
						}
					} else {
						ioResult = getResources().getString(R.string.noExtStorage);
					}
				} else {
					ioResult = getResources().getString(R.string.err_no_device);
				}
			} else {
				ioResult = getResources().getString(R.string.noExtStorage);
			}
			return ioResult;
		}

		protected void onPostExecute(String result) {
		}
	}

	/**
	 * Update UI with values received from spMonitor device (Arduino part)
	 *
	 * @param value
	 *        result sent by spMonitor
	 */
	private void updateUI(final String value) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				/** Pointer to text views to be updated */
				TextView valueFields;
				/* String with results received from spMonitor device */
				String result;

				if (value.length() != 0) {
					if (url.endsWith("get") || isWAN) {
						// decode JSON
						if (Utilities.isJSONValid(value)) {
							try {
								if (!isWAN) {
									/** JSON object containing result from server */
									JSONObject jsonResult = new JSONObject(value);
									/** JSON object containing the values */
									JSONObject jsonValues = jsonResult.getJSONObject("value");

									try {
										solarPowerMin = Float.parseFloat(jsonValues.getString("S"));
										lastSolarPowerMin = solarPowerMin;
									} catch (Exception excError) {
										solarPowerMin = lastSolarPowerMin;
									}
									/** Temporary buffer for last read power value */
									Float oldPower = solarPowerSec;
									try {
										solarPowerSec = Float.parseFloat(jsonValues.getString("sr"));
									} catch (Exception excError) {
										solarPowerSec = oldPower;
									}
									try {
										consPowerMin = Float.parseFloat(jsonValues.getString("C"));
										lastConsPowerMin = consPowerMin;
									} catch (Exception excError) {
										consPowerMin = lastConsPowerMin;
									}
									oldPower = consPowerSec;
									try {
										consPowerSec = Float.parseFloat(jsonValues.getString("cr"));
									} catch (Exception excError) {
										consPowerSec = oldPower;
									}

									result = "S=" + String.valueOf(solarPowerMin) + "W ";
									result += "s=";
									try {
										result += jsonValues.getString("s");
									} catch (Exception excError) {
										result += "---";
									}
									result += "A sv=";
									try {
										result += jsonValues.getString("sv");
									} catch (Exception excError) {
										result += "---";
									}
									result += "V sr=";
									try {
										result += jsonValues.getString("sr");
									} catch (Exception excError) {
										result += "---";
									}
									result += "W sa=";
									try {
										result += jsonValues.getString("sa");
									} catch (Exception excError) {
										result += "---";
									}
									result += "W sp=";
									try {
										result += jsonValues.getString("sp");
									} catch (Exception excError) {
										result += "---";
									}
									result += "\nC=" + String.valueOf(consPowerMin) + "W c=";
									try {
										result += jsonValues.getString("c");
									} catch (Exception excError) {
										result += "---";
									}
									result += "A cv=";
									try {
										result += jsonValues.getString("cv");
									} catch (Exception excError) {
										result += "---";
									}
									result += "V cr=";
									try {
										result += jsonValues.getString("cr");
									} catch (Exception excError) {
										result += "---";
									}
									result += "W ca=";
									try {
										result += jsonValues.getString("ca");
									} catch (Exception excError) {
										result += "---";
									}
									result += "W cp=";
									try {
										result += jsonValues.getString("cp") + "\n";
									} catch (Exception excError) {
										result += "---" + "\n";
									}

									try {
										lightValMin = Long.parseLong(jsonValues.getString("L"));
										lastLightValMin = lightValMin;
									} catch (Exception excError) {
										lightValMin = lastLightValMin;
									}
									/** Temporary buffer for last read light value */
									long oldLight = lightValSec;
									try {
										lightValSec = Long.parseLong(jsonValues.getString("l"));
									} catch (Exception excError) {
										lightValSec = oldLight;
									}
								} else {
									/** JSON object containing result from server */
									JSONObject jsonResult = new JSONObject(value.substring(1, value.length() - 1));
									try {
										solarPowerMin = solarPowerSec = Float.parseFloat(jsonResult.getString("s"));
										lastSolarPowerMin = solarPowerMin;
									} catch (Exception excError) {
										solarPowerMin = solarPowerSec = lastSolarPowerMin;
									}
									try {
										consPowerMin = consPowerSec = Float.parseFloat(jsonResult.getString("c"));
										lastConsPowerMin = consPowerMin;
									} catch (Exception excError) {
										consPowerMin = consPowerSec = lastConsPowerMin;
									}
									try {
										lightValMin = lightValSec = Long.parseLong(jsonResult.getString("l"));
										lastLightValMin = lightValMin;
									} catch (Exception excError) {
										lightValMin = lightValSec = lastLightValMin;
									}
									result = "WAN! S="
											+ String.valueOf(solarPowerMin) + "W C= "
											+ String.valueOf(consPowerMin) + "W L= "
											+ String.valueOf(lightValMin) + "lux";
								}

								/** Double for the result of solar current and consumption used at 1min updates */
								double resultPowerMin = solarPowerMin + consPowerMin;

								valueFields = (TextView) findViewById(R.id.tv_solar_val);
								/** String for display */
								String displayTxt;
								displayTxt = String.format("%.0f", solarPowerMin) + "W";
								valueFields.setText(displayTxt);
								valueFields = (TextView) findViewById(R.id.tv_cons_val);
								displayTxt = String.format("%.0f", resultPowerMin) + "W";
								valueFields.setText(displayTxt);
								resultTextView.setText(result);

								valueFields = (TextView) findViewById(R.id.tv_result_txt);
								if (consPowerMin > 0.0d) {
									valueFields.setText(getString(R.string.tv_result_txt_im));
									valueFields = (TextView) findViewById(R.id.tv_result_val);
									valueFields.setTextColor(getResources()
											.getColor(android.R.color.holo_red_light));
								} else {
									valueFields.setText(getString(R.string.tv_result_txt_ex));
									valueFields = (TextView) findViewById(R.id.tv_result_val);
									valueFields.setTextColor(getResources()
											.getColor(android.R.color.holo_green_light));
								}
								displayTxt = String.format("%.0f", Math.abs(consPowerMin)) + "W";
								valueFields.setText(displayTxt);

								valueFields = (TextView) findViewById(R.id.tv_light_value);
								displayTxt = String.valueOf(lightValMin) + "lux";
								valueFields.setText(displayTxt);

								if (autoRefreshOn && !isSimpleUI) {
									if (isWANonStart) {
										isWANonStart = false;
										/** Integer list with today's date info */
										int[] requestedDate = Utilities.getCurrentDate();
										/** Instance of DataBaseHelper */
										DataBaseHelper dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME);
										/** Instance of data base */
										SQLiteDatabase dataBase = dbHelper.getReadableDatabase();

										/** Cursor with data from the database */
										Cursor newDataSet = DataBaseHelper.getLastRow(dataBase);
										newDataSet.moveToFirst();
										if (newDataSet.getInt(0) == requestedDate[0] - 2000 &&
												newDataSet.getInt(1) == requestedDate[1] &&
												newDataSet.getInt(2) == requestedDate[2]) {
											newDataSet.close();
											/** Cursor with data from the database */
											newDataSet = DataBaseHelper.getDay(dataBase,
													requestedDate[2], requestedDate[1], requestedDate[0] - 2000);
											Utilities.fillSeries(newDataSet);
										}
										newDataSet.close();
										dataBase.close();
										dbHelper.close();
										initChart(true);
									}

									/** Current time as string */
									String nowTime = Utilities.getCurrentTime();
									plotData.addXValue(nowTime);
									timeStampsCont.add(nowTime);
									solarSeries.add(new Entry(solarPowerMin, solarSeries.size()));
									solarPowerCont.add(solarPowerMin);
									if (consPowerMin < 0.0) {
										consPSeries.add(new Entry(consPowerMin, consPSeries.size()));
										consumPPowerCont.add(consPowerMin);
										consMSeries.add(new Entry(0, consMSeries.size()));
										consumMPowerCont.add(0.0f);
									} else {
										consMSeries.add(new Entry(consPowerMin, consMSeries.size()));
										consumMPowerCont.add(consPowerMin);
										consPSeries.add(new Entry(0, consPSeries.size()));
										consumPPowerCont.add(0.0f);
									}
									lightSeries.add(new Entry(lightValMin, lightSeries.size()));
									lightValueCont.add(lightValMin);

									/** Text view to show min and max poser values */
									TextView maxPowerText = (TextView) findViewById(R.id.tv_cons_max);
									displayTxt = "(" + String.format("%.0f",
											Collections.max(consumMPowerCont)) + "W)";
									maxPowerText.setText(displayTxt);
									maxPowerText = (TextView) findViewById(R.id.tv_solar_max);
									displayTxt = "(" + String.format("%.0f",
											Collections.max(solarPowerCont)) + "W)";
									maxPowerText.setText(displayTxt);

									// let the chart know it's data has changed
									lineChart.notifyDataSetChanged();
									lineChart.invalidate();
								}

								Utilities.stopRefreshAnim();
								return;
							} catch (Exception excError) {
								Utilities.stopRefreshAnim();
								return;
							}
						}
					} else if (url.endsWith("b")) {
						new backupDataBase().execute(deviceIP);
					}
					resultTextView.setText(value);
					Utilities.stopRefreshAnim();
					return;
				}
				if (isWAN) {
					/** Buttons to be disabled when on mobile network */
					Button uiButton = (Button) findViewById(R.id.bt_sync);
					uiButton.setEnabled(false);
					uiButton.setTextColor(getResources().getColor(android.R.color.darker_gray));
					uiButton = (Button) findViewById(R.id.bt_status);
					uiButton.setEnabled(false);
					uiButton.setTextColor(getResources().getColor(android.R.color.darker_gray));
				} else {
					/** Buttons to be enabled when on wifi */
					Button uiButton = (Button) findViewById(R.id.bt_sync);
					uiButton.setEnabled(true);
					uiButton.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
					uiButton = (Button) findViewById(R.id.bt_status);
					uiButton.setEnabled(true);
					uiButton.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
				}
				result = "\n";
				resultTextView.setText(result);
				Utilities.stopRefreshAnim();
			}
		});
	}

	/**
	 * Update UI with values received from spMonitor device (Linino part)
	 *
 	 * @param result
	 *        result sent by spMonitor
	 * @param syncMonth
	 *        Month that got synced
	 */
	private void updateSynced(final String result, final String syncMonth) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				resultTextView.setText(result);

				if (!showingLog) {
					/** Today split into 3 integers for the database query */
					int[] todayDate = Utilities.getCurrentDate();
					/** Instance of DataBaseHelper */
					DataBaseHelper dbHelper;
					/** Array with existing log dates on the Arduino */
					List<String> thisLogDates;

					if (syncMonth.equalsIgnoreCase(dbNamesList[0])) {
						dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME);
						thisLogDates = logDates;
					} else {
						dbHelper = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME_LAST);
						thisLogDates = lastLogDates;
					}
					/** Instance of data base */
					SQLiteDatabase dataBase = dbHelper.getReadableDatabase();
					/** Cursor with new data from the database */
					Cursor newDataSet = DataBaseHelper.getDay(dataBase, todayDate[2],
							todayDate[1], todayDate[0] - 2000);
					Utilities.fillSeries(newDataSet);
					newDataSet.close();
					thisLogDates.clear();
					/** List with years in the database */
					ArrayList<Integer> yearsAvail = DataBaseHelper.getEntries(dataBase, "year", 0, 0);
					for (int year = 0; year < yearsAvail.size(); year++) {
						/** List with months of year in the database */
						ArrayList<Integer> monthsAvail = DataBaseHelper.getEntries(dataBase, "month",
								0, yearsAvail.get(year));
						for (int month = 0; month < monthsAvail.size(); month++) {
							/** List with days of month of year in the database */
							ArrayList<Integer> daysAvail = DataBaseHelper.getEntries(dataBase, "day",
									monthsAvail.get(month),
									yearsAvail.get(year));
							for (int day = 0; day < daysAvail.size(); day++) {
								thisLogDates.add(("00" + String.valueOf(yearsAvail.get(year)))
										.substring(String.valueOf(yearsAvail.get(year)).length()) +
										"-" + ("00" + String.valueOf(monthsAvail.get(month)))
										.substring(String.valueOf(monthsAvail.get(month)).length()) +
										"-" + ("00" + String.valueOf(daysAvail.get(day)))
										.substring(String.valueOf(daysAvail.get(day)).length()));
							}
						}
					}

					dataBase.close();
					dbHelper.close();

					if (syncMonth.equalsIgnoreCase(dbNamesList[0])) {
						logDatesIndex = thisLogDates.size() - 1;
						initChart(true);
						Utilities.stopRefreshAnim();
					} else {
						lastLogDatesIndex = thisLogDates.size() - 1;
					}
				}
			}
		});
	}

	/**
	 * Initialize chart to show solar power, consumption and light values
	 *
	 * @param isContinuous
	 *          Flag for display mode
	 *          true = continuous display of data received from spMonitor
	 *          false = display content of a log file
	 */
	@SuppressLint("SimpleDateFormat")
	private void initChart(boolean isContinuous) {

		// Pointer to the chart in the layout
		lineChart = (LineChart) findViewById(R.id.graph);

		timeSeries.clear();
		solarSeries.clear();
		consPSeries.clear();
		consMSeries.clear();
		lightSeries.clear();
		if (!isContinuous) {
			for (int i=0; i<timeStamps.size(); i++) {
				timeSeries.add(timeStamps.get(i));
			}
			for (int i=0; i<solarPower.size(); i++) {
				solarSeries.add(new Entry(solarPower.get(i), i));
			}
			for (int i=0; i<consumPPower.size(); i++) {
				consPSeries.add(new Entry(consumPPower.get(i), i));
			}
			for (int i=0; i<consumMPower.size(); i++) {
				consMSeries.add(new Entry(consumMPower.get(i), i));
			}
			for (int i= 0; i<lightValue.size(); i++) {
				lightSeries.add(new Entry(lightValue.get(i), i));
			}
		} else {
			if (timeStampsCont.size() != 0) {
				for (int i=0; i<timeStampsCont.size(); i++) {
					timeSeries.add(timeStampsCont.get(i));
				}
				for (int i=0; i<solarPowerCont.size(); i++) {
					solarSeries.add(new Entry(solarPowerCont.get(i), i));
				}
				for (int i=0; i<consumPPowerCont.size(); i++) {
					consPSeries.add(new Entry(consumPPowerCont.get(i), i));
				}
				for (int i=0; i<consumMPowerCont.size(); i++) {
					consMSeries.add(new Entry(consumMPowerCont.get(i), i));
				}
				for (int i= 0; i<lightValueCont.size(); i++) {
					lightSeries.add(new Entry(lightValueCont.get(i), i));
				}
			}
		}
		/** Line data set for solar data */
		solar = new LineDataSet(solarSeries, "Solar");
		/** Line data set for consumption data */
		consP = new LineDataSet(consPSeries, "Export");
		/** Line data set for consumption data */
		consM = new LineDataSet(consMSeries, "Import");
		/** Line data set for light data */
		light = new LineDataSet(lightSeries, "Light");

		solar.setLineWidth(1.75f);
		solar.setCircleSize(0f);
		solar.setColor(0xFFFFBB33);
		solar.setCircleColor(0xFFFFBB33);
		solar.setHighLightColor(0xFFFFBB33);
		solar.setFillColor(0xAAFFBB33);
		if (showSolar) {
			solar.setVisible(true);
		} else {
			solar.setVisible(false);
		}
		solar.setDrawValues(false);
		solar.setDrawFilled(true);

		consP.setLineWidth(1.75f);
		consP.setCircleSize(0f);
		consP.setColor(Color.GREEN);
		consP.setCircleColor(Color.GREEN);
		consP.setHighLightColor(Color.GREEN);
		consP.setFillColor(0xAA00FF00);
		if (showCons) {
			consP.setVisible(true);
		} else {
			consP.setVisible(false);
		}
		consP.setDrawValues(false);
		consP.setDrawValues(false);
		consP.setDrawFilled(true);
		consP.setAxisDependency(YAxis.AxisDependency.LEFT);

		consM.setLineWidth(1.75f);
		consM.setCircleSize(0f);
		consM.setColor(Color.RED);
		consM.setCircleColor(Color.RED);
		consM.setHighLightColor(Color.RED);
		consM.setFillColor(0xAAFF0000);
		if (showCons) {
			consM.setVisible(true);
		} else {
			consM.setVisible(false);
		}
		consM.setDrawValues(false);
		consM.setDrawValues(false);
		consM.setDrawFilled(true);
		consM.setAxisDependency(YAxis.AxisDependency.LEFT);

		light.setLineWidth(1.75f);
		light.setCircleSize(0f);
		light.setColor(Color.WHITE);
		light.setCircleColor(Color.WHITE);
		light.setHighLightColor(Color.WHITE);
		if (showLight) {
			light.setVisible(true);
		} else {
			light.setVisible(false);
		}
		light.setDrawValues(false);
		light.setAxisDependency(YAxis.AxisDependency.RIGHT);

		/** Data set with data for the 4 plots */
		ArrayList<LineDataSet> dataSets = new ArrayList<>();
		dataSets.add(solar);
		dataSets.add(consP);
		dataSets.add(consM);
		dataSets.add(light);

		/** Data object with the data set and the y values */
		plotData = new LineData(timeSeries, dataSets);

		lineChart.setBackgroundColor(Color.BLACK);
		lineChart.setDrawGridBackground(false);
		lineChart.setTouchEnabled(true);
		lineChart.setDragEnabled(true);
		lineChart.setAutoScaleMinMaxEnabled(true);
		lineChart.setData(plotData);

		if (dayToShow != null) {
			TextView chartTitle = (TextView) findViewById(R.id.tv_plotTitle);

			Calendar c = Calendar.getInstance();
			@SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("yy-MM-dd");
			try {
				Date myDate = df.parse(dayToShow.trim());
				c.setTime(myDate);
				df = new SimpleDateFormat("yyyy-MMM-dd");
				dayToShow = df.format(c.getTime());
			} catch (ParseException ignore) {
			}

			chartTitle.setText(dayToShow);
		}

		/** Instance of left y axis */
		YAxis lYAx = lineChart.getAxisLeft();
		lYAx.setEnabled(true);
		lYAx.setTextColor(Color.WHITE);
		lYAx.setStartAtZero(false);
		lYAx.setSpaceTop(1);
		lYAx.setSpaceBottom(1);

		/** Instance of right y axis */
		YAxis rYAx = lineChart.getAxisRight();
		rYAx.setEnabled(true);
		rYAx.setTextColor(Color.WHITE);
		rYAx.setStartAtZero(false);
		rYAx.setSpaceTop(1);
		rYAx.setSpaceBottom(1);
		/** Hide right axis */
		rYAx.setDrawLabels(false);
		rYAx.setEnabled(false);

		/** Instance of x axis */
		XAxis xAx = lineChart.getXAxis();
		xAx.setEnabled(true);
		xAx.setTextColor(Color.WHITE);
		xAx.setPosition(XAxis.XAxisPosition.BOTTOM);

		lineChart.getLegend().setEnabled(false);

		// create a custom MarkerView (extend MarkerView) and specify the layout
		// to use for it
		/** Instance of custom marker view handler */
		CustomMarkerView mv = new CustomMarkerView(appContext);
		lineChart.setMarkerView(mv);

		// set the marker to the chart
		lineChart.setMarkerView(mv);

		// let the chart know it's data has changed
		lineChart.notifyDataSetChanged();
		lineChart.invalidate();
	}

	/**
	 * Show time, consumption and solar power when user touches a data point
	 *
	 */
	public class CustomMarkerView extends MarkerView {

		/** Pointer to text view for time */
		private final TextView tvMarkerTime;
		/** Pointer to text view for consumption */
		private final TextView tvMarkerCons;
		/** Pointer to text view for solar power */
		private final TextView tvMarkerSolar;

		public CustomMarkerView(Context context) {
			super(context, R.layout.plot_marker);
			/** Text view for time in marker */
			tvMarkerTime = (TextView) findViewById(R.id.tv_marker_time);
			/** Text view for consumption in marker */
			tvMarkerCons = (TextView) findViewById(R.id.tv_marker_cons);
			/** Text view for solar power in marker */
			tvMarkerSolar = (TextView) findViewById(R.id.tv_marker_solar);
		}

		// callbacks every time the MarkerView is redrawn, can be used to update the
		// content (user-interface)
		@Override
		public void refreshContent(Entry e, Highlight highlight) {
			/** Index for the series at the touched data point */
			int dataIndex = highlight.getXIndex();
			/** Entry with data of solar power at given index */
			Entry touchSolar = solarSeries.get(dataIndex);
			/** Entry with data of consumption at given index */
			Entry touchCons = consMSeries.get(dataIndex);
			if (touchCons.getVal() == 0) {
				touchCons = consPSeries.get(dataIndex);
			}

			tvMarkerTime.setText(timeSeries.get(dataIndex));
			/** Text for update text view */
			String updateTxt = (Float.toString(touchCons.getVal())+"W");
			tvMarkerCons.setText(updateTxt);
			updateTxt = (Float.toString(touchSolar.getVal())+"W");
			tvMarkerSolar.setText(updateTxt);
		}

		@Override
		public int getXOffset() {
			// this will center the marker-view horizontally
			return -(getWidth() / 2);
		}

		@Override
		public int getYOffset() {
			// this will cause the marker-view to be above the selected value
			return -getHeight();
		}
	}

	/**
	 * Receiver for both Update and Sync service results
	 *
	 */
	public class DataServiceResponse extends BroadcastReceiver {
		public static final String ACTION_RESP =
				"SYNC_FINISHED";

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra("startRefresh") && autoRefreshOn) {
				Utilities.startRefreshAnim();
				return;
			}
			if (intent.hasExtra("resultString") && autoRefreshOn) {
				url = "get";
				updateUI(intent.getStringExtra("resultString"));
			} else {
				resultTextView.setText(getResources().getString(R.string.filesSyncFinished));
			}
			initChart(true);
			Utilities.stopRefreshAnim();
		}
	}
}
