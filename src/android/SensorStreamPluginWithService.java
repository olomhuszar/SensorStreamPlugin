package hu.sensorStream.host;

import hu.sensorStream.host.StreamService;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;



public class SensorStreamPlugin extends CordovaPlugin {
	public static final String ACTION_START_STREAM = "startStream";
	public static final String ACTION_STOP_STREAM = "stopStream";
	public static final String ACTION_CONNECT = "connect";
	public static final String ACTION_DISCONNECT = "disconnect";
	private Intent serviceHandler;
	private String ipAddress = null;
	private int port = null;
	private MystreamBinder streamBinder = null;

	public SensorStreamPlugin() {
	    serviceHandler = new Intent(cordova.getActivity(), StreamService.class);
	}
	@Override
	public boolean execute(String action, JSONArray args,
			final CallbackContext callbackContext) throws JSONException {
		try {
			if (ACTION_CONNECT.equals(action)) {
				JSONObject arguments = args.getJSONObject(0);
				ipAddress = arguments.getString("ipAddress");
				port = arguments.getInt("port");
				cordova.getThreadPool().execute(new Runnable() {
				    public void run() {
						connect(ipAddress, port);
						Log.d("CordovaLog", "Connecting to " + ipAddress + ":" + port);
						callbackContext.success(); 
				    }
				});
				return true;
			} else if (ACTION_DISCONNECT.equals(action)) {
				cordova.getThreadPool().execute(new Runnable() {
				    public void run() {
						disconnect();
						Log.d("CordovaLog", "Disconnect from server");
						callbackContext.success(); 
				    }
				});
				return true;
			} else if (ACTION_START_STREAM.equals(action)) {
				cordova.getThreadPool().execute(new Runnable() {
				    public void run() {
						startStream();
						Log.d("CordovaLog", "Starting stream");
						callbackContext.success(); 
				    }
				});
				return true;
			} else if (ACTION_STOP_STREAM.equals(action)) {
				cordova.getThreadPool().execute(new Runnable() {
				    public void run() {
						disconnect();
						Log.d("CordovaLog", "Stop streaming");
						callbackContext.success(); 
				    }
				});
				return true;
			}
			callbackContext.error("Invalid action");
			return false;
		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
			callbackContext.error(e.getMessage());
			return false;
		}
	}
	public void connect( String ipAddress, int port) {
	    if(streamBinder != null && streamBinder.isBinderAlive()) {
	    	streamBinder.setParams(ipAddress, port);
	    	streamBinder.connect();
	    } else {
			Log.d("CordovaLog", "Error disconnecting: binder unreachable");
		}
	    cordova.getActivity().bindService(serviceHandler, mConnection, Context.BIND_AUTO_CREATE);
	}
	public void disconnect() {
		if(streamBinder != null && streamBinder.isBinderAlive()) {
	    	streamBinder.disconnect();
		} else {
			Log.d("CordovaLog", "Error disconnecting: binder unreachable");
		}
	    cordova.getActivity().disconnect(serviceHandler);
	}
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			streamBinder = (MystreamBinder) service;
	    	streamBinder.setServerAddress(serverAddress);
			streamBinder.setToken(token);
			Log.d("CordovaLog", "Token at serviceConnected: " + token);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			token = streamBinder.getToken();
			Log.d("CordovaLog", "Token at serviceDisconnected: " + token);
			streamBinder = null;
		}
		
	};
}