package com.pijulius.xposeddex;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	Context protectedContext;
	SharedPreferences protectedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		protectedContext = createDeviceProtectedStorageContext();
		protectedPreferences = protectedContext.getSharedPreferences("com.pijulius.xposeddex_preferences", MODE_PRIVATE);

		getFragmentManager().beginTransaction().replace(android.R.id.content,
				new PreferencesFragment()).commit();

		SharedPreferences preferences = getSharedPreferences(
				getClass().getPackage().getName()+"_preferences", MODE_PRIVATE);
		preferences.registerOnSharedPreferenceChangeListener(this);
	}

	public static class PreferencesFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.settings);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Editor editor = protectedPreferences.edit();

		editor.putBoolean(key, sharedPreferences.getBoolean(key, true));
		editor.apply();

		AsyncTask.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(500);
				} catch (Throwable t) {}

				File protectedFolder = protectedContext.getFilesDir().getParentFile();

				if (protectedFolder.exists()) {
					protectedFolder.setExecutable(true, false);
					protectedFolder.setReadable(true, false);

					File sharedPrefsFolder =
						new File(protectedFolder.getAbsolutePath() + "/shared_prefs");

					if (sharedPrefsFolder.exists()) {
						sharedPrefsFolder.setExecutable(true, false);
						sharedPrefsFolder.setReadable(true, false);

						File sharedPrefsFile =
							new File(sharedPrefsFolder.getAbsolutePath() + "/com.pijulius.xposeddex_preferences.xml");

						if (sharedPrefsFile.exists()) {
							sharedPrefsFile.setReadable(true, false);
							sharedPrefsFile.setExecutable(true, false);
						}
					}
				}
			}
		});
	}
}