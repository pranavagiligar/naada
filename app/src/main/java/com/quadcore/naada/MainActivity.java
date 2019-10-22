package com.quadcore.naada;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements
		SeekBar.OnSeekBarChangeListener, MediaPlayer.OnCompletionListener,
		OnClickListener, OnLongClickListener, OnCancelListener {

	ImageButton play;
	ImageButton btnForward;
	ImageButton btnBackward;
	ImageButton btnPlaylist;
	ImageButton btnPrevious;
	ImageButton btnNext;
	ImageButton btnRepeat;
	ImageButton btnShuffle;
	ImageButton btnSkin;
	ImageView embeddedPicture;

	TextView tvAppName, tvSongTitle;
	TextView songCurrentDurationLabel, songTotalDurationLabel;

	SongListManager listManager;
	ArrayList<File> songsList;
	ArrayList<String> songs;

	Dialog ad;

	SeekBar songProgressBar;
	Handler mHandler = new Handler();
	Runnable seekThread, playingProgressThread;

	MediaPlayer player = new MediaPlayer();
	AudioManager myAudioManager;
	NotificationManager nm;
	MyNotificationReceiver notificationBroadcastReceiver;

	NotificationCompat.Builder builder, freqBuilder;
	Intent freqIntent, freqIntentPlay;
	PendingIntent freqPi, freqPiPlay;

	Intent serviceIntent;
	PlayerService playerService;
	MainActivity mainActivity;

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {

			PlayerService.LocalBinder binder = (PlayerService.LocalBinder) service;
			playerService = binder.getServiceInstance();
			playerService.player = player;
			playerService.registerMainActivity(mainActivity);
			if (playerService != null) {
				playerService.songIndexObject = songIndex;
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	};

	boolean firstTimeStart = true;
	boolean firstTimeOnStartMethod = true;
	boolean isRepeat = false;
	boolean isShuffle = false;
	boolean isPaused = true;
	boolean songTotalDuration = true;
	boolean clickOnPlaylist = false;
	boolean isPreviousButtonPressed = false;
	boolean isFileNotExists = true;

	// //////////////////////////
	boolean notificationNaada = false;
	boolean notificationClosingTechniqueAtFirstTimeClose = false;
	boolean isNotificationOn = false;
	// //////////////////////////
	boolean isSkinOn = false;
	boolean isCalling = false;
	// //////////////////////////

	PhoneStateListener phoneStateListener = new PhoneStateListener() {

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			super.onCallStateChanged(state, incomingNumber);

			switch (state) {
			case TelephonyManager.CALL_STATE_RINGING:
				if (player != null && isPaused == false) {
					onClick(play);
					isCalling = true;
				}
				break;
			case TelephonyManager.CALL_STATE_IDLE:
				if (player != null && isCalling == true && isPaused == true) {
					if (!firstTimeStart)
						onClick(play);
					isCalling = false;
				}
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK :
				if (player != null && isPaused == false) {
					onClick(play);
					isCalling = true;
				}
				break;
			}
		}
	};
	TelephonyManager telephonyManager;

	int songIndex = 0;
	Integer songIndexObject = 0;
	int currentDuration = 0;

	int musicStreamVolume; // The initial Volume when ear-phone is plugged-in.

	BroadcastReceiver headsetReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra("state")) {
				if (intent.getIntExtra("state", 0) == 0 && isPaused == false) {
					onClick(play);
				}
				if (intent.getIntExtra("state", 0) == 1) {
					for (int i = 1; i <= musicStreamVolume; i++) {
						myAudioManager.setStreamVolume(
								AudioManager.STREAM_MUSIC, i,
								AudioManager.FLAG_PLAY_SOUND);
					}
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player);

		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		if ((getIntent().getAction() == "com.quadcore.naada.NOTIFICATION")) {
			nm.cancel(123456);
			notificationNaada = true;
			finish();
		} else {			/*
			 * Instantiate all the widget elements in the player layout.
			 */
			play = (ImageButton) findViewById(R.id.btnPlay);
			btnForward = (ImageButton) findViewById(R.id.btnForward);
			btnBackward = (ImageButton) findViewById(R.id.btnBackward);
			btnPlaylist = (ImageButton) findViewById(R.id.btnPlaylist);
			btnPrevious = (ImageButton) findViewById(R.id.btnPrevious);
			btnNext = (ImageButton) findViewById(R.id.btnNext);
			btnRepeat = (ImageButton) findViewById(R.id.btnRepeat);
			btnShuffle = (ImageButton) findViewById(R.id.btnShuffle);
			btnSkin = (ImageButton) findViewById(R.id.btnSkin);

			embeddedPicture = (ImageView) findViewById(R.id.embeddedPicture);
			tvAppName = (TextView) findViewById(R.id.AppName);
			tvSongTitle = (TextView) findViewById(R.id.songTitle);
			songCurrentDurationLabel = (TextView) findViewById(R.id.songCurrentDurationLabel);
			songTotalDurationLabel = (TextView) findViewById(R.id.songTotalDurationLabel);
			songProgressBar = (SeekBar) findViewById(R.id.songProgressBar);

			telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

			setListener();
			onLongClick(tvSongTitle); // This is Dummy code for forcibly make
										// user interact.
			songProgressBar.setProgress(0);
			songProgressBar.setMax(100);
			musicStreamVolume = myAudioManager
					.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			musicStreamVolume = (int) (((musicStreamVolume * 33.0) / 100.0) + 0.5);

			// ***********************************************

			serviceIntent = new Intent(MainActivity.this, PlayerService.class);
			mainActivity = MainActivity.this;
			startService(serviceIntent); // Starting the service

			// ***********************************************
		}
	}

	public void reInstantiatorSkin1() {
		embeddedPicture = (ImageView) findViewById(R.id.embeddedPicture1);

		play = (ImageButton) findViewById(R.id.btnPlay1);
		btnForward = (ImageButton) findViewById(R.id.btnForward1);
		btnBackward = (ImageButton) findViewById(R.id.btnBackward1);
		btnPlaylist = (ImageButton) findViewById(R.id.btnPlaylist1);
		btnPrevious = (ImageButton) findViewById(R.id.btnPrevious1);
		btnNext = (ImageButton) findViewById(R.id.btnNext1);
		btnRepeat = (ImageButton) findViewById(R.id.btnRepeat1);
		btnShuffle = (ImageButton) findViewById(R.id.btnShuffle1);
		btnSkin = (ImageButton) findViewById(R.id.btnSkin1);

		tvAppName = (TextView) findViewById(R.id.AppName1);
		tvSongTitle = (TextView) findViewById(R.id.songTitle1);
		songCurrentDurationLabel = (TextView) findViewById(R.id.songCurrentDurationLabel1);
		songTotalDurationLabel = (TextView) findViewById(R.id.songTotalDurationLabel1);
		songProgressBar = (SeekBar) findViewById(R.id.songProgressBar1);
		songProgressBar.setMax(100);
	}

	public void reInstantiatorSkin() {
		play = (ImageButton) findViewById(R.id.btnPlay);
		btnForward = (ImageButton) findViewById(R.id.btnForward);
		btnBackward = (ImageButton) findViewById(R.id.btnBackward);
		btnPlaylist = (ImageButton) findViewById(R.id.btnPlaylist);
		btnPrevious = (ImageButton) findViewById(R.id.btnPrevious);
		btnNext = (ImageButton) findViewById(R.id.btnNext);
		btnRepeat = (ImageButton) findViewById(R.id.btnRepeat);
		btnShuffle = (ImageButton) findViewById(R.id.btnShuffle);
		btnSkin = (ImageButton) findViewById(R.id.btnSkin);

		embeddedPicture = (ImageView) findViewById(R.id.embeddedPicture);
		tvAppName = (TextView) findViewById(R.id.AppName);
		tvSongTitle = (TextView) findViewById(R.id.songTitle);
		songCurrentDurationLabel = (TextView) findViewById(R.id.songCurrentDurationLabel);
		songTotalDurationLabel = (TextView) findViewById(R.id.songTotalDurationLabel);
		songProgressBar = (SeekBar) findViewById(R.id.songProgressBar);
		songProgressBar.setMax(100);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (notificationNaada) {
			finish();
		} else {
			if (ad != null) {
				ad.dismiss();
			}
			if (firstTimeStart) {
				if (songsList.size() != 0)
					playSong(songIndex);
				else {
					TextView tv = new TextView(this);
					tv.setTextColor(getResources().getColor(R.color.red));
					tv.setText("No Song in Secondary Storage..");

					ad = new Dialog(this);
					ad.setTitle(":Sorry:");
					ad.setContentView(tv);
					ad.setOnCancelListener(this);
					ad.show();
				}
			}
			if (playerService != null) {
				playerService.isNotificationOpened = false;
			}
			nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			nm.cancel(123456);

			/*
			 * if (notificationBroadcastReceiver != null)
			 * unregisterReceiver(notificationBroadcastReceiver);
			 */
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (notificationNaada) {
			finish();
		} else {
			clickOnPlaylist = false;
			if (playerService != null)
				playerService.isNotificationOpened = false;
			if (firstTimeOnStartMethod) {
				// Get the playList of type File.

				listManager = new SongListManager();

				songsList = listManager.getPlayList();
				
				if (songsList.size() != 0) {
					/*
					 * Check .Naada file exists. 1. If exists then retrieve song
					 * information of the song that being played before close
					 * the App. 1.Path name of the song. 2.Current duration.
					 * 3.Is in repeat mode. 4.Is in shuffle mode.
					 * 
					 * And prepare that song to play.
					 * 
					 * 2. If not exist then play the first song.
					 */
					isFileNotExists = new File("/sdcard/.Naada").mkdir();
					if (!isFileNotExists) {
						try {
							FileInputStream input = new FileInputStream(
									"/sdcard/.Naada/pause.dat");
							byte[] byteData = new byte[100];
							input.read(byteData);
							input.close();
							String data = new String(byteData);

							String[] dataEle = new String[4];
							int eleI = 0;

							char temp;
							int startingI = 0;
							for (int i = 0; i < data.length(); i++) {
								temp = (char) data.charAt(i);
								if (temp == '*') {
									dataEle[eleI] = data
											.substring(startingI, i);
									startingI = i + 1;
									eleI++;
									if (eleI == 4)
										break;
								}
							}

							String pathName = dataEle[0];
							String duration = dataEle[1];
							String isR = dataEle[2];
							String isS = dataEle[3];

							if (isR.equals("true")) {
								isRepeat = true;
								btnRepeat
										.setImageResource(R.drawable.img_btn_repeat_pressed);
							}
							if (isS.equals("true")) {
								isShuffle = true;
								btnShuffle
										.setImageResource(R.drawable.img_btn_shuffle_pressed);
							}

							currentDuration = Integer.parseInt(duration);
							for (int i = 0; i < songsList.size(); i++) {
								if (pathName.equals(songsList.get(i)
										.getAbsolutePath())) {
									songIndex = i;
									break;
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					bindService(serviceIntent, mConnection,
							Context.BIND_AUTO_CREATE);

					/*
					 * List song name into songs<string> from songList.
					 */
					Iterator<File> iterator = songsList.iterator();
					songs = new ArrayList<String>();
					while (iterator.hasNext()) {
						File file = iterator.next();
						songs.add(file.getName().substring(0,
								file.getName().length() - 4));
						MediaPlayer mp = new MediaPlayer();
						mp.reset();
						try {
							mp.setDataSource(file.getAbsolutePath());
							mp.prepare();
						} catch (Exception e) {
							e.printStackTrace();
						}
						songs.add("" + milliSecondsToTimer(mp.getDuration()));
					}

					seekThread = new Runnable() {

						@Override
						public void run() {

							long totalDuration = player.getDuration();
							long currentDurations = player.getCurrentPosition();

							if (songTotalDuration) {
								if (!isSkinOn) {
									songTotalDurationLabel
											.setTextColor(getResources()
													.getColor(
															R.color.mySkin1Color));
								} else {
									songTotalDurationLabel
											.setTextColor(getResources()
													.getColor(R.color.red));
								}
								songTotalDurationLabel.setText(""
										+ milliSecondsToTimer(totalDuration));
							} else {
								if (!isSkinOn) {
									songTotalDurationLabel
											.setTextColor(getResources()
													.getColor(R.color.red));
								} else {
									songTotalDurationLabel
											.setTextColor(getResources()
													.getColor(R.color.green));
								}
								songTotalDurationLabel.setText("-"
										+ milliSecondsToTimer(totalDuration
												- currentDurations));
							}
							// Displaying time completed playing
							songCurrentDurationLabel.setText(""
									+ milliSecondsToTimer(currentDurations));

							int progress = (int) (getProgressPercentage(
									currentDurations, totalDuration));

							songProgressBar.setProgress(progress);
							mHandler.postDelayed(this, 30);
						}
					};

					playingProgressThread = new Runnable() {

						@Override
						public void run() {
							if (songProgressBar != null && builder != null
									&& playerService.isNotificationOpened) {
								freqBuilder = new NotificationCompat.Builder(
										getApplicationContext())
										.setOngoing(true)
										.setSmallIcon(
												R.drawable.notification_icon2)
										.setAutoCancel(true)
										.setContentIntent(freqPi)
										.setPriority(
												NotificationCompat.PRIORITY_MAX)
										.setContentTitle(tvSongTitle.getText())
										.setProgress(100,
												songProgressBar.getProgress(),
												false);

								if (isPaused) {
									freqBuilder.setContentText("Paused");
									freqBuilder.addAction(
											R.drawable.notification_icon,
											"play/pause", freqPiPlay);
								} else {
									freqBuilder.setContentText("Playing");
									freqBuilder.addAction(
											R.drawable.notification_icon_pause,
											"play/pause", freqPiPlay);
								}
								nm.notify(123456, freqBuilder.build());
							}
							mHandler.postDelayed(this, 1000);
						}
					};

					freqIntent = new Intent(getApplicationContext(),
							MainActivity.class);
					freqIntent.setAction("com.quadcore.naada.NOTIFICATION");
					freqPi = PendingIntent.getActivity(getApplicationContext(),
							0, freqIntent, 0);

					freqIntentPlay = new Intent(
							"com.quadcore.naada.NOTIFICATION_PLAY");
					freqPiPlay = PendingIntent.getBroadcast(
							getApplicationContext(), 0, freqIntentPlay, 0);

					mHandler.postDelayed(playingProgressThread, 10);
					mHandler.postDelayed(seekThread, 30);
					firstTimeOnStartMethod = false;

					notificationBroadcastReceiver = new MyNotificationReceiver();
					IntentFilter filter = new IntentFilter();
					filter.addAction("com.quadcore.naada.NOTIFICATION_PLAY");
					registerReceiver(notificationBroadcastReceiver, filter);
				} else {
					Toast.makeText(getApplicationContext(),
							"No songs on memory", Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	@Override
	protected void onStop() {

		super.onPause();
		if (notificationNaada) {
			finish();
		} else {
			if (ad != null) {
				ad.dismiss();
			}
			if (songsList.size() == 0) {
				finish();
			} else {
				Intent intent = new Intent(this, MainActivity.class);
				intent.setAction("com.quadcore.naada.NOTIFICATION");
				PendingIntent pi = PendingIntent.getActivity(
						getApplicationContext(), 0, intent, 0);

				Intent intentPlay = new Intent(
						"com.quadcore.naada.NOTIFICATION_PLAY");
				PendingIntent piPlay = PendingIntent.getBroadcast(
						getApplicationContext(), 0, intentPlay, 0);

				builder = new NotificationCompat.Builder(
						getApplicationContext())
						.setContentTitle(tvSongTitle.getText())
						.setOngoing(true)
						.setSmallIcon(R.drawable.notification_icon2)
						.setAutoCancel(true).setContentIntent(pi)
						.setProgress(100, songProgressBar.getProgress(), false)
						.setPriority(NotificationCompat.PRIORITY_MAX);

				if (isPaused) {
					builder.setContentText("Paused");
					builder.addAction(R.drawable.notification_icon,
							"play/pause", piPlay);
				} else {
					builder.setContentText("Playing");
					builder.addAction(R.drawable.notification_icon_pause,
							"play/pause", piPlay);
				}

				if (!clickOnPlaylist
						&& !notificationClosingTechniqueAtFirstTimeClose) {
					nm.notify(123456, builder.build());
					playerService.isNotificationOpened = true;
				}
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		nm.cancel(123456);
		if (notificationNaada) {
			finish();
		} else {
			if (songsList.size() != 0) {
				new File("/sdcard/.Naada").mkdir();
				try {
					FileOutputStream output = new FileOutputStream(
							"/sdcard/.Naada/pause.dat");
					String pausedData = songsList.get(songIndex)
							.getAbsolutePath()
							+ "*"
							+ player.getCurrentPosition()
							+ "*"
							+ isRepeat
							+ "*" + isShuffle + "*";
					output.write(pausedData.getBytes());
					output.close();
					player.pause();
				} catch (Exception e) {
					e.printStackTrace();
				}
				player.stop();
				mHandler.removeCallbacks(playingProgressThread);
				unbindService(mConnection);
				stopService(serviceIntent);
				if (notificationBroadcastReceiver != null)
					unregisterReceiver(notificationBroadcastReceiver);
				if (headsetReceiver != null)
					unregisterReceiver(headsetReceiver);
			} else {
				stopService(serviceIntent);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (notificationNaada) {
			finish();
		} else {
			if (resultCode == 100) {
				int temp = data.getExtras().getInt("songIndex");
				if (temp != songIndex) {
					songIndex = temp;
					// play selected song
					isPaused = false;
					playSong(songIndex);
					clickOnPlaylist = false;
					playerService.songIndexObject = songIndex;
				}
			}
		}
	}

	public void onClick(View controlButton) {

		if (controlButton == tvAppName) {
			// About our team.
			TextView tv = new TextView(this);
			tv.setBackgroundColor(getResources().getColor(R.color.teal));
			tv.setTextColor(getResources().getColor(R.color.white));
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			tv.setText("Naada is build in SDK provided by AOSP(Android OpenSource Project),\r\n"
					
					  + "Devloped by :\r\n" + "GuruPrasad\r\n" + "Pranava\r\n"
					  + "Rakesh\r\n" + "Sunil")
					 + "which is under Apache OpenSource License");

			ad = new Dialog(this);
			ad.setTitle("About");
			ad.setContentView(tv);
			ad.show();
		}

		if (controlButton == play) {

			// check for already playing
			if (player.isPlaying()) {
				if (player != null) {
					player.pause();
					isPaused = true;
					// Changing button image to play button
					if (!isSkinOn)
						play.setImageResource(R.drawable.btn_play);
					else
						play.setImageResource(R.drawable.btn_play1);

				}
			} else {
				// Resume song
				if (player != null) {
					player.start();
					isPaused = false;
					// Changing button image to pause button
					if (!isSkinOn)
						play.setImageResource(R.drawable.btn_pause);
					else
						play.setImageResource(R.drawable.btn_pause1);
				}
			}
		}

		if (controlButton == btnForward) {
			// get current song position
			int currentPosition = player.getCurrentPosition();
			// check if seekForward time is lesser than song duration
			if (currentPosition + 5000 <= player.getDuration()) {
				// forward song
				player.seekTo(currentPosition + 5000);
			} else {
				// forward to end position
				player.seekTo(player.getDuration());
			}
		}

		if (controlButton == btnBackward) {
			// get current song position
			int currentPosition = player.getCurrentPosition();
			// check if seekBackward time is greater than 0 sec
			if (currentPosition - 5000 >= 0) {
				// forward song
				player.seekTo(currentPosition - 5000);
			} else {
				// backward to starting position
				player.seekTo(0);
			}
		}

		if (controlButton == btnPrevious) {
			isPreviousButtonPressed = true;
			if (songIndex > 0) {
				playSong(--songIndex);
			} else {
				// play last song
				playSong(songsList.size() - 1);
				songIndex = songsList.size() - 1;
			}
			playerService.songIndexObject = songIndex;
		}

		if (controlButton == btnNext) {
			if (songIndex < songsList.size() - 1) {
				playSong(++songIndex);
			} else {
				playSong(0);
				songIndex = 0;
			}
			playerService.songIndexObject = songIndex;
		}

		if (controlButton == btnPlaylist) {
			clickOnPlaylist = true;
			SharedPreferences sPref = getSharedPreferences("ScrollPosition", 0);
			SharedPreferences.Editor editor = sPref.edit();
			editor.putInt("pos", songIndex);
			editor.commit();
			
			try {
				Intent playListIntent = new Intent(getApplicationContext(),
						Class.forName("com.quadcore.naada.PlayListActivity"));
				playListIntent.putStringArrayListExtra("allSongsName", songs);
				playListIntent.putExtra("playingSongIndex", songIndex);
				startActivityForResult(playListIntent, 100);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (controlButton == btnRepeat) {

			if (isRepeat) {
				isRepeat = false;
				if (!isSkinOn)
					btnRepeat.setImageResource(R.drawable.img_btn_repeat);
				else
					btnRepeat.setImageResource(R.drawable.img_btn_repeat1);
				Toast.makeText(getApplicationContext(), (String) "Repeat off",
						Toast.LENGTH_SHORT).show();
			} else {
				isRepeat = true;
				if (!isSkinOn)
					btnRepeat
							.setImageResource(R.drawable.img_btn_repeat_pressed);
				else
					btnRepeat
							.setImageResource(R.drawable.img_btn_repeat_pressed1);
				Toast.makeText(getApplicationContext(), (String) "Repeat on",
						Toast.LENGTH_SHORT).show();
			}
		}

		if (controlButton == btnShuffle) {

			if (!isShuffle) {

				isShuffle = true;
				if (!isSkinOn)
					btnShuffle
							.setImageResource(R.drawable.img_btn_shuffle_pressed);
				else
					btnShuffle
							.setImageResource(R.drawable.img_btn_shuffle_pressed1);
				Toast.makeText(getApplicationContext(), (String) "Shuffle on",
						Toast.LENGTH_SHORT).show();

			} else {
				isShuffle = false;
				if (!isSkinOn)
					btnShuffle.setImageResource(R.drawable.img_btn_shuffle);
				else
					btnShuffle.setImageResource(R.drawable.img_btn_shuffle1);

				Toast.makeText(getApplicationContext(), (String) "Shuffle off",
						Toast.LENGTH_SHORT).show();
			}
		}

		if (controlButton == btnSkin) {
			embeddedPicture = null;
			if (!isSkinOn) {
				setContentView(R.layout.player1);
				isSkinOn = true;
				reInstantiatorSkin1();
				// //////////////////////////////
				if (player.isPlaying()) {
					play.setImageResource(R.drawable.btn_pause1);
				}
				if (isRepeat) {
					btnRepeat
							.setImageResource(R.drawable.img_btn_repeat_pressed1);
				}
				if (isShuffle) {
					btnShuffle
							.setImageResource(R.drawable.img_btn_shuffle_pressed1);
				}
				// //////////////////////////////
			} else {
				setContentView(R.layout.player);
				isSkinOn = false;
				reInstantiatorSkin();
				// //////////////////////////////
				if (player.isPlaying()) {
					play.setImageResource(R.drawable.btn_pause);
				}
				if (isRepeat) {
					btnRepeat
							.setImageResource(R.drawable.img_btn_repeat_pressed);
				}
				if (isShuffle) {
					btnShuffle
							.setImageResource(R.drawable.img_btn_shuffle_pressed);
				}
				// //////////////////////////////
			}

			setAgainListener();

			File file = songsList.get(songIndex);
			tvSongTitle.setText((String) file.getName().substring(0,
					file.getName().length() - 4));
			MediaMetadataRetriever mmr = new MediaMetadataRetriever();
			mmr.setDataSource(songsList.get(songIndex).getAbsolutePath());
			byte[] artBytes = mmr.getEmbeddedPicture();
			mmr.release();
			if (artBytes != null) {
				Bitmap bitmapImage = BitmapFactory.decodeByteArray(artBytes, 0,
						artBytes.length);
				embeddedPicture.setImageBitmap(bitmapImage);
			}

			mHandler.postDelayed(seekThread, 30);
		}

		// ////////////////////////////////////
		if (controlButton == songTotalDurationLabel) {
			songTotalDuration = !songTotalDuration;
		}

		if (controlButton == embeddedPicture) {
			TextView tv = new TextView(this);
			tv.setBackgroundColor(getResources().getColor(R.color.maroon));
			tv.setTextColor(getResources().getColor(R.color.white));
			tv.setGravity(Gravity.START);
			String songInfo = "";
			try {
				MediaMetadataRetriever mmr = new MediaMetadataRetriever();
				mmr.setDataSource(songsList.get(songIndex).getAbsolutePath());

				String temp = mmr
						.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
				if (temp != null) {
					songInfo = "ALBUM  :" + temp + "\r\n";
				}
				temp = mmr
						.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
				if (temp != null) {
					songInfo = songInfo + "ARTIST :" + temp + "\r\n";
				}
				temp = mmr
						.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR);
				if (temp != null) {
					songInfo = songInfo + "AUTHOR :" + temp + "\r\n";
				}
				temp = mmr
						.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
				if (temp != null) {
					songInfo = songInfo + "YEAR   :" + temp + "\r\n";
				}
				temp = mmr
						.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
				if (temp != null) {
					songInfo = songInfo + "BitRate:"
							+ (Integer.parseInt(temp) / 1000) + "kbps\r\n";
				}
				mmr.release();
			} catch (AndroidRuntimeException e) {
				Log.v("###", "Error in Song Info");
			}

			if (songInfo.equals(""))
				songInfo = "Information not available";
			tv.setText(songInfo);

			ad = new Dialog(this);
			ad.setTitle("About Song");
			ad.setContentView(tv);
			ad.show();
		}
	}

	public boolean onLongClick(View controlButton) {
		if (controlButton == btnSkin) {
			Toast.makeText(getApplicationContext(), (String) "Skin",
					Toast.LENGTH_SHORT).show();
		}

		if (controlButton == play) {
			Toast.makeText(getApplicationContext(), (String) "Play/Pause",
					Toast.LENGTH_SHORT).show();
		}

		if (controlButton == btnForward) {
			Toast.makeText(getApplicationContext(), (String) "Forward(5s)",
					Toast.LENGTH_SHORT).show();
		}

		if (controlButton == btnBackward) {
			Toast.makeText(getApplicationContext(), (String) "Backward(5s)",
					Toast.LENGTH_SHORT).show();
		}

		if (controlButton == btnPrevious) {
			Toast.makeText(getApplicationContext(), (String) "Previous Song",
					Toast.LENGTH_SHORT).show();
		}

		if (controlButton == btnNext) {
			Toast.makeText(getApplicationContext(), (String) "Next Song",
					Toast.LENGTH_SHORT).show();
		}

		if (controlButton == btnPlaylist) {
			Toast.makeText(getApplicationContext(), (String) "PlayList",
					Toast.LENGTH_SHORT).show();
		}

		if (controlButton == btnRepeat) {
			Toast.makeText(getApplicationContext(), (String) "Repeat on/off",
					Toast.LENGTH_SHORT).show();
		}

		if (controlButton == btnShuffle) {
			Toast.makeText(getApplicationContext(), (String) "Shuffle on/off",
					Toast.LENGTH_SHORT).show();
		}

		return false;
	}

	int counter = 0;

	public void onStartTrackingTouch(SeekBar seekBar) {
		mHandler.removeCallbacks(seekThread);
	}

	/*
	 * When user stops moving the progress handler.
	 */
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		mHandler.removeCallbacks(seekThread);
		int totalDuration = player.getDuration();
		int currentPositions = progressToTimer(seekBar.getProgress(),
				totalDuration);

		// forward or backward to certain seconds
		player.seekTo(currentPositions);

		// update timer progress again
		mHandler.postDelayed(seekThread, 30);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
	}

	public void onCompletion(MediaPlayer mp) {
		if (songsList.size() != 0) {
			if (isRepeat) {
				playSong(songIndex);
			} else {
				if (isShuffle) {
					if (songsList.size() != 1) {
						Random ran = new Random();
						int indx = 0;
						do {
							indx = ran.nextInt(songsList.size() );
						} while (indx == songIndex);
						songIndex = indx;
					}
					playSong(songIndex);
				} else {
					if (songIndex < songsList.size() - 1)
						playSong(++songIndex);
					else {
						playSong(0);
						songIndex = 0;
					}
				}
			}

			playerService.songIndexObject = songIndex;

			if (playerService.activity != null) {
				SharedPreferences sPref = getSharedPreferences("ScrollPosition", 0);
				SharedPreferences.Editor editor = sPref.edit();
				editor.putInt("pos", songIndex);
				editor.commit();
				playerService.activity.myCallbackUpdater();
			}
			if (playerService.isNotificationOpened) {
				Intent intent = new Intent(this, MainActivity.class);
				intent.setAction("com.quadcore.naada.NOTIFICATION");
				PendingIntent pi = PendingIntent.getActivity(
						getApplicationContext(), 0, intent, 0);

				Intent intentPlay = new Intent(
						"com.quadcore.naada.NOTIFICATION_PLAY");
				PendingIntent piPlay = PendingIntent.getBroadcast(
						getApplicationContext(), 0, intentPlay, 0);
				builder = new NotificationCompat.Builder(
						getApplicationContext())
						.setSmallIcon(R.drawable.notification_icon2)
						.setOngoing(true)
						.setAutoCancel(true)
						.setContentIntent(pi)
						.addAction(R.drawable.notification_icon_pause,
								"play/pause", piPlay).setContentText("Playing")
						.setContentTitle(tvSongTitle.getText())
						.setProgress(100, songProgressBar.getProgress(), false)
						.setPriority(NotificationCompat.PRIORITY_MAX);

				if (!notificationClosingTechniqueAtFirstTimeClose && !isPaused) {
					nm.notify(123456, builder.build());
					playerService.isNotificationOpened = true;
				}
			}
		}
	}

	private void setListener() {
		play.setOnClickListener(this);
		btnForward.setOnClickListener(this);
		btnBackward.setOnClickListener(this);
		btnPlaylist.setOnClickListener(this);
		btnPrevious.setOnClickListener(this);
		btnNext.setOnClickListener(this);
		btnRepeat.setOnClickListener(this);
		btnShuffle.setOnClickListener(this);
		tvAppName.setOnClickListener(this);
		songTotalDurationLabel.setOnClickListener(this);

		btnSkin.setOnClickListener(this);
		embeddedPicture.setOnClickListener(this);
		play.setOnLongClickListener(this);
		btnForward.setOnLongClickListener(this);
		btnBackward.setOnLongClickListener(this);
		btnPlaylist.setOnLongClickListener(this);
		btnPrevious.setOnLongClickListener(this);
		btnNext.setOnLongClickListener(this);
		btnRepeat.setOnLongClickListener(this);
		btnShuffle.setOnLongClickListener(this);
		btnSkin.setOnLongClickListener(this);

		songProgressBar.setOnSeekBarChangeListener(this);

		player.setOnCompletionListener(this);

		telephonyManager.listen(phoneStateListener,
				PhoneStateListener.LISTEN_CALL_STATE);

		registerReceiver(headsetReceiver, new IntentFilter(
				Intent.ACTION_HEADSET_PLUG));

		myAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	}

	private void setAgainListener() {
		play.setOnClickListener(this);
		btnForward.setOnClickListener(this);
		btnBackward.setOnClickListener(this);
		btnPlaylist.setOnClickListener(this);
		btnPrevious.setOnClickListener(this);
		btnNext.setOnClickListener(this);
		btnRepeat.setOnClickListener(this);
		btnShuffle.setOnClickListener(this);
		tvAppName.setOnClickListener(this);
		songTotalDurationLabel.setOnClickListener(this);

		btnSkin.setOnClickListener(this);
		embeddedPicture.setOnClickListener(this);
		play.setOnLongClickListener(this);
		btnForward.setOnLongClickListener(this);
		btnBackward.setOnLongClickListener(this);
		btnPlaylist.setOnLongClickListener(this);
		btnPrevious.setOnLongClickListener(this);
		btnNext.setOnLongClickListener(this);
		btnRepeat.setOnLongClickListener(this);
		btnShuffle.setOnLongClickListener(this);
		btnSkin.setOnLongClickListener(this);

		songProgressBar.setOnSeekBarChangeListener(this);
	}

	void playSong(int index) {

		try {
			player.reset();
			try {
				player.setDataSource(songsList.get(index).getAbsolutePath());
			}
			catch(Exception fe) {
				songsList.remove(index);
				songs.remove(index*2);
				songs.remove(index*2);
				if(songsList.size() == 0) {
					playerService.isNotificationOpened = false;
					
					nm.cancel(123456);
					finish();
				}
				if (playerService.activity != null) {
					--songIndex;
					playerService.activity.songs = songs;
					playerService.activity.myCallbackUpdater();
				}
				
				if(isPreviousButtonPressed) {
					playSong(--songIndex);
				}
				return;
			}
			player.prepare();
			if (currentDuration != 0 && firstTimeStart) {
				player.seekTo(currentDuration);
			}
			File file = songsList.get(index);

			tvSongTitle.setText((String) file.getName().substring(0,
					file.getName().length() - 4));
			MediaMetadataRetriever mmr = new MediaMetadataRetriever();
			mmr.setDataSource(songsList.get(index).getAbsolutePath());
			byte[] artBytes = mmr.getEmbeddedPicture();
			mmr.release();
			if (artBytes != null) {
				Bitmap bitmapImage = BitmapFactory.decodeByteArray(artBytes, 0,
						artBytes.length);
				embeddedPicture.setImageBitmap(bitmapImage);
			} else {
				if (isSkinOn) {
					embeddedPicture.setImageDrawable(getResources()
							.getDrawable(R.drawable.radioactive3));
				} else {
					embeddedPicture.setImageDrawable(getResources()
							.getDrawable(R.drawable.radioactive2));
				}
			}

			if (!isPaused) {
				player.start();
				if (!isSkinOn)
					play.setImageResource(R.drawable.btn_pause);
				else
					play.setImageResource(R.drawable.btn_pause1);
			} else
				firstTimeStart = false;
			isPreviousButtonPressed = false;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getProgressPercentage(long currentDurations, long totalDuration) {
		Double percentage = (double) 0;

		long currentSeconds = (int) (currentDurations / 1000);
		long totalSeconds = (int) (totalDuration / 1000);

		// calculating percentage
		percentage = (((double) currentSeconds) / totalSeconds) * 100;

		// return percentage
		return percentage.intValue();
	}

	public int progressToTimer(int progress, int totalDuration) {
		int currentDurations = 0;
		totalDuration = (int) (totalDuration / 1000);
		currentDurations = (int) ((((double) progress) / 100) * totalDuration);

		// return current duration in milliseconds
		return currentDurations * 1000;
	}

	public String milliSecondsToTimer(long milliseconds) {
		String finalTimerString = "";
		String secondsString = "";

		// Convert total duration into time
		int hours = (int) (milliseconds / (1000 * 60 * 60));
		int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
		int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
		// Add hours if there
		if (hours > 0) {
			finalTimerString = hours + ":";
		}

		// Prepending 0 to seconds if it is one digit
		if (seconds < 10) {
			secondsString = "0" + seconds;
		} else {
			secondsString = "" + seconds;
		}

		finalTimerString = finalTimerString + minutes + ":" + secondsString;

		// return timer string
		return finalTimerString;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && !isPaused) {
			moveTaskToBack(true);
		}
		if (keyCode == KeyEvent.KEYCODE_BACK && isPaused) {
			notificationClosingTechniqueAtFirstTimeClose = true;
			finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		finish();
	}
}
