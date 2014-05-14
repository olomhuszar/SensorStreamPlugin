package hu.sensorStream.client;

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


import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;



public class SensorStreamPlugin extends CordovaPlugin {
	private static final String ACTION_START_STREAM = "startStream";
	private static final String ACTION_STOP_STREAM = "stopStream";
	private static final String ACTION_CONNECT = "connect";
	private static final String ACTION_DISCONNECT = "disconnect";
	private static final String ACTION_GET_INFO = "getInfo";
	private String ipAddress = null;
	private int port = 0;
	private Socket socket;
	private Stream stream;
	private Thread streamThread = null;
	private PrintWriter out = null;
	private boolean running = false;

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
						connect();
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
				    @Override
					public void run() {
						startStream();
						Log.d("CordovaLog", "Starting stream");
						callbackContext.success(); 
				    }
				});
				return true;
			} else if (ACTION_STOP_STREAM.equals(action)) {
				cordova.getThreadPool().execute(new Runnable() {
				    @Override
					public void run() {
						String info = getInfo();
						Log.d("CordovaLog", "Stop streaming");
						callbackContext.success(); 
				    }
				});
				return true;
			} else if (ACTION_GET_INFO.equals(action)) {
				cordova.getThreadPool().execute(new Runnable() {
				    @Override
					public void run() {
						stopStream();
						Log.d("CordovaLog", "Get sensor informations.");
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
	public void connect() {		
		try {
			socket = new Socket(ipAddress, port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void disconnect() {
		if(socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	}
	public void startStream() {
		if( out == null && socket != null ) {
			try {
				out = new PrintWriter(socket.getOutputStream(), true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		running = true;
		streamData();
	}
	public void stopStream() {
		running = false;
		if(out != null) {
			out.close();
		}
	}
	protected class Stream implements Runnable {
		@Override
		public void run() {
			double data;
			Random rand = new Random();
			while(running) {
	        	data = 50.0 + ( rand.nextDouble() * 100 );
	        	out.println(data);
				try {
					Thread.sleep(rand.nextInt(30)+20);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	public void streamData() {
		streamThread = new Thread(new Stream());
		streamThread.start();
	}
	public String getInfo() {
		String info = new String();
		return info;
	}
}