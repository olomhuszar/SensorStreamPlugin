package hu.sensorStream.host;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

import org.json.JSONObject;
import org.json.JSONException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


public class StreamService extends Service {
    private String token = null;
    private String serverAddress = null;
    private Boolean isPolling = false;
    private final IBinder mystreamBinder = new MystreamBinder();
    private URL url;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("CordovaLog", "Polling started");
        Calendar cal = Calendar.getInstance();
        Intent intent = new Intent(this, StreamService.class);
        PendingIntent pintent = PendingIntent.getService(this, 0, intent, 0);
        AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 3000, pintent); 
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if( token == null ) return START_STICKY;
        try {
            String fullUrl = serverAddress + "?silent=1&token=" +  token;
            Log.d("CordovaLog", "Polling: " + fullUrl );
            url = new URL(fullUrl);
            isPolling = true;
            URLConnection conn = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            String status = null;
            while ((inputLine = br.readLine()) != null) {
                    Log.d("CordovaLog", "Got: " + inputLine);
                    JSONObject jsonObject = new JSONObject(inputLine);
                    token = jsonObject.getString("token");
                    status = jsonObject.getString("type");
            }
            if(!status.equals("nothing")) {
                Log.d("CordovaLog", "Should sign now");
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                Toast.makeText(getApplicationContext(),"Önnek új eseménye értkezett, téjen vissza a Spot!-hoz",Toast.LENGTH_LONG).show();
            }
            br.close();
        } catch (MalformedURLException e) {
            Log.d("CordovaLog", "Polling failed, malformed....");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d("CordovaLog", "Polling failed, IO....");
            e.printStackTrace();
        } catch (JSONException e) {
            Log.d("CordovaLog", "Polling failed, json....");
            e.printStackTrace();
        }
        isPolling = false;
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        ((MystreamBinder) mystreamBinder).stopStream();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mystreamBinder;
    }
    public class MystreamBinder extends Binder {
        public void setToken(String t) {
            token = t;
        }
        public String getToken() {
            return token;
        }
        public Boolean tokenSafe() {
            return !isPolling;
        }
        public void setServerAddress(String address) {
            serverAddress = address;
        }
        public void stopStream() {
            Intent intentstop = new Intent(StreamService.this, StreamService.class);
            PendingIntent senderstop = PendingIntent.getService(StreamService.this,0, intentstop, 0);
            AlarmManager alarmManagerstop = (AlarmManager) StreamService.this.getSystemService(Context.ALARM_SERVICE);
            alarmManagerstop.cancel(senderstop);
            Log.d("CordovaLog", "Polling ended");
        }
    }
}