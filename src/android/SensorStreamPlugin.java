package hu.sensorStream.client;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.http.cookie.SM;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
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
	private SensorManager mSensorManager;

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
						Log.d("CordovaLog", "Connecting to " + ipAddress + ":"
								+ port);
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
						Log.d("CordovaLog", "Stop streaming");
						callbackContext.success();
					}
				});
				return true;
			} else if (ACTION_GET_INFO.equals(action)) {
				cordova.getThreadPool().execute(new Runnable() {
					@Override
					public void run() {
						String info = getInfo();
						Log.d("CordovaLog", "Get sensor informations.");
						callbackContext.success(info);
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
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void startStream() {
		if (out == null && socket != null) {
			try {
				out = new PrintWriter(socket.getOutputStream(), true);
				mSensorManager = (SensorManager) cordova.getActivity()
						.getSystemService(Context.SENSOR_SERVICE);
				Sensor mLinearAcceleration = mSensorManager
						.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				mSensorManager.registerListener(
						new LinearAccelerationListener(), mLinearAcceleration,
						SensorManager.SENSOR_DELAY_FASTEST);
				Sensor mGravity = mSensorManager
						.getDefaultSensor(Sensor.TYPE_GRAVITY);
				mSensorManager.registerListener(
						new GravityListener(), mGravity,
						SensorManager.SENSOR_DELAY_FASTEST);
				Sensor mMagneticField = mSensorManager
						.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
				mSensorManager.registerListener(
						new MagneticFieldListener(), mMagneticField,
						SensorManager.SENSOR_DELAY_FASTEST);
				Sensor mRotationVector = mSensorManager
						.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
				mSensorManager.registerListener(
						new RotationVectorListener(), mRotationVector,
						SensorManager.SENSOR_DELAY_FASTEST);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		running = true;
		// streamData();
	}

	public void stopStream() {
		running = false;
		if (out != null) {
			out.close();
		}
	}

	protected class Stream implements Runnable {
		@Override
		public void run() {
			double data;
			Random rand = new Random();
			while (running) {
				data = 50.0 + (rand.nextDouble() * 100);
				out.println(data);
				try {
					Thread.sleep(rand.nextInt(30) + 20);
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
		StringBuilder builder = new StringBuilder("<ul>");
		mSensorManager = (SensorManager) cordova.getActivity()
				.getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> deviceSensors = mSensorManager
				.getSensorList(Sensor.TYPE_ALL);
		for (Sensor sensor : deviceSensors) {
			builder.append("<li>");
			builder.append(sensor.getName());
			builder.append("(");
			builder.append(sensor.getType());
			builder.append(")");
			builder.append(": ");
			builder.append(sensor.getMaximumRange());
			builder.append(" unit maxRange, ");
			builder.append(sensor.getMinDelay());
			builder.append(" ms minDelay, ");
			builder.append(sensor.getResolution());
			builder.append(" unit resolution, ");
			builder.append(sensor.getVendor());
			builder.append(" vendor, ");
			builder.append(sensor.getVersion());
			builder.append(" version");
			builder.append("</li>");
		}
		builder.append("</ul>");
		return builder.toString();
	}

	protected class LinearAccelerationListener implements SensorEventListener {
		float prevNs;

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			Log.d("CordovaLog", "Sensor accuracy changed to: " + accuracy);
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			Log.d("CordovaLog", "new datas from sensor!");
			String type = "Acceleration";
			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];
			float ns = event.timestamp;
			float deltat = ns - prevNs;
			if (prevNs == 0) {
				deltat = 0;
			}
			prevNs = ns;
			if (running) {
				JSONObject result = new JSONObject();
				try {
					result.put("type", type);
					result.put("x", x);
					result.put("y", y);
					result.put("z", z);
					result.put("deltat", deltat);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				out.println(result.toString());
			}
		}

	}
	protected class GravityListener implements SensorEventListener {
		float prevNs;

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			Log.d("CordovaLog", "Sensor accuracy changed to: " + accuracy);
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			Log.d("CordovaLog", "new datas from sensor!");
			String type = "Gravity";
			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];
			float ns = event.timestamp;
			float deltat = ns - prevNs;
			if (prevNs == 0) {
				deltat = 0;
			}
			prevNs = ns;
			if (running) {
				JSONObject result = new JSONObject();
				try {
					result.put("type", type);
					result.put("x", x);
					result.put("y", y);
					result.put("z", z);
					result.put("deltat", deltat);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				out.println(result.toString());
			}
		}

	}
	protected class MagneticFieldListener implements SensorEventListener {
		float prevNs;

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			Log.d("CordovaLog", "Sensor accuracy changed to: " + accuracy);
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			Log.d("CordovaLog", "new datas from sensor!");
			String type = "MagneticField";
			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];
			float ns = event.timestamp;
			float deltat = ns - prevNs;
			if (prevNs == 0) {
				deltat = 0;
			}
			prevNs = ns;
			if (running) {
				JSONObject result = new JSONObject();
				try {
					result.put("type", type);
					result.put("x", x);
					result.put("y", y);
					result.put("z", z);
					result.put("deltat", deltat);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				out.println(result.toString());
			}
		}

	}
	protected class RotationVectorListener implements SensorEventListener {
		float prevNs;

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			Log.d("CordovaLog", "Sensor accuracy changed to: " + accuracy);
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			Log.d("CordovaLog", "new datas from sensor!");
			String type = "RotationVector";
			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];
			float ns = event.timestamp;
			float deltat = ns - prevNs;
			if (prevNs == 0) {
				deltat = 0;
			}
			prevNs = ns;
			if (running) {
				JSONObject result = new JSONObject();
				try {
					result.put("type", type);
					result.put("x", x);
					result.put("y", y);
					result.put("z", z);
					result.put("deltat", deltat);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				out.println(result.toString());
			}
		}

	}
}