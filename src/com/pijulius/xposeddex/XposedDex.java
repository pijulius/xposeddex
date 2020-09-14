package com.pijulius.xposeddex;

import static de.robv.android.xposed.XposedHelpers.findClass;

import java.io.File;

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

		if (settings.getBoolean("makeAppsImmersive", true))
			makeAppsImmersive(lpparam);

		if (settings.getBoolean("fixSBrowser", true))
			sbrowserFixes(lpparam);
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		if (settings == null)
			settings = new XSharedPreferences(new File(settingsFile));

		taskbarResourceFixes(resparam);
	}

	public void makeAppsImmersive(LoadPackageParam lpparam) {
		Class<?> hookClass = findClass("com.android.internal.policy.MultiWindowDecorSupport",
				lpparam.classLoader);

		XposedBridge.hookAllMethods(hookClass, "isImmersiveMode", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(true);
			}
		});
	}

	public void taskbarResourceFixes(InitPackageResourcesParam resparam) {
		if (!resparam.packageName.equals("com.samsung.desktopsystemui"))
			return;

		final boolean hideTasksButton = settings.getBoolean("hideTasksButton", true);
		final boolean hideTaskbarSeparators = settings.getBoolean("hideTaskbarSeparators", true);
		final boolean spaceAboveAppDock = settings.getBoolean("fixTaskbar", true);

		if (hideTasksButton || hideTaskbarSeparators || spaceAboveAppDock) {
			resparam.res.hookLayout("com.samsung.desktopsystemui", "layout", "taskbar", new XC_LayoutInflated() {
				@Override
				public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
					View view = null;

					if (hideTasksButton) {
						view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
							"recent_apps", "id", "com.samsung.desktopsystemui"));

						view.setVisibility(View.GONE);
						view.getLayoutParams().width = 0;

						MarginLayoutParams marginParams = (MarginLayoutParams)view.getLayoutParams();
						marginParams.setMarginStart(0);
						marginParams.setMarginEnd(0);
					}

					if (hideTaskbarSeparators) {
						view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
							"all_apps_divider", "id", "com.samsung.desktopsystemui"));

						view.setVisibility(View.GONE);
						view.getLayoutParams().width = 0;
						view.setPadding(0, 0, 0, 0);

						MarginLayoutParams marginParams = (MarginLayoutParams)view.getLayoutParams();
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
					}

					if (spaceAboveAppDock) {
						view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
							"appdock_container", "id", "com.samsung.desktopsystemui"));

						view.setPadding(0, 4, 0, 0);
					}
				}
			});
		}

		if (settings.getBoolean("hideNoSIMIcon", true)) {
			resparam.res.hookLayout("com.samsung.desktopsystemui", "layout", "desk_signal_cluster_view_25", new XC_LayoutInflated() {
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

		if (settings.getBoolean("hideBatteryPercentage", true)) {
			resparam.res.hookLayout("com.samsung.desktopsystemui", "layout", "battery_percentage_view", new XC_LayoutInflated() {
				@Override
				public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
					View view = null;

					view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
						"battery_percentage_view", "id", "com.samsung.desktopsystemui"));

					view.setVisibility(View.GONE);
				}
			});

			resparam.res.hookLayout("com.samsung.desktopsystemui", "layout", "desk_system_icons", new XC_LayoutInflated() {
				@Override
				public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
					View view = null;

					view = (View)liparam.view.findViewById(liparam.res.getIdentifier(
						"system_icons_battery_container", "id", "com.samsung.desktopsystemui"));

					MarginLayoutParams marginParams = (MarginLayoutParams)view.getLayoutParams();
					marginParams.setMarginStart(10);
					marginParams.setMarginEnd(10);
				}
			});
		}
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
}
