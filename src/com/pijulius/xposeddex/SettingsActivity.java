package com.pijulius.xposeddex;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
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

			MultiSelectListPreference immersiveApps =
					(MultiSelectListPreference) findPreference("immersiveApps");

			Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

			List<ResolveInfo> pkgAppsList =
					getActivity().getPackageManager().queryIntentActivities(mainIntent, 0);

			String[] allApps = new String[pkgAppsList.size()];
			String[] allPackages = new String[pkgAppsList.size()];

			int i = 0;
			for (ResolveInfo ri : pkgAppsList) {
				if (ri.activityInfo == null)
					continue;

				allApps[i] = ri.activityInfo.loadLabel(
						getActivity().getPackageManager()).toString();
				allPackages[i] = ri.activityInfo.packageName;

				i++;
			}

			immersiveApps.setEntries(allApps);
			immersiveApps.setEntryValues(allPackages);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Editor editor = protectedPreferences.edit();

		if ("immersiveApps".equals(key))
			editor.putStringSet(key, sharedPreferences.getStringSet(key, new HashSet<String>()));

		if ("fixKeyboard".equals(key) || "fixSBrowser".equals(key) || "fixWeather".equals(key) || "fixSquid".equals(key))
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