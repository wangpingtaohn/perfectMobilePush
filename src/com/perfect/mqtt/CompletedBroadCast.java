package com.perfect.mqtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class CompletedBroadCast extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			
			Log.i("wpt", "CompletedBroadCast_ACTION_BOOT_COMPLETED");
			SharedPreferences spf = context.getSharedPreferences(
					MqttConstant.TAG, Context.MODE_PRIVATE);
			String userName = spf.getString(MqttConstant.USER_NAME, "");
			String password = spf.getString(MqttConstant.PASSWORD, "");
			String topic = null;
//			String[] str = { "news", "shop" };
			MqttConnection.getInstance().init(context, topic, userName, password);

		}

	}

}
