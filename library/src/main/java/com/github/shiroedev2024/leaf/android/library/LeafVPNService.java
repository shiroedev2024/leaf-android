/* Created by Mohammad Reza Mokhtarabadi <mmokhtarabadi@gmail.com> (C) 2023 ALL RIGHT RESERVED*/
package com.github.shiroedev2024.leaf.android.library;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class LeafVPNService extends VpnService {

	boolean enableIpv6 = true;
	boolean preferIpv6 = false;

	static {
		System.loadLibrary("native");
	}

	public static final String ACTION_STOP = LeafVPNService.class.getSimpleName() + "_stop";
	public static final String ACTION_START = LeafVPNService.class.getSimpleName() + "_start";

	private ConnectivityManager connectivity;
	private Notification notification;

	private boolean networkConnectivityMonitorStarted = false;

	private native void init();
	private native boolean setProtectSocketCallback(String methodName);
	private native int runLeaf(String config);
	private native boolean isLeafRunning();
	private native int reloadLeaf();
	private native boolean stopLeaf();

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			if (intent.getAction().equals(ACTION_STOP)) {
				stop();
				return START_NOT_STICKY;
			}
		}

		start();
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		init();

		connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		createNotification();
	}

	@Override
	public void onRevoke() {
		Log.w("LeafVPNService", "onRevoke");
		stop();
		//super.onRevoke();
	}

	@Override
	public void onLowMemory() {
		Log.w("LeafVPNService", "onLowMemory");
		stop();
		super.onLowMemory();
	}

	@Override
	public void onDestroy() {
		Log.w("LeafVPNService", "onDestroy");
		super.onDestroy();
	}

	private void setEnvironmentVariables() {
		HashMap<String, String> environmentVariables = new HashMap<>();
		environmentVariables.put("LOG_NO_COLOR", "true");
		environmentVariables.put("USER_AGENT", "Leaf Client For Android");

		//if (BuildConfig.DEBUG) {
			environmentVariables.put("RUST_BACKTRACE", "1");
		//}

		if (enableIpv6) {
			environmentVariables.put("ENABLE_IPV6", "true");
			if (preferIpv6) {
				environmentVariables.put("PREFER_IPV6", "true");
			}
		}

		for (String key : environmentVariables.keySet()) {
			try {
				Os.setenv(key, environmentVariables.get(key), true);
			} catch (ErrnoException e) {
				e.printStackTrace();
			}
		}
	}

	@Keep
	public boolean protectSocket(int fd) {
		return LeafVPNService.this.protect(fd);
	}

	private void start() {
		startForeground(1, notification);
		setEnvironmentVariables();
		setProtectSocketCallback("protectSocket");

		Intent prepare = prepare(this);
		if (prepare != null) {
			Log.e("LeafVPNService", "create vpn permission not granted");
			sendDataToActivity("permission_error");
			stop();
			return;
		}

		Builder builder = new Builder().setSession("Leaf VPN")
				.setMtu(1500)
				.addAddress("10.0.0.33", 24)
				.addDnsServer("10.0.0.1")
				.addRoute("0.0.0.0", 0);

		if (enableIpv6) {
			builder.addAddress("2001:2::2", 64)
					.addRoute("::", 0);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			builder.setMetered(false);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Network activeNetwork = connectivity.getActiveNetwork();
			if (activeNetwork != null) {
				builder.setUnderlyingNetworks(new Network[]{activeNetwork});
			}
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			startNetworkConnectivityMonitor();
		}

		int fd = builder.establish().detachFd();

		Log.d("LeafVPNService", "builder established");

		new Thread(() -> {
			// TODO: 4/21/24 test config here
			sendDataToActivity("started");

			int result = runLeaf("[General]\n" +
					"loglevel = info\n" +
					"\n" +
					"dns-server = tcp://127.0.0.1:5353\n" +
					"routing-domain-resolve = true\n" +
					"always-fake-ip = *\n" +
					"\n" +
					"tun=" + fd + "\n" +
					"\n" +
					"dns-interface = 127.0.0.1\n" +
					"dns-port = 5353\n" +
					"\n" +
					"[Env]\n" +
					"ENABLE_IPV6=" + enableIpv6 + "\n" +
					"\n" +
					"[Dns]\n" +
					"DOH1 = doh, 104.21.233.179, 443, domain=cloudflare-dns.com, sni=cloudflare-dns.com, path=/dns-query, post=true, fragment=true, fragment-packets=0-1, fragment-length=6-9, fragment-interval=8-12\n" +
					"DOH2 = doh, 104.21.233.180, 443, domain=cloudflare-dns.com, sni=cloudflare-dns.com, path=/dns-query, post=true, fragment=true, fragment-packets=0-1, fragment-length=6-9, fragment-interval=8-12\n" +
					"\n" +
					"[Proxy]\n" +
					"GB1 = trojan, 104.21.233.179, 443, password=none, tls=true, fragment=true, fragment-packets=0-1, fragment-length=6-19, fragment-interval=8-12, sni=new.myfakefirstdomaincard.top, ws=true, ws-host=new.myfakefirstdomaincard.top, ws-path=/chat, amux=true, amux-max=16, amux-con=4\n" +
					"GB2 = trojan, 104.21.233.180, 443, password=none, tls=true, fragment=true, fragment-packets=0-1, fragment-length=6-19, fragment-interval=8-12, sni=new.myfakefirstdomaincard.top, ws=true, ws-host=new.myfakefirstdomaincard.top, ws-path=/chat, amux=true, amux-max=16, amux-con=4\n" +
					"\n" +
					"[Proxy Group]\n" +
					"Proxy = failover, GB1, GB2\n" +
					"\n" +
					"[Rule]\n" +
					"FINAL, Proxy\n");

			Log.i("LeafVPNService", "start leaf with result: " + result);
		}).start();
	}

	private void sendDataToActivity(String data) {
		Intent intent = new Intent(getPackageName() + ".ACTION_DATA_FROM_SERVICE_TO_ACTIVITY");
		intent.putExtra("data", data);
		sendBroadcast(intent);
	}

	private void reload() {
		new Thread(() -> {
			int result = reloadLeaf();
			Log.i("LeafVPNService", "reload leaf with result: " + result);

			sendDataToActivity("reloaded");
		}).start();
	}

	private void stop() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			stopNetworkConnectivityMonitor();
		}

		if (isLeafRunning()) {
			boolean result = stopLeaf();
			Log.i("LeafVPNService", "stop leaf with result: " + result);
		}

		sendDataToActivity("stopped");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			stopForeground(STOP_FOREGROUND_REMOVE);
		} else {
			stopForeground(true);
		}

		// kill my process
		//killMyProcess();
	}

	private void createNotification() {
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

		NotificationChannelCompat notificationChannel = new NotificationChannelCompat.Builder("vpn_service",
				NotificationManagerCompat.IMPORTANCE_DEFAULT).setName(getString(R.string.vpn_service_name)).build();
		notificationManager.createNotificationChannel(notificationChannel);

		Intent stopIntent = new Intent(this, LeafVPNService.class);
		stopIntent.setAction(ACTION_STOP);

		int flags = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			flags |= PendingIntent.FLAG_IMMUTABLE;
		}

		PendingIntent stopPendingIntent;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			stopPendingIntent = PendingIntent.getForegroundService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE);
		} else {
			stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, flags);
		}

		notification = new NotificationCompat.Builder(this, notificationChannel.getId())
				.setContentTitle(getString(R.string.vpn_service))
				.setContentText(getString(R.string.vpn_service_description)).setSmallIcon(R.drawable.round_vpn_lock_24)
				.addAction(R.drawable.round_stop_24, getString(R.string.stop), stopPendingIntent).setOnlyAlertOnce(true)
				.setOngoing(true).setAutoCancel(true).setShowWhen(false)
				.setPriority(NotificationCompat.PRIORITY_DEFAULT).setDefaults(NotificationCompat.FLAG_ONLY_ALERT_ONCE)
				.build();
	}

	private final IMyAidlInterface.Stub binder = new IMyAidlInterface.Stub() {
		@Override
		public boolean isRunning() throws RemoteException {
			boolean isLeafRunning = isLeafRunning();
			Log.d("LeafVPNService", "is leaf running: " + isLeafRunning);
			return isLeafRunning;
		}

		@Override
		public void reload() throws RemoteException {
			LeafVPNService.this.reload();
		}

		@Override
		public void stop() throws RemoteException {
			LeafVPNService.this.stop();
		}
	};

	@RequiresApi(api = Build.VERSION_CODES.P)
	private void startNetworkConnectivityMonitor() {
		NetworkRequest.Builder builder = new NetworkRequest.Builder()
				.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
				.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);

		if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) { // workarounds for OEM bugs
			builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
			builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
		}

		try {
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
				connectivity.registerNetworkCallback(defaultNetworkRequest, defaultNetworkCallback);
			} else {
				connectivity.requestNetwork(defaultNetworkRequest, defaultNetworkCallback);
			}
			networkConnectivityMonitorStarted = true;
		} catch (SecurityException se) {
			se.printStackTrace();
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.P)
	private void stopNetworkConnectivityMonitor() {
		try {
			if (networkConnectivityMonitorStarted) {
				connectivity.unregisterNetworkCallback(defaultNetworkCallback);
				networkConnectivityMonitorStarted = false;
			}
		} catch (Exception e) {
			// Ignore, monitor not installed if the connectivity checks failed.
		}
	}

	@RequiresApi(Build.VERSION_CODES.P)
	private final NetworkRequest defaultNetworkRequest = new NetworkRequest.Builder()
			.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
			.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED).build();

	@RequiresApi(Build.VERSION_CODES.P)
	private final ConnectivityManager.NetworkCallback defaultNetworkCallback = new ConnectivityManager.NetworkCallback() {
		@Override
		public void onAvailable(@NonNull Network network) {
			setUnderlyingNetworks(new Network[]{network});
		}

		@Override
		public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
			// it's a good idea to refresh capabilities
			setUnderlyingNetworks(new Network[]{network});
		}

		@Override
		public void onLost(@NonNull Network network) {
			setUnderlyingNetworks(null);
		}
	};
}
