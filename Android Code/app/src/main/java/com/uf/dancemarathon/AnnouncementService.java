package com.uf.dancemarathon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

public class AnnouncementService extends Service {
	
	private int lastSize;
	
	//Set up receiver to receive TIME_TICK intents
	private BroadcastReceiver receiver = new BroadcastReceiver(){
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			
			//The time ticks every minute
			if(intent.getAction().equals(Intent.ACTION_TIME_TICK))
			{
				new AnnouncementsLoader().execute();
			}
		}
		
	};;
		
	public AnnouncementService() {
		super();
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		super.onStartCommand(intent, flags, startId);
		this.registerReceiver(receiver, new IntentFilter(Intent.ACTION_TIME_TICK));
		lastSize = readCache().size();
		//Log.d("service","in start");
		return Service.START_STICKY;
	}

	
	/* (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		this.unregisterReceiver(receiver);
	}


	
	/**
	 * Read the cache for announcements
	 * @return The cache announcements if they exist. New arraylist otherwise.
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<Announcement> readCache()
	{
		Object o = CacheManager.readObjectFromCacheFile(this, "announcements");
		if(o != null)
			return (ArrayList<Announcement>) o;
		else
			return new ArrayList<Announcement>();
	}
	
	/**
	 * Get announcements and write to cache. 
	 * @return true if there are new announcements
	 */
	private boolean getNewAnnouncements()
	{
		ArrayList<Announcement> announcements = new ArrayList<Announcement>();
		boolean isNew = false;
		String path;
		try {
			path = new ConfigFileReader(this).getSetting("announcementsPath");
			URL url = new URL(path); //The path to the webservice 
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			
			//Parse JSON response
			String announcementsJSON = reader.readLine();
			// Log.d("json", eventsJSON);
			JSONArray arr = new JSONArray(announcementsJSON);
			announcements = parseAnnouncementsJSON(arr);
			
			//Log.d("service", String.valueOf(lastSize));
			//If new announcements have been found, update ments and cache
			if(announcements.size() > lastSize)
			{
				CacheManager.writeObjectToCacheFile(this, announcements, "announcements");
				isNew = true;
			}	
			lastSize = announcements.size();
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
		
		return isNew;
			
		
	}
	
	/**
	 * @param obj The JSON object containing the events
	 * @return An arraylist of events
	 * @throws JSONException if parse fails
	 */
	protected ArrayList<Announcement> parseAnnouncementsJSON(JSONArray arr) throws JSONException
	{
		ArrayList<Announcement> announcements = new ArrayList<Announcement>();
		for(int i = 0; i < arr.length(); i++)
		{
			String text = arr.getJSONObject(i).getString("text").trim();
			String date = arr.getJSONObject(i).getString("date").trim();
			
			try
			{
				Announcement a = new Announcement(text, date, "yyyy-MM-dd HH:mm:ss");
				if(a.hasOccurred())
					announcements.add(a);
			} catch (ParseException e)
			{
				// Log.d("Announcements Parsing", "Failed to parse announcement" + text);
			}
		}
		
		return announcements; 
	}

	
	/**
	 * This method creates a pending intent to open the HomeActivity when
	 * the notification is pressed.
	 * @return The pending intent to use
	 */
	private PendingIntent getMainPendingIntent()
	{
		Intent intent = new Intent(this, HomeActivity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(HomeActivity.class);
		stackBuilder.addNextIntent(intent);
		PendingIntent pIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		
		return pIntent;
	}
	
	@Override	
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	/**
	 * This class handles loading announcements and notifying the user if there are new announcements.
	 */
	private class AnnouncementsLoader extends AsyncTask<Void, Double, Boolean>
	{
		
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
		 */
		//This method will perform the request to the web service and try to obtain the events
		@Override
		protected Boolean doInBackground(Void... params)
		{
			return getNewAnnouncements();
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Boolean newAnnouncementsExist) {
			// TODO Auto-generated method stub
			if(newAnnouncementsExist)
				notifyUser(18);
		}
		
		private void notifyUser(int mId)
		{
			//Set vibration pattern
			long[] pattern = {1, 1000};
			
			//Set the pending intent for when the user clicks the notification
			PendingIntent pIntent = getMainPendingIntent();
					
			NotificationCompat.Builder mBuilder =
			        new NotificationCompat.Builder(AnnouncementService.this)
			        .setSmallIcon(R.drawable.launcher_icon)
			        .setContentTitle("New Announcements Available!")
			        .setContentText("Click to see new announcements")
			        .setAutoCancel(true)
			        .setVibrate(pattern)
			        .setContentIntent(pIntent);
				
				NotificationManager mNotificationManager =
					    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					// mId allows you to update the notification later on.
					mNotificationManager.notify(mId, mBuilder.build());
		}
		
		
	}

}
