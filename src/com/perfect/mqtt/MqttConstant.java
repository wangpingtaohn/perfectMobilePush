package com.perfect.mqtt;


public class MqttConstant {
	
	
	public static final String      TAG = "perfect_mqtt_push";
	//brokerIP
	public static final String 		MQTT_HOST = "58.215.52.161";
	
	//broker端口
	public static final int			MQTT_BROKER_PORT_NUM = 1883;
	
	public static final String 		MQTTCONNSPEC = "tcp://" + MQTT_HOST + ":" + MQTT_BROKER_PORT_NUM;
	
	public static final short 		MQTT_KEEP_ALIVE = 60 * 15;
	
	public static final String 		MQTT_APP_KEY = "appKey";
	
	public static final int 		MQTT_TIME_OUT = 10 * 1000;
	
	public static final String		PREF_DEVICE_ID = "deviceID";
	
	public static final String		ACTION_START = MQTT_APP_KEY + ".START";
	
	public static final String		ACTION_STOP = MQTT_APP_KEY + ".STOP";
	
	public static final String		ACTION_KEEPALIVE = MQTT_APP_KEY + ".KEEP_ALIVE";
	
	public static final String		ACTION_RECONNECT = MQTT_APP_KEY + ".RECONNECT";
	
	public static final long        KEEP_ALIVE_INTERVAL = 1000 * 60 * 28;
	
	public static final long		INITIAL_RETRY_INTERVAL = 1000 * 10;
	
	public static final long		MAXIMUM_RETRY_INTERVAL = 1000 * 60 * 30;
	
	public static final String		NOTIFICATION_PUSH_ACTION = "android.intent.action.perfect.push";
	
	public static final String      PREF_STARTED = "isStarted";
	
	public static final String      PREF_RETRY = "retryInterval";
	
	public static final String      USER_NAME = "userName";
	
	public static final String      PASSWORD = "password";
	
	public static final String      CONNECT = "connect";
	
	public static final String      UNSUBSCRIBLE = "unSubscrible";
	
	public static final String      SUBSCRIBLE = "subscrible";
	
	public static final String      DISCONNECT = "disConnect";
	
	public static final String      UNREGISTER = "unRegister";
	
	public static final String      EXECUTE_RECEIVER_ACTION = "android.intent.action.execute";
	
	public static final String      EXECUTE_RECEIVER_EXTRA = "execute_extra";
	
	public static final String      TOPIC = "topic";
	
}
