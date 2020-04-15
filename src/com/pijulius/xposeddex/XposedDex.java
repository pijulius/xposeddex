package com.pijulius.xposeddex;

import static de.robv.android.xposed.XposedHelpers.findClass;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.XResources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedDex implements IXposedHookInitPackageResources, IXposedHookLoadPackage, IXposedHookZygoteInit {
	public static String modulePath;
	public static String packageName;

	public static XSharedPreferences settings = null;
	public static String settingsFile = "/data/user_de/0/com.pijulius.xposeddex/shared_prefs/com.pijulius.xposeddex_preferences.xml";

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		modulePath = startupParam.modulePath;
		packageName = getClass().getPackage().getName();
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (settings == null)
			settings = new XSharedPreferences(new File(settingsFile));

		immersiveApps(lpparam);

		if (settings.getBoolean("fixKeyboard", true))
			keyboardFixes(lpparam);

		if (settings.getBoolean("fixSBrowser", true))
			sbrowserFixes(lpparam);

		if (settings.getBoolean("fixWeather", true))
			weatherFixes(lpparam);
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		if (settings == null)
			settings = new XSharedPreferences(new File(settingsFile));

		if (settings.getBoolean("fixKeyboard", true))
			keyboardResourceFixes(resparam);
	}

	public void immersiveApps(LoadPackageParam lpparam) {
		// Make selected apps to be in immersive mode
		XposedBridge.hookAllMethods(Activity.class, "onStart", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Activity activity = (Activity)param.thisObject;
				if (activity == null)
					return;

				settings.reload();

				Set<String> immersiveApps = settings.getStringSet("immersiveApps", new HashSet<String>());

				if (!immersiveApps.contains(activity.getPackageName()))
					return;

				activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
			}
		});

		XposedBridge.hookAllMethods(Activity.class, "onWindowFocusChanged", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Activity activity = (Activity)param.thisObject;
				if (activity == null)
					return;

				Set<String> immersiveApps = settings.getStringSet("immersiveApps", new HashSet<String>());

				if (!immersiveApps.contains(activity.getPackageName()))
					return;

				activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
			}
		});
	}

	public void keyboardFixes(LoadPackageParam lpparam) {
		// Fix floating keyboard for none qwery layouts too
		if (!lpparam.packageName.equals("com.samsung.android.honeyboard"))
			return;

		Class<?> hookClass = null;

		// context, keyboardtype/ , com/samsung/android/honeyboard/base/i/b/a;->i()Z,
		// if-eqz, const-string "KeyboardViewType.VIEW_FLOATING"
		// else, const-string "KeyboardViewType.VIEW_NORMAL"
		hookClass = findClass("com.samsung.android.honeyboard.base.i.b.a",
				lpparam.classLoader);

		XposedBridge.hookAllMethods(hookClass, "i", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(true);
			}
		});
	}

	public void keyboardResourceFixes(InitPackageResourcesParam resparam) {
		if (!resparam.packageName.equals("com.samsung.android.honeyboard"))
			return;

		resparam.res.setReplacement(
			"com.samsung.android.honeyboard", "drawable", "desktop_decor_caption_title", new XResources.DrawableLoader() {
				@Override
				public Drawable newDrawable(XResources res, int id) throws Throwable {
					if ((res.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
						return new ColorDrawable(Color.BLACK);

					return new ColorDrawable(Color.LTGRAY);
				}
			});
	}

	public void sbrowserFixes(LoadPackageParam lpparam) {
		// SBrowser fixes, disable desktop scrollbar and enable secret mode
		if (!lpparam.packageName.equals("com.sec.android.app.sbrowser"))
			return;

		Class<?> hookClass = null;

		hookClass = findClass("com.sec.android.app.sbrowser.SBrowserCommandLine",
				lpparam.classLoader);

		XposedBridge.hookAllMethods(hookClass, "setSamsungDexEnabled", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(false);
			}
		});

		hookClass = findClass("com.sec.android.app.sbrowser.utils.SBrowserFlags",
				lpparam.classLoader);

		XposedBridge.hookAllMethods(hookClass, "isSecretModeSupported", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(true);
			}
		});
	}

	public void weatherFixes(LoadPackageParam lpparam) {
		// Enable weather statusbar notification in Dex
		if (!lpparam.packageName.equals("com.sec.android.daemonapp"))
			return;

		Class<?> hookClass = null;

		hookClass = findClass("com.sec.android.daemonapp.app.notification.NotificationUtil",
				lpparam.classLoader);

		XposedBridge.hookAllMethods(hookClass, "isDesktopMode", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(false);
			}
		});

		hookClass = findClass("com.sec.android.daemonapp.app.notification.WeatherNotificationManager",
				lpparam.classLoader);

		XposedBridge.hookAllMethods(hookClass, "stopPanelOnGoingNoti", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(false);
			}
		});
	}
}
