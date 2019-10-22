package com.quadcore.naada;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

public class PlayerService extends Service {

	private final IBinder mBinder = new LocalBinder();
	public MediaPlayer player;
	public Integer songIndexObject;
	public PlayListActivity activity;
	public MainActivity mainActivity;
	public boolean isNotificationOpened = false;
	
	public int scrollbarPositionOnList = 0;


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// returns the instance of the service
	public class LocalBinder extends Binder {
		public PlayerService getServiceInstance() {
			return PlayerService.this;
		}
	}

	public void registerActivity(PlayListActivity ref) {
		activity = ref;
	}
	
	public void registerMainActivity(MainActivity ref) {
		mainActivity = ref;
	}

}
