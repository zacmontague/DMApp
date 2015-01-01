package com.example.dancemarathon;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


public class LoginActivity extends ActionBarActivity
{
	public static int IS_USER_STILL_LOGGED_IN = 5;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		//Hide Action Bar
		getSupportActionBar().hide();
				
		FragmentManager manager = getSupportFragmentManager();
		if(savedInstanceState != null)
			return;
		
		manager.beginTransaction().add(R.id.kintera_container, LoginFragment.newInstance()).commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings)
		{
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(requestCode == LoginFragment.IS_USER_STILL_LOGGED_IN)
		{
			if(resultCode == Activity.RESULT_CANCELED)
			{
				setResult(Activity.RESULT_CANCELED, new Intent());
			}
			else if(resultCode == Activity.RESULT_OK)
			{
				setResult(Activity.RESULT_OK, data);
			}
			
			this.finish();
		}
	}
}
