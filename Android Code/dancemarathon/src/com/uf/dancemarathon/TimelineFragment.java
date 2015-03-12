package com.uf.dancemarathon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;




/**
 * A {@link Fragment} subclass which is responsible for displaying the timeline information. It uses
 * an {@link AsyncTask} to load the data and then it updates the UI accordingly.
 * 
 */
public class TimelineFragment extends Fragment
{
	private Context c;
	/**
	 * The list of Events
	 */
	private ArrayList<Event> events;
	
	/**
	 * Flag stating whether or not the event list is currently in the refresh process.
	 * Useful so we know in the async task whether a refresh or initial request was made.
	 */
	private boolean isRefreshing = false;
	/**
	 * The loader which performs the async load operation.
	 */
	private EventLoader loader;
	
	private AlertDialog mFilterDialog;
	private Button mFilterButton;
	private ListView mEventListView;
	private TimelineAdapter mEventAdapter;
	private SwipeRefreshLayout mListLayout;
	
	public TimelineFragment()
	{
		// Required empty public constructor
	}

	/**
	 * This method is necessary because an empty, no argument constructor must be provided
	 * for a fragment in Android
	 * @return A new instance of timeline fragment that is executing the load operation
	 */
	public static TimelineFragment newInstance(Context c)
	{
		TimelineFragment f = new TimelineFragment();
		f.c = c;
		f.isRefreshing = false;
		f.resetLoader();
		
		f.events = new ArrayList<Event>();
		
		return f;
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		 View v = inflater.inflate(R.layout.fragment_timeline, container, false);
		 
		 initializeEventListViews(v);
		 
		 //Initialize filter button
		 mFilterButton = (Button) v.findViewById(R.id.timeline_filter_button);
		 mFilterButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mFilterDialog.show();
			}
			 
		 });
		 
		 
		 //Load events
		 ArrayList<Event> cacheEvents = forceCacheRead();
		 if(cacheEvents != null)
		 {
			 events = cacheEvents;
			 String[] filterArray = getEventCategories(events);
			 createFilterDialog(filterArray);
			 showEventList(v, events);
		 }
		 else
			 forceEventListUpdate();
		 
		 
		 
		 return v;
	}
	
	private void initializeEventListViews(View v)
	{
		//Populate list view
		mEventAdapter = new TimelineAdapter(getActivity(), events);								 
		mEventListView = (ListView) v.findViewById(R.id.event_list); //Get the list view
		
		mEventListView.setAdapter(mEventAdapter);
		
		//Set click listener which will open event activity
		OnItemClickListener oc = new OnItemClickListener()
		{
			@Override
			//On item click, we start the individual event activity
			public void onItemClick(AdapterView<?> parent,
					View selectedView, int position, long selectedViewId)
			{
				Event e = mEventAdapter.getItem(position);
				Intent intent = new Intent(getActivity(), EventActivity.class);
				Bundle args = new Bundle();
				
				//Add event information to bundle
				args.putParcelable("event", (Parcelable) e);
				
				//Add bundle to intent
				intent.putExtras(args);
				startActivity(intent);
			}
			
		};
		mEventListView.setOnItemClickListener(oc);
		
		//Initialize Refresh layout
		mListLayout = (SwipeRefreshLayout) v.findViewById(R.id.event_list_container);
		
		mListLayout.setOnRefreshListener(new OnRefreshListener(){
			@Override
			public void onRefresh()
			{
				isRefreshing = true;
				forceEventListUpdate();
			}
		});
		
		//Set refresh layout colors
		mListLayout.setColorSchemeResources(R.color.dm_orange_primary, R.color.dm_blue_secondary, R.color.GreenYellow);
	}
	
	/**
	 * This method initializes the dialog that will be used to allow users to filter
	 * events by type.
	 */
	private void createFilterDialog(final String[] items)
	{
		 AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		 builder.setTitle(R.string.filter_dialog_title)
		 		.setItems(items, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						String category = items[which];
						
						ArrayList<Event> newList;
						if(!category.equals("All"))
							newList = filterEvents(events, category);
						else
							newList = events;
						
						
						showEventList(getView(), newList);
					}
				});
		 
		 mFilterDialog = builder.create();
	}
	
	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onStop()
	 */
	//Needed to override this method to cancel the refresh if this fragment is stopped
	public void onStop()
	{
		super.onStop();
		//If a loader exists, cancel its execution
		if(loader != null)
			loader.cancel(true);
		
		//Remove any hazy foreground that may exist and set the refresh layout status to not refreshing
		removeHazyForeground(getView());
		((SwipeRefreshLayout) getView().findViewById(R.id.event_list_container)).setRefreshing(false);
	}
	
	private void disableViewClicking()
	{
		if(mEventListView != null)
			mEventListView.setEnabled(false);
	}
	
	private void enableViewClicking()
	{
		if(mEventListView != null)
			mEventListView.setEnabled(true);
	}
	
	/**
	 * Get the unique categories
	 * @return A string array with the category names
	 */
	private String[] getEventCategories(ArrayList<Event> events){
		ArrayList<String> categories = new ArrayList<String>();
		categories.add("All");
		for(int i = 0; i < events.size(); i++)
		{
			Event e = events.get(i);
			String cat = e.getCategory();
			if(cat == null || categories.contains(cat))
			{
			}
			else
				categories.add(cat);
		}
		
		return categories.toArray(new String[categories.size()]);
	}
	
	/**
	 * Filter the events by a certain category
	 * @param events The events to filter
	 * @param category The category to filter by
	 * @return A new arraylist with the filtered events
	 */
	private ArrayList<Event> filterEvents(ArrayList<Event> events, String category)
	{
		ArrayList<Event> newList = new ArrayList<Event>();
		for(int i = 0; i < events.size(); i++)
		{
			Event e = events.get(i);
			String cat = e.getCategory();
		    if(cat != null)
		    {
		    	if(cat.equals(category))
		    		newList.add(e);
		    }
			else
			{
			}
		}
		
		return newList;
	}
	
	/**
	 * Show the event list on the view and hide the progress wheel
	 * @param v The view to modify
	 * @param events The events to show
	 */
	private void showEventList(final View v, ArrayList<Event> events)
	{
		mEventAdapter.clear();
		mEventAdapter.addAll(events);
		mEventAdapter.notifyDataSetChanged();
		
		//Set visibility in case they have been hidden
		mListLayout.setVisibility(View.VISIBLE);
		mFilterButton.setVisibility(View.VISIBLE);
		
		//Hide progress wheel
		ProgressBar bar = (ProgressBar) v.findViewById(R.id.progress_wheel);
		bar.setVisibility(View.GONE);
		
		
	}
	
	/**
	 * Show a load error page 
	 * @param v The view to show it on
	 */
	private void showLoadErrorPage(View v)
	{
		//Show error textview
		final TextView errorView = (TextView) v.findViewById(R.id.tline_load_error);
		errorView.setVisibility(View.VISIBLE);
		
		//Hide progress wheel
		final ProgressBar bar = (ProgressBar) v.findViewById(R.id.progress_wheel);
		bar.setVisibility(View.GONE);
		
		//Hide listview
		mListLayout.setVisibility(View.GONE);
		mFilterButton.setVisibility(View.GONE);
		
		//Show retry button
		final Button retry = (Button) v.findViewById(R.id.retry_button);
		//If button is clicked, try the load again
		retry.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v)
			{
				//Hide error textview
				errorView.setVisibility(View.GONE);
				//Hide retry button
				retry.setVisibility(View.GONE);
				
				//Show progress wheel animation
				bar.setVisibility(View.VISIBLE);
				
				//Execute load again
				loader = new EventLoader();
				loader.execute();
			}
			
		});
		retry.setVisibility(View.VISIBLE);	
	}
	
	/**
	 * Show a toast if the refresh operation fails
	 */
	private void showRefreshErrorToast()
	{
		Toast toast = Toast.makeText(c, "Could not refresh data", Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM, 0, 40);
		toast.show();
	}
	
	/**
	 * Shows a hazy view over all the other views
	 * @param v The container view
	 */
	private void showHazyForeground(View v)
	{
		View hazyView = v.findViewById(R.id.hazy_foreground);
		hazyView.setVisibility(View.VISIBLE);
		hazyView.bringToFront();
	}
	
	/**
	 * Removes the hazy view over all other views, if one exists
	 * @param v The main view
	 */
	private void removeHazyForeground(View v)
	{
		View hazyView = v.findViewById(R.id.hazy_foreground);
		hazyView.setVisibility(View.GONE);
	}
	
	/**
	 * This method is used to reload the events from a button click
	 * @param v
	 */
	public void retryLoad(View v)
	{
		forceEventListUpdate();
	}
	
	/**
	 * Reset the loader so we can do another load operation.
	 * An instance of async task may only be executed once
	 * so we re-instantiate the loader.
	 */
	public void resetLoader()
	{
		loader = new EventLoader();
	}
	
	/**
	 * This method is used to force the timeline to update
	 */
	public void forceEventListUpdate()
	{
		resetLoader();
		loader.execute();
	}
	
	/**
	 * Forces a read from cache
	 * @param v T
	 */
	public ArrayList<Event> forceCacheRead()
	{
		//Try to read data from cache
		 Object o = CacheManager.readObjectFromCacheFile(c , "events");
		 
		if(o != null)
			return (ArrayList<Event>) o;
		else
			return null;
	}
	
	/**
	 * This class is responsible for loading the events. It is necessary because Android
	 * does not allow you to have loading operations on the same thread as the UI.
	 */
	private class EventLoader extends AsyncTask<Void, Double, ArrayList<Event>>
	{
		/**
		 * Flag stating whether or not the load operation was successful
		 */
		private boolean loadSuccessful = false;
		
		
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			if(isRefreshing)
			{
				disableViewClicking();
				showHazyForeground(getView());
			}
		}


		/* (non-Javadoc)
		 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
		 */
		//This method will perform the request to the web service and try to obtain the events
		@Override
		protected ArrayList<Event> doInBackground(Void... params)
		{
			ArrayList<Event> events = new ArrayList<Event>();
			try
			{	
				String path = new ConfigFileReader(c).getSetting("eventsPath");
				URL url = new URL(path); //The path to the webservice 
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				
				//Parse JSON response
				String eventsJSON = reader.readLine();
				// //Log.d("json", eventsJSON);
				JSONArray arr = new JSONArray(eventsJSON);
				events = parseEventJSON(arr);
				events = removeOldEvents(events);
				
				setEvents(events);
				
				//Write data to cache
				CacheManager.writeObjectToCacheFile(getActivity(), events, "events");
				
				//Set success flag to true
				loadSuccessful = true;
				
				
			} catch (MalformedURLException e)
			{
				// TODO Auto-generated catch block
				//e.printStackTrace();
				loadSuccessful = false;
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				//e.printStackTrace();
				loadSuccessful = false;
			} catch (JSONException e)
			{
				// TODO Auto-generated catch block
				//e.printStackTrace();
				loadSuccessful = false;
			}
			
			return events;
		}
		
			
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		//This method will update the UI after the load is finished.
		protected void onPostExecute(ArrayList<Event> events)
		{
			final SwipeRefreshLayout l = (SwipeRefreshLayout) getView().findViewById(R.id.event_list_container);
			
			if(loadSuccessful)
			{	
				//Log.d("load", "successful");
				showEventList(getView(), events);
				
				//Update filter categories
				createFilterDialog(getEventCategories(events));
			}
			else
			{
				//Log.d("load", "unsuccessful");
				if(isRefreshing)
					showRefreshErrorToast();
				else
					showLoadErrorPage(getView());
			}
			
			//We need to do special layout things if the update was from a refresh
			if(isRefreshing)
			{
				isRefreshing = false;
				l.setRefreshing(false);
				removeHazyForeground(getView());
				enableViewClicking();
			}
				
		}
		
		/**
		 * @param obj The JSON object containing the events
		 * @return An arraylist of events
		 * @throws JSONException if parse fails
		 */
		protected ArrayList<Event> parseEventJSON(JSONArray arr) throws JSONException
		{
			ArrayList<Event> events = new ArrayList<Event>();
			for(int i = 0; i < arr.length(); i++)
			{
				String id = arr.getJSONObject(i).getString("id");
				String title = arr.getJSONObject(i).getString("title").trim();
				String location = arr.getJSONObject(i).getString("location").trim();
				String description = arr.getJSONObject(i).getString("description").trim();
				String startDate = arr.getJSONObject(i).getString("startDate").trim();
				String endDate = arr.getJSONObject(i).getString("endDate").trim();
				String lastModified = arr.getJSONObject(i).getString("lastModified").trim();
				String category = arr.getJSONObject(i).getString("category");
				try
				{
					Event e = new Event(id, title, location, startDate, endDate, lastModified, description);
					
					if(!(category.equals("null")))
						e.setCategory(category.trim());
					
					events.add(e);
				} catch (ParseException e)
				{
					//Log.d("Event Parsing", "Failed to parse event" + title);
				}
			}
			
			return events; 
		}
	
		/**
		 * This method is responsible for removing events that have already
		 * occurred from the input array.
		 * @param events The events
		 * @return The events that have yet to pass
		 */
		private ArrayList<Event> removeOldEvents(ArrayList<Event> events)
		{
			ArrayList<Event> newEvents = new ArrayList<Event>(events);
			Iterator<Event> i = events.iterator();
			while(i.hasNext())
			{
				Event e = i.next();
				//If the event has already passed, remove it from the list
				if(e.getEndDate().getTime() < Calendar.getInstance().getTimeInMillis())
					newEvents.remove(e);
			}
			return newEvents;
		}
	}

	/**
	 * @return the events
	 */
	public ArrayList<Event> getEvents()
	{
		return events;
	}

	/**
	 * @param events the events to set
	 */
	public void setEvents(ArrayList<Event> events)
	{
		Collections.sort(events);
		this.events = events;
	}

}
