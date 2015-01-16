package com.uf.dancemarathon;

import java.util.Locale;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBarActivity;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.uf.dancemarathon.R;

public class SwipeActivity extends ActionBarActivity
{

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a 
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
	DrawerLayout mDrawerLayout;
	ListView mDrawerList;
	KinteraUser user;
	private String[] mOtherOptions;
	boolean trackEnabled = false;
    static final int GET_USER_REQUEST = 1;
	
	//These methods allow us to maintain the state of the user//
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putParcelable("user", user);
	}
	
	public void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		//Get user from savedInstanceState
		user = savedInstanceState.getParcelable("user");
	}
	//
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_swipe);
		
		//Hide Action Bar
		getSupportActionBar().hide();
		
		setUpPagers();
		setUpNavDrawer();
		user = (KinteraUser) CacheManager.readObjectFromCacheFile(this, "user");
		
		//Set on page change listener to implement google analytics
		mViewPager.setOnPageChangeListener(new OnPageChangeListener(){
			int currPos = 0;
			@Override
			public void onPageScrollStateChanged(int arg0)
			{
				//If page has settled
				if(arg0 == ViewPager.SCROLL_STATE_IDLE)
				{
					int statePos = mViewPager.getCurrentItem();
					//If the page settled on a different page
					if(currPos != statePos)
					{
						//Send tracking event and update current position
						sendTrackingView(statePos);
						currPos = statePos;
					}
				}
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2)
			{
				// TODO Auto-generated method stub
			
			}

			@Override
			public void onPageSelected(int pos)
			{
				//Open the navigation drawer
				if(pos == 0)
					mDrawerLayout.openDrawer(Gravity.START);
			}
			
			private void sendTrackingView(int page)
			{
				if(trackEnabled)
				{
					@SuppressWarnings("unused")
					String LogString = "";
					String sendString = "";
					switch(page)
					{
					case 0: LogString="NavDrawer"; sendString="Navigation Drawer";break;
					case 1: LogString="HomeFragment"; sendString="Home Swipe Screen";break;
					case 2: LogString="TimelineFragment"; sendString="Timeline Swipe Screen";break;
					case 3: LogString="MTKFragment"; sendString="MTK Swipe Screen";break;
					}
					//Log.d("Tracking", LogString);
					TrackerManager.sendScreenView((MyApplication)getApplication(), sendString);
				}
			}
			
		});

	}
	
	protected void onStart()
	{
		super.onStart();
		
		//Register google analytics page hit
		int canTrack = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplication());
		if(canTrack == ConnectionResult.SUCCESS)
		{
			//Log.d("Tracking", "SwipeActivity");
			TrackerManager.sendScreenView((MyApplication) getApplication(), "Main Screen");
			trackEnabled = true;
		}
	}

	protected void onResume()
	{
		super.onResume();
		
		//Don't show notifications if user is in-app
		stopService(new Intent(this, NotificationService.class));
		
		//If this activity was started from the service, go to timeline
		if(this.getIntent().hasExtra("start_source"))
		{
			if(this.getIntent().getStringExtra("start_source").equals("Service"))
				mViewPager.setCurrentItem(2);
		}
	}
	
	protected void onStop()
	{
		super.onStop();
		//Show notifications if user exits out of app
		//Could not use onDestroy because it is not always called
		startService(new Intent(this, NotificationService.class));
	}
	
	/**
	 * This method handles the initializations for all the 
	 * {@link ViewPager}/{@link PagerTabStrip} stuff
	 */
	private void setUpPagers()
	{
		// Create the adapter that will return a fragment for each of the 
		// primary sections of the activity.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		
		// Set the ViewPager to Home
		mViewPager.setCurrentItem(1, false);

		// Change PagerTabStrip spacing
		PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.pager_title_strip);
		tabStrip.setTextSpacing(0);
		tabStrip.setTextSize(TypedValue.COMPLEX_UNIT_PT, 7);
	}
	
	/**
	 * This method handles the initializations for the navigation drawer
	 */
	private void setUpNavDrawer()
	{
		 mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
	     mDrawerList = (ListView) findViewById(R.id.left_drawer);
	     mOtherOptions = getResources().getStringArray(R.array.navList);
	     
	     // Set the adapter for the list view
	     mDrawerList.setAdapter(new ArrayAdapter<String>(this,
	           R.layout.nav_drawer_item, R.id.nav_item, mOtherOptions));
	     // Set the list's click listener
	     mDrawerList.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id)
			{
				// TODO Auto-generated method stub
				switch(position)
				{
				case 0: openFAQActivity();break;
				case 1: openSocialMediaActivity();break;
				case 2: openFundraisingActivity();break;
				case 3: openAboutDMActivity();break;
				case 4: openContactUsActivity();break;
				}
				
			}
	    	 
	     });
	     
	     mDrawerLayout.setDrawerListener(new DrawerListener(){

			@Override
			public void onDrawerClosed(View arg0)
			{
			}

			@Override
			public void onDrawerOpened(View arg0)
			{
				// TODO Auto-generated method stub
				mViewPager.setCurrentItem(1, false);
			}

			@Override
			public void onDrawerSlide(View arg0, float arg1)
			{
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onDrawerStateChanged(int arg0)
			{
				// TODO Auto-generated method stub
				
			}
	    	 
	     });
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return true;
	}

	/**
	 * This method handles opening of the my fundraising progress activity.
	 * If a user has been defined so far, then we open the user activity.
	 * Else, the //Login activity is opened
	 */
	private void openFundraisingActivity()
	{
		if(user instanceof KinteraUser)
		{
			Intent intent = new Intent(this, UserActivity.class);
			Bundle b = new Bundle();
			b.putParcelable("user", user);
			intent.putExtras(b);
			startActivityForResult(intent, GET_USER_REQUEST);
		}
		else
		{
			Intent intent = new Intent(this, LoginActivity.class);
			startActivityForResult(intent, GET_USER_REQUEST);
		}
	}
	
	/**
	 * This method opens the FAQ activity
	 */
	private void openContactUsActivity()
	{
		Intent intent = new Intent(this, ContactUsActivity.class);
		startActivity(intent);
	}
	
	/**
	 * This method opens the FAQ activity
	 */
	private void openFAQActivity()
	{
		Intent intent = new Intent(this, FAQActivity.class);
		startActivity(intent);
	}
	
	private void openAboutDMActivity()
	{
		Intent intent = new Intent(this, AboutActivity.class);
		startActivity(intent);
	}
	
	private void openSocialMediaActivity()
	{
		Intent intent = new Intent(this, SocialMedia.class);
		startActivity(intent);
	}
	
	/**
	 * This method handles opening the sponsor activity
	 */
	@SuppressWarnings("unused")
	private void openSponsorActivity()
	{
		Intent intent = new Intent(this, SponsorActivity.class);
		startActivity(intent);
	}
	

	/* (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int, android.content.Intent)
	 * The current implementation allows the user data to be passed back from the //Login activity
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(requestCode == GET_USER_REQUEST)
		{
			if(resultCode == RESULT_OK)
			{
				user = data.getExtras().getParcelable("user");
			}
			else if(resultCode == RESULT_CANCELED)
			{
				user = null;
			}
		}
	}
	
	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentStatePagerAdapter
	{

		public SectionsPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public Fragment getItem(int position)
		{
			// getItem is called to instantiate the fragment for the given page.
			// Return the fragment that corresponds to the position
			switch (position)
			{
			case 0:return new Fragment(); //Return blank fragment because this will be covered by nav drawer
			case 1:return HomeFragment.newInstance();
			case 2:return TimelineFragment.newInstance(SwipeActivity.this);
			case 3:return MtkFragment.newInstance();
			}
			return null;
		}

		@Override
		public int getCount()
		{
			// Get total number of pages
			return 4;
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			Drawable drawerIcon = getResources().getDrawable(R.drawable.ic_drawer);
			SpannableStringBuilder sb = new SpannableStringBuilder("  "); 

		    drawerIcon.setBounds(0, 0, drawerIcon.getIntrinsicWidth(), drawerIcon.getIntrinsicHeight()); 
		    ImageSpan span = new ImageSpan(drawerIcon, ImageSpan.ALIGN_BASELINE); 
		    sb.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); 
		   
		    
			Locale l = Locale.getDefault();
			switch (position)
			{
			case 0:return sb;
			case 1:return getString(R.string.title_section1).toUpperCase(l);
			case 2:return getString(R.string.title_section2).toUpperCase(l);
			case 3:return getString(R.string.title_section3).toUpperCase(l);
			}
			return null;
		}
	}
}
