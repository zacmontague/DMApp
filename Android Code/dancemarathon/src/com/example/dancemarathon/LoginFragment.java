package com.example.dancemarathon;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass.
 * 
 */
public class LoginFragment extends Fragment
{

	public LoginFragment()
	{
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.fragment_login, container, false);
		v.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v)
			{
				//Hide keyboard from edit text views if user clicks outside of the keyboard
				InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
					    Activity.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
			}
			
		});
		
		v.findViewById(R.id.login_button).setOnClickListener(new OnClickListener(){

			@Override
			//Not working right now
			public void onClick(View v)
			{
				String errorMessage = "";
				if(formValid())
				{
					Intent intent = new Intent(getActivity(), UserActivity.class);
					startActivity(intent);
				}
				else
				{
					errorMessage="Fields cannot be blank!";
					Toast toast = Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 20);
					toast.show();
				}
			}
			
			private Boolean formValid()
			{
				int uSize = ((EditText) getView().findViewById(R.id.username_field)).getText().length();
				int pSize = ((EditText) getView().findViewById(R.id.password_field)).getText().length();
				if(uSize > 0 && pSize > 0)
					return true;
				else
					return false;
			}
			
		});
		
	
		return v;
	}
	
	public static LoginFragment newInstance()
	{
		LoginFragment lf = new LoginFragment();
		return lf;
	}

}
