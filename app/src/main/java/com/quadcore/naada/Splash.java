package com.quadcore.naada;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Splash extends Activity implements Runnable {
	
	Bundle bundle;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash_layout);
		bundle = savedInstanceState;
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Thread th = new Thread(this);
		th.start();
	}

	@Override
	protected void onStop() {
		super.onStop();
		finish();
	}
	
	public void run() {
		try {
			Thread.sleep(1000);
		}
		catch(InterruptedException e) {}
		Intent startingPoint = new Intent(getApplicationContext(),MainActivity.class);
		startActivity(startingPoint,bundle);
	}

}
