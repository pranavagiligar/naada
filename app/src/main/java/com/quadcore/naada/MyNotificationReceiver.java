package com.quadcore.naada;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public class MyNotificationReceiver extends BroadcastReceiver {

	PlayerService playerService;
	NotificationManager nm;
	NotificationCompat.Builder builder;

	@Override
	public void onReceive(Context context, Intent intent) {
		nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if(intent.getAction().equals("com.quadcore.naada.NOTIFICATION_PLAY")) {
			Intent serviceIntent = new Intent(context,PlayerService.class);
			PlayerService.LocalBinder binder = (PlayerService.LocalBinder) peekService(context, serviceIntent);
			playerService = binder.getServiceInstance();
			playerService.mainActivity.onClick(playerService.mainActivity.play);
			
			////////////////////////////////////////////////////
			Intent intentt = new Intent(context, MainActivity.class);
			intentt.setAction("com.quadcore.naada.NOTIFICATION");
			PendingIntent pi = PendingIntent.getActivity(context, 0, intentt, 0);
			
			Intent intentPlay = new Intent("com.quadcore.naada.NOTIFICATION_PLAY");
			PendingIntent piPlay = PendingIntent.getBroadcast(context, 0, intentPlay, 0);
			
			String contentText = (String)playerService.mainActivity.tvSongTitle.getText();
			builder = new NotificationCompat.Builder(context)
				.setContentTitle(contentText)
				.setSmallIcon(R.drawable.notification_icon2)
				.setOngoing(true)
				.setAutoCancel(true)
				.setContentIntent(pi)
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.setProgress(100,playerService.mainActivity.songProgressBar.getProgress(),false);
			//////////////////////////////////////////////////
			
			if(playerService.mainActivity.isPaused) {
				builder.setContentText("Paused");
				builder.addAction(R.drawable.notification_icon, "play/pause", piPlay);
			}
			else {
				builder.setContentText("Playing");
				builder.addAction(R.drawable.notification_icon_pause, "play/pause", piPlay);		
			}
			
			nm.notify(123456,builder.build());
		}
		
		if(intent.getAction().equals("com.quadcore.naada.NOTIFICATION_PLAY_FROM_PLAYLIST")) {
			Intent serviceIntent = new Intent(context,PlayerService.class);
			PlayerService.LocalBinder binder = (PlayerService.LocalBinder) peekService(context, serviceIntent);
			playerService = binder.getServiceInstance();
			playerService.mainActivity.onClick(playerService.mainActivity.play);
			
			////////////////////////////////////////////////////
			Intent intentt = new Intent(context, MainActivity.class);
			intentt.setAction("com.quadcore.naada.NOTIFICATION");
			PendingIntent pi = PendingIntent.getActivity(context, 0, intentt, 0);
			
			Intent intentPlay = new Intent("com.quadcore.naada.NOTIFICATION_PLAY_FROM_PLAYLIST");
			PendingIntent piPlay = PendingIntent.getBroadcast(context, 0, intentPlay, 0);
			
			String contentText = (String)playerService.mainActivity.tvSongTitle.getText();
			builder = new NotificationCompat.Builder(context)
				.setContentTitle(contentText)
				.setSmallIcon(R.drawable.notification_icon2)
				.setOngoing(true)
				.setAutoCancel(true)
				.setContentIntent(pi)
				.setProgress(100,playerService.mainActivity.songProgressBar.getProgress(),false)
				.setPriority(NotificationCompat.PRIORITY_MAX);
			//////////////////////////////////////////////////
			
			if(playerService.mainActivity.isPaused) {
				builder.setContentText("Paused");
				builder.addAction(R.drawable.notification_icon, "play/pause", piPlay);
			}
			else {
				builder.setContentText("Playing");
				builder.addAction(R.drawable.notification_icon_pause, "play/pause", piPlay);		
			}
			nm.notify(123456,builder.build());
		}
	}

}
