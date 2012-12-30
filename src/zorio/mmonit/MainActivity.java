package zorio.mmonit;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
	
	public static MMonitClient mc;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		loginIfRequired();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		loginIfRequired();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menu_logout:
				mc = null;
				loginIfRequired();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private void loginIfRequired() {
		if(mc == null) {
			Intent i = new Intent().setClass(this, LoginActivity.class);
			startActivity(i);
		}
	}

}
