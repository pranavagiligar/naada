package com.quadcore.naada;

import java.util.ArrayList;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PlayListActivity extends Activity implements OnScrollListener {
	
	NotificationManager nm;
	ArrayList<String> songs;
	ListView list;
	CustomAdapter adapter;
	Resources res;
	SharedPreferences sPref;
	
	public PlayListActivity CustomListView = null;
	public ArrayList<ListModel> CustomListViewValuesArr = new ArrayList<ListModel>();
	int playingSongIndex;
	
	Intent serviceIntent;
	PlayerService playerService;
	ServiceConnection mConnection;
	MyNotificationReceiver notificationBroadcastReceiver;
	NotificationCompat.Builder builder;
	
	int scrollBarPosition = 0;
	boolean isPaused = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.listview_activity);
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (getIntent().getAction() == "com.quadcore.naada.NOTIFICATION") {
			nm.cancel(123456);
		}
		sPref = getSharedPreferences("ScrollPosition", 0);
		
		/////////////////////////////////////////////////////////
		
		mConnection = new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName className, IBinder service) {

				PlayerService.LocalBinder binder = (PlayerService.LocalBinder) service;
				playerService = binder.getServiceInstance();
				playerService.registerActivity(PlayListActivity.this);
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
			}
		};
		
		/////////////////////////////////////////////////////////
		serviceIntent = new Intent(PlayListActivity.this, PlayerService.class);
		bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);
		
		CustomListView = this;
		songs = getIntent().getStringArrayListExtra("allSongsName");
		playingSongIndex = getIntent().getIntExtra("playingSongIndex", -1);
		res = getResources();
		list = (ListView) findViewById(R.id.list);

		setListData();
		adapter = new CustomAdapter(CustomListView, CustomListViewValuesArr,
				res);
		list.setAdapter(adapter);
		list.setSelection(sPref.getInt("pos", 0)-3);
		
		//list.setSelection(sPref.getInt("pos", 0)-3);
		list.setOnScrollListener(this);
		
		notificationBroadcastReceiver = new MyNotificationReceiver();
		IntentFilter filter = new IntentFilter();
	    filter.addAction("com.quadcore.naada.NOTIFICATION_PLAY_FROM_PLAYLIST");
	    registerReceiver(notificationBroadcastReceiver, filter);

	}

	public void setListData() {
		for (int i = 0; i < songs.size(); i++) {

			final ListModel sched = new ListModel();

			/******* Firstly take data in model object ******/
			sched.setTitle(songs.get(i++));
			sched.setDuration(songs.get(i));
			sched.setImage("");

			/******** Take Model Object in ArrayList **********/
			CustomListViewValuesArr.add(sched);
		}
		CustomListViewValuesArr.get(playingSongIndex).setImage(
				"notification_icon");
	}

	public void onItemClick(int mPosition) {
		Intent playListIntent = new Intent(getApplicationContext(),MainActivity.class);
		playListIntent.putExtra("songIndex", mPosition);

		setResult(100, playListIntent);

		finish();
	}
	
	public void onItemLongClick(int mPosition) {
		
	}
	
	protected void onStop() {

		super.onPause();
		
		playerService.scrollbarPositionOnList = scrollBarPosition;
	    
		Intent intent = new Intent(this, MainActivity.class);
		intent.setAction("com.quadcore.naada.NOTIFICATION");
		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
		
		Intent intentPlay = new Intent("com.quadcore.naada.NOTIFICATION_PLAY_FROM_PLAYLIST");
		PendingIntent piPlay = PendingIntent.getBroadcast(getApplicationContext(), 0, intentPlay, 0);
		
		String contentText = (String)playerService.mainActivity.tvSongTitle.getText();
		builder = new NotificationCompat.Builder(getApplicationContext())
			.setContentTitle(contentText)
			.setSmallIcon(R.drawable.notification_icon2)
			.setOngoing(true)
			.setAutoCancel(true)
			.setContentIntent(pi)
			.setPriority(NotificationCompat.PRIORITY_MAX);
			
		if(playerService.mainActivity.isPaused) {
			builder.setContentText("Paused");
			builder.addAction(R.drawable.notification_icon,"play/pause",piPlay);
		}
		else {
			builder.setContentText("Playing");
			builder.addAction(R.drawable.notification_icon_pause,"play/pause",piPlay);
		}

		nm.notify(123456, builder.build());
		playerService.isNotificationOpened = true;
	}

	@Override
	protected void onDestroy() {

		super.onDestroy();
		
		nm.cancel(123456);
		playerService.isNotificationOpened = false;
		if(notificationBroadcastReceiver != null)
			unregisterReceiver(notificationBroadcastReceiver);
		playerService.activity = null;
		unbindService(mConnection);
		
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		isPaused = true;
	}

	@Override
	protected void onResume() {

		super.onResume();
		isPaused = false;
		nm.cancel(123456);
		if(playerService != null)
			playerService.isNotificationOpened = false;
		
	}
	
	public void myCallbackUpdater() {	
		CustomListViewValuesArr.clear();
		setListData();
		////////////////////////////////////////
		CustomListViewValuesArr.get(playingSongIndex).setImage("");
		playingSongIndex = playerService.songIndexObject;
		CustomListViewValuesArr.get(playerService.songIndexObject).setImage("notification_icon");
		adapter = new CustomAdapter(CustomListView, CustomListViewValuesArr, res);
		list.setAdapter(adapter);
		list.setSelection(scrollBarPosition);
		if(!isPaused) {
			Toast t = Toast.makeText(getApplicationContext(), playerService.mainActivity.tvSongTitle.getText() ,Toast.LENGTH_LONG);
			t.setGravity(Gravity.CENTER,0,0);
			TextView tvt = new TextView(this);
			tvt.setText(playerService.mainActivity.tvSongTitle.getText());
			tvt.setBackgroundColor(getResources().getColor(R.color.black));
			tvt.setTextColor(getResources().getColor(R.color.red));
			tvt.setAlpha(0.8f);
			tvt.setTextSize(22.3f);
			t.setView(tvt);
			t.show();
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		scrollBarPosition = firstVisibleItem;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {}
}
