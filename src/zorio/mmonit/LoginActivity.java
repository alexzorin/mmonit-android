package zorio.mmonit;

import android.os.Bundle;
import android.os.AsyncTask;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class LoginActivity extends Activity {
	
	private Dialog mDialog;
	
	public static String ATTEMPT_AUTOLOGIN = "ATTEMPT_AUTOLOGIN";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		findViewById(R.id.buttonLogin).setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
				doLogin((Button)v);
			}
		});		
		
		Spinner editTimezone = (Spinner) findViewById(R.id.editTimezone);
		editTimezone.setSelection(editTimezone.getCount() / 2);
	
		populateLoginForm();
		if(!finishIfRequired()) {
			attemptAutologin();
		}
	}
	
	private void attemptAutologin() {
		Intent i = getIntent();
		if(i.getBooleanExtra(LoginActivity.ATTEMPT_AUTOLOGIN, false)) {
			SharedPreferences sp = getSharedPreferences("UserInfo", 0);
			String username = sp.getString("Username", null),
				   password = sp.getString("Password", null),
				   endpoint = sp.getString("Endpoint", null);
			if(username != null && password != null && endpoint != null) {
				findViewById(R.id.buttonLogin).callOnClick();
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		finishIfRequired();
	}
	
	private boolean finishIfRequired() {
		if(MainActivity.mc != null) {
			finish();
			return true;
		} else {
			return false;
		}
	}
	
	private void clearModalDialog() {
		if(mDialog != null) {
			mDialog.cancel();
			mDialog = null;
		}
	}
	
	private void populateLoginForm() {
		EditText editEndpoint = (EditText) findViewById(R.id.editEndpoint),
				editUsername = (EditText) findViewById(R.id.editUsername),
				editPassword = (EditText) findViewById(R.id.editPassword);
		Spinner editTimezone = (Spinner) findViewById(R.id.editTimezone);

		SharedPreferences sp = getSharedPreferences("UserInfo", 0);
		
		if(sp.contains("Username")) {
			editUsername.setText(sp.getString("Username", ""));
		}
		if(sp.contains("Password")) {
			editPassword.setText(sp.getString("Password", ""));
		}
		if(sp.contains("Endpoint")) {
			editEndpoint.setText(sp.getString("Endpoint", ""));
		}
		if(sp.contains("Timezone")) {
			editTimezone.setSelection(sp.getInt("Timezone", 0));
		}
	}
	
	private void doLogin(Button loginBtn) {
		EditText editEndpoint = (EditText) findViewById(R.id.editEndpoint),
				editUsername = (EditText) findViewById(R.id.editUsername),
				editPassword = (EditText) findViewById(R.id.editPassword);
		Spinner editTimezone = (Spinner) findViewById(R.id.editTimezone);
		
		String endpoint = editEndpoint.getText().toString(),
			   username = editUsername.getText().toString(),
			   password = editPassword.getText().toString();
				
		if(!URLUtil.isValidUrl(endpoint)) {
			editEndpoint.setError("Invalid server URL");
		}
		else if(username.length() == 0) {
			editUsername.setError("Please specify the username");
		}
		else if(password.length() == 0) {
			editPassword.setError("Please specify the password");
		}
		else {
			// Reset error state
			editEndpoint.setError(null);
			editUsername.setError(null);
			editPassword.setError(null);
			
			// Save settings if requested
			CheckBox cbRemember = (CheckBox)findViewById(R.id.checkRememberLogin);
			if(cbRemember.isChecked()) {
				SharedPreferences sp = getSharedPreferences("UserInfo", 0);
				SharedPreferences.Editor e = sp.edit();
				
				e.putString("Username", username);
				e.putString("Password", password);
				e.putString("Endpoint", endpoint);
				e.putInt("Timezone", editTimezone.getSelectedItemPosition());
				
				e.commit();
			}
			
			// Try login
			mDialog = ProgressDialog.show(LoginActivity.this, "Loading", "Trying to login ...");
			((ProgressDialog)mDialog).setIndeterminate(true);
			mDialog.show();
			
			new LoginTask().execute(endpoint, username, password);
		}
	}
	
	private void onLogin(MMonitClient mc) {
		clearModalDialog();
		if(mc == null) {
			Toast toast = Toast.makeText(this, "Unable to login - check details", Toast.LENGTH_LONG);
			toast.show();
		} else {
			MainActivity.mc = mc;
			// MainActivity should always be adjacent in the activity stack
			finish();
		}
	}
	
	private class LoginTask extends AsyncTask<String, Void, MMonitClient> {

		@Override
		protected MMonitClient doInBackground(String... args) {
			String endpoint = args[0],
				   username = args[1],
				   password = args[2];
			try {
				MMonitClient mc = new MMonitClient(endpoint, username, password);
				return mc;
			} catch(Throwable t) {
				t.printStackTrace();
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(MMonitClient mc) {
			LoginActivity.this.onLogin(mc);
		}
			
	}
	
}
