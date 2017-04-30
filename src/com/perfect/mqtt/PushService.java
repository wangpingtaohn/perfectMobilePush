package com.perfect.mqtt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import com.ibm.micro.client.mqttv3.MqttCallback;
import com.ibm.micro.client.mqttv3.MqttClient;
import com.ibm.micro.client.mqttv3.MqttConnectOptions;
import com.ibm.micro.client.mqttv3.MqttDeliveryToken;
import com.ibm.micro.client.mqttv3.MqttException;
import com.ibm.micro.client.mqttv3.MqttMessage;
import com.ibm.micro.client.mqttv3.MqttSecurityException;
import com.ibm.micro.client.mqttv3.MqttTopic;

public class PushService extends Service {

	private boolean mStarted;

	private SharedPreferences mPrefs;

	private MQTTConnection mConnection;

	private long mStartTime;

	private String[] sendInitTopic;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i("wpt", "onCreate");
	}

	@Override
	public void onDestroy() {
		Log.i("wpt", "onDestroy");
		if (mStarted == true) {
			stop();
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.i("wpt", "onStart");
		
		mPrefs = getSharedPreferences(MqttConstant.TAG, MODE_PRIVATE);
		mStartTime = System.currentTimeMillis();
		if (null != intent) {
			sendInitTopic = intent.getStringArrayExtra(MqttConstant.TOPIC);
			Log.i("wpt", "intent_aciont=" + intent.getAction());
			if (intent.getAction().equals(MqttConstant.ACTION_START) == true) {
				if (wasStarted()) {
					stopKeepAlives();
				}
				start();
			} else if (intent.getAction().equals(MqttConstant.ACTION_KEEPALIVE) == true) {
				keepAlive();
			} else if (intent.getAction().equals(MqttConstant.ACTION_RECONNECT) == true) {
				if (isNetworkAvailable()) {
					reconnectIfNecessary();
				}
			}
		} else {
			start();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private boolean wasStarted() {
		return mPrefs.getBoolean(MqttConstant.PREF_STARTED, false);
	}

	private void setStarted(boolean started) {
		mPrefs.edit().putBoolean(MqttConstant.PREF_STARTED, started).commit();
		mStarted = started;
	}

	private synchronized void start() {
		Log.i("wpt", "Starting service...");

		if (mStarted == true) {
			Log.i("wpt", "Attempt to start connection that is already active");
			return;
		}

		connect();
		
		registerReceiver(mConnectivityChanged, new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION));
		registerReceiver(executeBroadCast, new IntentFilter(
				MqttConstant.EXECUTE_RECEIVER_ACTION));
	}

	private synchronized void stop() {
		Log.i("wpt", "stop()_mStarted=" + mStarted);
		if (mStarted == false) {
			Log.w(MqttConstant.TAG, "Attempt to stop connection not active.");
			return;
		}

		setStarted(false);

		unregisterReceiver(mConnectivityChanged);
		try {
			unregisterReceiver(executeBroadCast);
		} catch (Exception e) {
			e.printStackTrace();
		}
		cancelReconnect();

		if (mConnection != null) {
			// mConnection.disconnect();
			new ConnAsyncTask().execute(MqttConstant.DISCONNECT);
			// mConnection = null;
		}
	}

	//
	private synchronized void connect() {
		Log.i("wpt", "Connecting...");
		String deviceID = mPrefs.getString(MqttConstant.PREF_DEVICE_ID, null);
		if (deviceID == null) {
			Log.i("wpt", "Device ID not found...");
		} else {
			try {
				mConnection = new MQTTConnection();
			} catch (MqttException e) {
				Log.e(MqttConstant.TAG, "MqttException", e);
				if (isNetworkAvailable()) {
					scheduleReconnect(mStartTime);
				}
			}
			setStarted(true);
		}
	}

	private synchronized void keepAlive() {
		try {
			if (mStarted == true && mConnection != null) {
				Log.i("wpt", "keepAlive...");
				mConnection.sendKeepAlive();
			}
		} catch (MqttException e) {
			// mConnection.disconnect();
			new ConnAsyncTask().execute(MqttConstant.DISCONNECT);
			mConnection = null;
			cancelReconnect();
		}
	}

	private void startKeepAlives() {
		Log.i("wpt", "startKeepAlives()...");
		Intent i = new Intent();
		i.setClass(this, PushService.class);
		i.setAction(MqttConstant.ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + MqttConstant.KEEP_ALIVE_INTERVAL,
				MqttConstant.KEEP_ALIVE_INTERVAL, pi);
	}

	// Remove all scheduled keep alives
	private void stopKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, PushService.class);
		i.setAction(MqttConstant.ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.cancel(pi);
	}

	// We schedule a reconnect based on the starttime of the service
	public void scheduleReconnect(long startTime) {
		// the last keep-alive interval
		long interval = mPrefs.getLong(MqttConstant.PREF_RETRY,
				MqttConstant.INITIAL_RETRY_INTERVAL);

		// Calculate the elapsed time since the start
		long now = System.currentTimeMillis();
		long elapsed = now - startTime;

		// Set an appropriate interval based on the elapsed time since start
		if (elapsed < interval) {
			interval = Math.min(interval * 4,
					MqttConstant.MAXIMUM_RETRY_INTERVAL);
		} else {
			interval = MqttConstant.INITIAL_RETRY_INTERVAL;
		}

		// Save the new internval
		mPrefs.edit().putLong(MqttConstant.PREF_RETRY, interval).commit();

		// Schedule a reconnect using the alarm manager.
		Intent i = new Intent();
		i.setClass(this, PushService.class);
		i.setAction(MqttConstant.ACTION_RECONNECT);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.set(AlarmManager.RTC_WAKEUP, now + interval, pi);
	}

	// Remove the scheduled reconnect
	public void cancelReconnect() {
		Log.i("wpt", "****cancelReconnect()*****");
		Intent i = new Intent();
		i.setClass(this, PushService.class);
		i.setAction(MqttConstant.ACTION_RECONNECT);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.cancel(pi);
	}

	private synchronized void reconnectIfNecessary() {
		if (mStarted == true && mConnection == null) {
			Log.i("wpt", "****reconnectIfNecessary()*****");
			connect();
			// new ConnAsyncTask().execute(MqttConstant.CONNECT);
		}
	}

	// This receiver listeners for network changes and updates the MQTT
	// connection
	private BroadcastReceiver mConnectivityChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get network info
			NetworkInfo info = (NetworkInfo) intent
					.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

			// Is there connectivity?
			boolean hasConnectivity = (info != null && info.isConnected()) ? true
					: false;

			Log.i("wpt", "Connectivity changed: connected=" + hasConnectivity);

			if (hasConnectivity) {
				reconnectIfNecessary();
			} else if (mConnection != null) {
				// if there no connectivity, make sure MQTT connection is
				// destroyed
				// mConnection.disconnect();
				new ConnAsyncTask().execute(MqttConstant.DISCONNECT);
				cancelReconnect();
				mConnection = null;
			}
		}
	};

	// Check if we are online
	private boolean isNetworkAvailable() {
		ConnectivityManager mConnMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo info = mConnMan.getActiveNetworkInfo();
		if (info == null) {
			return false;
		}
		return info.isConnected();
	}

	// This inner class is a wrapper on top of MQTT client.
	private class MQTTConnection implements MqttCallback {
		MqttClient mqttClient = null;

		// Creates a new connection given the broker address and initial topic
		public MQTTConnection() throws MqttException {

			Log.i("wpt", "MQTTConnection()...");

			String clientID = mPrefs.getString(MqttConstant.MQTT_APP_KEY, "")
					+ "/" + mPrefs.getString(MqttConstant.PREF_DEVICE_ID, "");
			Log.i("wpt", "MQTTConnection()_clientID=" + clientID);
			Log.i("wpt", "MQTTConnection()_mqttconnspec="
					+ MqttConstant.MQTTCONNSPEC);

			mqttClient = new MqttClient(MqttConstant.MQTTCONNSPEC, clientID,
					null);
			MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();

			mqttConnectOptions.setCleanSession(false);
			mqttConnectOptions.setConnectionTimeout(MqttConstant.MQTT_TIME_OUT);
			mqttConnectOptions
					.setKeepAliveInterval(MqttConstant.MQTT_KEEP_ALIVE);
			String userName = mPrefs.getString(MqttConstant.USER_NAME, "");
			String password = mPrefs.getString(MqttConstant.PASSWORD, "");
			if (null != userName) {
				mqttConnectOptions.setUserName(MD5.getSecurityInfo(userName));
			}
			if (null != password) {
				mqttConnectOptions.setPassword(MD5.getSecurityInfo(password)
						.toCharArray());
			}
			mqttClient.setCallback(this);
			// mqttClient.connect(mqttConnectOptions);

			Log.i("wpt", "MQTTConnection()_sendInitTopic=" + sendInitTopic);
			if (null != sendInitTopic && sendInitTopic.length > 0) {
				new ConnAsyncTask().execute(MqttConstant.CONNECT, mqttClient,
						mqttConnectOptions, sendInitTopic);
				// subscribeToTopic(sendInitTopic);
			} else {
				new ConnAsyncTask().execute(MqttConstant.CONNECT, mqttClient,
						mqttConnectOptions);
			}

			// Save start time
			mStartTime = System.currentTimeMillis();
			// Star the keep-alives
			startKeepAlives();
		}

		// Disconnect
		public void disconnect() {
			try {
				stopKeepAlives();
				mqttClient.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/*
		 * Send a request to the message broker to be sent messages published
		 * with the specified topic name. Wildcards are allowed.
		 */
		public void subscribeToTopic(String[] topicName) throws MqttException {
			Log.i("wpt", "##########subscribeToTopic#######");
			// Log.i("wpt", "topicName=" + topicName.toString());
			if ((mqttClient == null) || (mqttClient.isConnected() == false)) {
				Log.i("wpt", "subscribeToTopic=" + "No connection");
			} else {
				// String[] topics = { topicName };
				if (null != topicName && !"".equals(topicName)) {
					int[] topicQOS = new int[topicName.length];
					for (int i = 0; i < topicQOS.length; i++) {
						Log.i("wpt", "subscribeToTopic_topicName="
								+ topicName[i]);
						topicQOS[i] = 2;
					}
					mqttClient.subscribe(topicName, topicQOS);
				}
			}
		}

		private void unSubscribeToTopic(String[] topics) {
			if ((mqttClient == null) || (mqttClient.isConnected() == false)) {
				Log.i("wpt", "Connection error" + "No connection");
			} else {
				try {
					Log.i("wpt", "unSubscribeToTopic");
					mqttClient.unsubscribe(topics);
				} catch (MqttException e) {
					e.printStackTrace();
				}
			}
		}

		/*
		 * Sends a message to the message broker, requesting that it be
		 * published to the specified topic.
		 */
		private void publishToTopic(String topicName, String message)
				throws MqttException {
			Log.i("wpt", "##########publishToTopic#######");
			if ((mqttClient == null) || (mqttClient.isConnected() == false)) {
				Log.i("wpt", "No connection to public to");
			} else {
				MqttTopic mqttTopic = mqttClient.getTopic(topicName);
				MqttMessage mqttMessage = new MqttMessage(message.getBytes());
				mqttMessage.setQos(1);
				mqttMessage.setRetained(false);
				mqttTopic.publish(mqttMessage);
			}
		}

		public void sendKeepAlive() throws MqttException {
			Log.i("wpt", "sendKeepAlive()...");
			// publish to a keep-alive topic
			publishToTopic(mPrefs.getString(MqttConstant.MQTT_APP_KEY, "")
					+ "/keepalive",
					mPrefs.getString(MqttConstant.PREF_DEVICE_ID, ""));
		}

		@Override
		public void connectionLost(Throwable arg0) {

			Log.i("wpt", "Loss of connection" + "connection downed");
			stopKeepAlives();
			// null itself
			mConnection = null;
			if (isNetworkAvailable() == true) {
				reconnectIfNecessary();
			}

		}

		@Override
		public void deliveryComplete(MqttDeliveryToken mt) {
			Log.i("wpt", "deliveryComplete:" + mt.isComplete());
		}

		@Override
		public void messageArrived(MqttTopic topic, MqttMessage message)
				throws Exception {
			Intent intent = new Intent(MqttConstant.NOTIFICATION_PUSH_ACTION);
			intent.putExtra("topic", topic.getName());
			intent.putExtra("message", message.toString());
			sendBroadcast(intent);
			Log.i("wpt", "topic=" + topic.getName());
		}

	}

	private class ConnAsyncTask extends AsyncTask<Object, String, String> {

		@Override
		protected String doInBackground(Object... params) {
			String pStr = (String) params[0];
			Log.i("wpt_conAsyncTask_params=", pStr);
			if (MqttConstant.CONNECT.equals(pStr)) {
				MqttClient client = (MqttClient) params[1];
				MqttConnectOptions options = (MqttConnectOptions) params[2];
				try {
					client.connect(options);
					if (params.length > 3) {
						Log.i("wpt", "params.length=" + params.length);
						String[] topics = (String[]) params[3];
						Log.i("wpt", "params[3]=" + topics);
						try {
							if (null != mConnection) {
								mConnection.subscribeToTopic(topics);
							}
						} catch (MqttException e) {
							e.printStackTrace();
						}
					}
				} catch (MqttSecurityException e) {
					e.printStackTrace();
				} catch (MqttException e) {
					e.printStackTrace();
				}
				// connect();
			} else if (MqttConstant.UNSUBSCRIBLE.equals(pStr)) {
				String[] topic = (String[]) params[1];
				if (null != mConnection) {
					mConnection.unSubscribeToTopic(topic);
				}
			} else if (MqttConstant.SUBSCRIBLE.equals(pStr)) {
				String[] topic = (String[]) params[1];
				if (null != mConnection) {
					try {
						mConnection.subscribeToTopic(topic);
					} catch (MqttException e) {
						e.printStackTrace();
					}
				}
			} else if (MqttConstant.DISCONNECT.equals(pStr)) {
				if (null != mConnection) {
					mConnection.disconnect();
					mConnection = null;
				}
			}
			return pStr;
		}

	}

	private BroadcastReceiver executeBroadCast = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String extraStr = intent
					.getStringExtra(MqttConstant.EXECUTE_RECEIVER_EXTRA);
			String[] topics = intent
					.getStringArrayExtra(MqttConstant.TOPIC);
			//取消订阅
			if (MqttConstant.UNSUBSCRIBLE.equals(extraStr)) {
				new ConnAsyncTask().execute(MqttConstant.UNSUBSCRIBLE, topics);
			//订阅
			} else if (MqttConstant.SUBSCRIBLE.equals(extraStr)) {
				new ConnAsyncTask().execute(MqttConstant.SUBSCRIBLE, topics);
			//注销广播
			} else if (MqttConstant.UNREGISTER.equals(extraStr)) {
				unregisterReceiver(executeBroadCast);
			//停止服务及连接
			} else if (MqttConstant.ACTION_STOP.equals(extraStr)) {
				stop();
				stopSelf();
			}
		}

	};

}