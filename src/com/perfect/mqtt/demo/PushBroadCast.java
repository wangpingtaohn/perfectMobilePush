package com.perfect.mqtt.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class PushBroadCast extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		String topic = intent.getStringExtra("topic");
		String message = intent.getStringExtra("message");
		
		Toast.makeText(context, "topic=" + topic + ",message=" + message, Toast.LENGTH_LONG).show();

	}

}
