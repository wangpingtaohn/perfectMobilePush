package com.perfect.mqtt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.telephony.TelephonyManager;

/**
 * @author we
 *
 */
/**
 * @author we
 * 
 */
public class MqttConnection {

	private static MqttConnection instance;

	private MqttConnection() {
	}

	public static MqttConnection getInstance() {
		synchronized (MqttConnection.class) {
			if (instance == null) {
				instance = new MqttConnection();
			}
		}

		return instance;
	}

	public void init(Context context, String initTopic, String userName, String passwd) {

		String[] topics = null;
		if (null != initTopic) {
			topics = new String[]{initTopic};
		}
		init(context, topics, userName, passwd);
	}

	public void init(Context context, String[] initTopic, String userName, String passwd) {
		Editor editor = context.getSharedPreferences(MqttConstant.TAG,
				Context.MODE_PRIVATE).edit();
		// 设备id
		TelephonyManager sManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String mDeviceID = sManager.getDeviceId();
		if (null != mDeviceID && !"".equals(mDeviceID)) {
			try {
				editor.putString(MqttConstant.PREF_DEVICE_ID,
						MD5.getMd5(mDeviceID, 16).substring(0, 10));
				editor.commit();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (null != userName && !"".equals(userName)) {
			editor.putString(MqttConstant.USER_NAME, userName);
			editor.commit();
		}
		if (null != passwd && !"".equals(passwd)) {
			editor.putString(MqttConstant.PASSWORD, passwd);
			editor.commit();
		}

		// 应用名称
		String[] sendInitTopic = null;
		if (null != initTopic) {
			String appkey = getAppkey(context);
			editor.putString(MqttConstant.MQTT_APP_KEY, appkey);
			editor.commit();
			sendInitTopic = new String[initTopic.length];
			for (int i = 0; i < initTopic.length; i++) {
				String topic = initTopic[i];
				if (null != topic && !"".equals(topic)) {
					sendInitTopic[i] = appkey + "/" + topic;
				}
			}
		}
//		PushService.actionStart(context, sendInitTopic);
		startPushService(context, sendInitTopic);

	}
	//启动连接服务器的service
	private void startPushService(Context context, String[] topics) {
		Intent intent = new Intent(context, PushService.class);
		intent.setAction(MqttConstant.ACTION_START);
		intent.putExtra(MqttConstant.TOPIC, topics);
		context.startService(intent);
	}

	/**
	 * 停止连接
	 */
	public void stopConnect(Context context) {
//		PushService.actionStop(context);
		sendBroadCastReceiver(context, MqttConstant.ACTION_STOP, null);
	}

	/**
	 * 根据主题订阅的相应主题
	 */
	public void subscribeTopic(Context context, String initTopic) {
		// 应用名称
		subscribeTopic(context, new String[] { initTopic });
	}

	/**
	 * 根据主题数组订阅的相应主题
	 */
	public void subscribeTopic(Context context, String[] initTopic) {

		// 应用名称
		String[] sendInitTopic = null;
		if (null != initTopic) {
			String appkey = getAppkey(context);
			sendInitTopic = new String[initTopic.length];
			for (int i = 0; i < initTopic.length; i++) {
				String topic = initTopic[i];
				if (null != topic && !"".equals(topic)) {
					sendInitTopic[i] = appkey + "/" + topic;
				}
			}
		}
		sendBroadCastReceiver(context, MqttConstant.SUBSCRIBLE, sendInitTopic);
	}

	/**
	 * 根据主题取消相应的订阅主题
	 */
	public void unSubscribe(Context context, String initTopic) {
		unSubscribe(context, new String[] { initTopic });
	}

	/**
	 * 根据主题取消相应的订阅主题数组
	 */
	public void unSubscribe(Context context, String[] initTopic) {
		String appkey = getAppkey(context);
		if (null != initTopic) {
			String[] cancelTopic = new String[initTopic.length];
			for (int i = 0; i < initTopic.length; i++) {
				cancelTopic[i] = appkey + "/" + initTopic[i];
			}
			sendBroadCastReceiver(context, MqttConstant.UNSUBSCRIBLE, cancelTopic);
		}
	}

	/**
	 * 获取manifest.xml里的meta-data appkey
	 */
	private String getAppkey(Context context) {
		String appValue = "";
		try {
			ApplicationInfo info = context.getPackageManager()
					.getApplicationInfo(context.getPackageName(),
							PackageManager.GET_META_DATA);
			appValue = info.metaData.getString(MqttConstant.MQTT_APP_KEY);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		return appValue;
	}
	
	private void sendBroadCastReceiver(Context context, String extraActionKey, String[] topics) {
		Intent intent = new Intent(MqttConstant.EXECUTE_RECEIVER_ACTION);
		intent.putExtra(MqttConstant.EXECUTE_RECEIVER_EXTRA,extraActionKey);
		intent.putExtra(MqttConstant.TOPIC, topics);
		context.sendBroadcast(intent);
	}
	
	public void unResgistBroadCast(Context context) {
		sendBroadCastReceiver(context, MqttConstant.UNREGISTER, null);
	}

}
