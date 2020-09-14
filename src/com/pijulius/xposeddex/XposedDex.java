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
import android.view.ViewGroup.MarginLayoutParams;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
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

		if (settings.getBoolean("fixTaskbar", true))
			taskbarFixes(lpparam);

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

		taskbarResourceFixes(resparam);

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

	public void taskbarFixes(LoadPackageParam lpparam) {
		// Taskbar fixes, disable setting the black backgrounds when an app is maximized
		if (!lpparam.packageName.equals("com.samsung.desktopsystemui"))
			return;

		Class<?> hookClass = null;

		hookClass = findClass("com.samsung.desktopsystemui.statusbar.phone.taskbar.views.TaskBarView",
				lpparam.classLoader);

		XposedBridge.hookAllMethods(hookClass, "setDarkBackground", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(false);
			}
		});

		hookClass = findClass("com.samsung.desktopsystemui.statusbar.phone.taskbar.desk.DeskStatusBarView",
				lpparam.classLoader);

		XposedBridge.hookAllMethods(hookClass, "setDarkBackground", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(false);
			}
		});
	}

	public void taskbarResourceFixes(InitPackageResourcesParam resparam) {
		if (!resparam.packageName.equals("com.samsung.desktopsystemui"))
			return;

		final boolean hideDexLogo = settings.getBoolean("hideDexLogo", true);
		final boolean hideTasksButton = settings.getBoolean("hideTasksButton", true);
		final boolean hideAppOverflowButtons = settings.getBoolean("hideAppOverflowButtons", true);
		final boolean hideStatusBarSeparators = settings.getBoolean("hideStatusBarSeparators", true);

		if (hideDexLogo || hideTasksButton || hideAppOverflowButtons || hideStatusBarSeparators) {
			resparam.res.hookLayout("com.samsung.desktopsystemui", "layout", "taskbar", new XC_LayoutInflated() {
				@Override
				public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
					View view = null;

					if (hideDexLogo) {
						view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
							"dex_community", "id", "com.samsung.desktopsystemui"));

						view.setVisibility(View.GONE);
						view.getLayoutParams().width = 0;

						view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
							"dex_community_divider", "id", "com.samsung.desktopsystemui"));

						view.setVisibility(View.GONE);
						view.getLayoutParams().width = 0;

						MarginLayoutParams marginParams = (MarginLayoutParams)view.getLayoutParams();
						marginParams.setMarginStart(0);
						marginParams.setMarginEnd(0);
					}

					if (hideTasksButton) {
						view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
							"recent_apps", "id", "com.samsung.desktopsystemui"));

						view.setVisibility(View.GONE);
						view.getLayoutParams().width = 0;

						MarginLayoutParams marginParams = (MarginLayoutParams)view.getLayoutParams();
						marginParams.setMarginStart(0);
						marginParams.setMarginEnd(0);
					}

					if (hideAppOverflowButtons) {
						view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
							"button_scroll_left", "id", "com.samsung.desktopsystemui"));

						view.setVisibility(View.GONE);
						view.getLayoutParams().width = 0;

						view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
							"button_scroll_right", "id", "com.samsung.desktopsystemui"));

						view.setVisibility(View.GONE);
						view.getLayoutParams().width = 0;
					}

					if (hideStatusBarSeparators) {
						view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
							"size_control", "id", "com.samsung.desktopsystemui"));

						MarginLayoutParams marginParams = (MarginLayoutParams)view.getLayoutParams();
						marginParams.setMarginStart(0);
						marginParams.setMarginEnd(0);

						view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
							"desk_status_bar_container", "id", "com.samsung.desktopsystemui"));

						marginParams = (MarginLayoutParams)view.getLayoutParams();
						marginParams.setMarginStart(0);
						marginParams.setMarginEnd(0);

						view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
							"taskbar_divider", "id", "com.samsung.desktopsystemui"));

						view.setVisibility(View.GONE);
						view.getLayoutParams().width = 0;
						view.setPadding(0, 0, 0, 0);

						marginParams = (MarginLayoutParams)view.getLayoutParams();
						marginParams.setMarginStart(0);
						marginParams.setMarginEnd(0);

						view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
							"global_function", "id", "com.samsung.desktopsystemui"));

						view.setPadding(0, view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
					}
				}
			});

			if (hideStatusBarSeparators) {
				resparam.res.hookLayout("com.samsung.desktopsystemui", "layout", "desk_status_bar", new XC_LayoutInflated() {
					@Override
					public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
						View view = null;

						view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
							"notification_icon_area", "id", "com.samsung.desktopsystemui"));

						view.setPadding(0, view.getPaddingTop(), 0, view.getPaddingBottom());

						MarginLayoutParams marginParams = (MarginLayoutParams)view.getLayoutParams();
						marginParams.setMarginStart(0);
						marginParams.setMarginEnd(0);

						view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
							"system_icon_area_outer", "id", "com.samsung.desktopsystemui"));

						view.setPadding(0, view.getPaddingTop(), 0, view.getPaddingBottom());
					}
				});

				resparam.res.setReplacement(
					"com.samsung.desktopsystemui", "drawable", "desk_panel_view_bg", new XResources.DrawableLoader() {
						@Override
						public Drawable newDrawable(XResources res, int id) throws Throwable {
							return new ColorDrawable(Color.TRANSPARENT);
						}
					});

				resparam.res.setReplacement(
					"com.samsung.desktopsystemui", "drawable", "quick_connect_hidden_icon", new XResources.DrawableLoader() {
						@SuppressWarnings("deprecation")
						@Override
						public Drawable newDrawable(XResources res, int id) throws Throwable {
							return res.getDrawable(android.R.drawable.arrow_up_float);
						}
					});

				resparam.res.setReplacement(
					"com.samsung.desktopsystemui", "drawable", "stat_notify_more_for_desk", new XResources.DrawableLoader() {
						@SuppressWarnings("deprecation")
						@Override
						public Drawable newDrawable(XResources res, int id) throws Throwable {
							return res.getDrawable(android.R.drawable.arrow_up_float);
						}
					});
			}
		}

		if (settings.getBoolean("hideNoSIMIcon", true)) {
			resparam.res.hookLayout("com.samsung.desktopsystemui", "layout", "desk_signal_cluster_view", new XC_LayoutInflated() {
				@Override
				public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
					View view = null;

					view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
						"no_sims_combo", "id", "com.samsung.desktopsystemui"));

					view.setVisibility(View.GONE);
					view.getLayoutParams().width = 0;

					view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
						"sec_no_sim_slot1", "id", "com.samsung.desktopsystemui"));

					view.setVisibility(View.GONE);
					view.getLayoutParams().width = 0;

					view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
						"sec_no_sim_slot2", "id", "com.samsung.desktopsystemui"));

					view.setVisibility(View.GONE);
					view.getLayoutParams().width = 0;
				}
			});
		}
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

		//hookClass = findClass("com.samsung.android.weather.infrastructure.system.android.DesktopModeManager",
		//hookClass = findClass("com.samsung.android.weather.infrastructure.system.sdl.DesktopModeManager",
		hookClass = findClass("com.samsung.android.weather.infrastructure.system.sep.DesktopModeManager",
				lpparam.classLoader);

		XposedBridge.hookAllMethods(hookClass, "isDesktopMode", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(false);
			}
		});
	}
}
