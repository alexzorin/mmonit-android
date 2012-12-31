package zorio.mmonit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import zorio.mmonit.model.Event;
import zorio.mmonit.model.Host;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;

public class MainActivity extends Activity {
	
	public static MMonitClient mc;
	
	private List<Event> events;
	private List<Host> hosts;
	private int newEventCount;
	
	private MenuItem refreshMenuItem;
	
	private int asyncTaskCount;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
				
		events = new ArrayList<Event>();
		hosts = new ArrayList<Host>();
		asyncTaskCount = 0;
		
		if(!loginIfRequired(true)) {
			refreshData();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(!loginIfRequired(true)) {
			refreshData();
		}
	}
	
	private void refreshData() {
		ExpandableListView list = (ExpandableListView) findViewById(R.id.viewCategories);
		if(list.getAdapter() == null) {
			list.setAdapter(new MainActivityListAdapter(this, events, hosts));
		}
		// Cut any current 'new events' off so they aren't marked next time
		if(getNewEventCount() > 0) {
			updateNewEventCutoff();
		}
		
		new GetEventsTask().execute();
		asyncTaskCount++;
		new GetHostsTask().execute();
		asyncTaskCount++;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);		
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		refreshMenuItem = menu.findItem(R.id.menu_refresh);
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menu_logout:
				mc = null;
				loginIfRequired(false);
				return true;
			case R.id.menu_refresh:
				if(item.isEnabled()) {
					// Spinny loader
					refreshMenuItem = item;					
					ImageView newIcon = (ImageView)LayoutInflater.from(this).inflate(R.layout.icon_refresh, null);
					Animation anim = AnimationUtils.loadAnimation(this, R.anim.anim_rotate);
					anim.setRepeatMode(Animation.RESTART);
					anim.setRepeatCount(Animation.INFINITE);
					newIcon.startAnimation(anim);
					refreshMenuItem.setActionView(newIcon);
					refreshMenuItem.setEnabled(false);					
					if(asyncTaskCount == 0) {
						refreshData();
					}
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private boolean loginIfRequired(boolean autologin) {
		if(mc == null) {
			Intent i = new Intent().setClass(this, LoginActivity.class);
			i.putExtra(LoginActivity.ATTEMPT_AUTOLOGIN, autologin);
			startActivity(i);
			return true;
		} else {
			return false;
		}
	}
	
	private void clearAnimsAndModals() {
		if(--asyncTaskCount == 0) {
			if(refreshMenuItem != null) {
				if(refreshMenuItem.getActionView() != null) {
					refreshMenuItem.getActionView().clearAnimation();
					refreshMenuItem.setActionView(null);
				}
				refreshMenuItem.setEnabled(true);
				refreshMenuItem = null;
			}
		}
	}
	
	private void onHostsLoaded(List<Host> newHosts) {
		clearAnimsAndModals();
		if(newHosts == null) {
			
		} else {
			hosts.clear();
			hosts.addAll(newHosts);
			((BaseAdapter)((ExpandableListView)findViewById(R.id.viewCategories)).getAdapter()).notifyDataSetChanged();			
		}
	}
	
	private void onEventsLoaded(List<Event> newEvents) {
		clearAnimsAndModals();
		if(newEvents == null) {			
		} else {
			events.clear();
			events.addAll(newEvents);
			fixEventDateTZs();
			newEventCount = countNewEvents();
			if(newEventCount > 0) {
				try {
					((Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(250);
				} catch(Throwable t) {
					t.printStackTrace();
				}
			}
			((BaseAdapter)((ExpandableListView)findViewById(R.id.viewCategories)).getAdapter()).notifyDataSetChanged();
		}
	}
	
	private void fixEventDateTZs() {
		SharedPreferences sp = getSharedPreferences("UserInfo", 0);
		String[] tzs = getResources().getStringArray(R.array.timezones);		
		
		int tzSelection = sp.getInt("Timezone", tzs.length / 2);
		int tzOffset = Integer.parseInt(tzs[tzSelection]) * -1;
		long currentTzOffset = 0;
				
		for(Event e : events) {
			Date d = e.getDate();
			currentTzOffset = TimeZone.getDefault().getOffset(d.getTime());
			e.setDate(new Date(d.getTime() + TimeUnit.MILLISECONDS.convert(tzOffset, TimeUnit.HOURS) + currentTzOffset));
		}
	}
	
	protected int countNewEvents() {
		SharedPreferences sp = getSharedPreferences("UserInfo", 0);
		
		int newCount = 0;
		
		long latestEventStart = Long.MAX_VALUE, latestEventEnd = 0;
		long oldLatestEventEnd = sp.getLong("LatestEventEnd", 0);
		
		for(Event e : events) {
			long eventTime = e.getDate().getTime();
			// Record end time for this group of events
			if(eventTime > latestEventEnd) {
				latestEventEnd = eventTime;
			}
			// Count new events
			if(eventTime > oldLatestEventEnd) {			
				// Record start time only for new events
				if(eventTime < latestEventStart) {
					latestEventStart = eventTime;
				}
				newCount++;
			}
		}
		SharedPreferences.Editor editor = sp.edit();
		editor.putLong("LatestEventStart", latestEventStart);
		editor.putLong("LatestEventEnd", latestEventEnd);
		editor.commit();
		
		return newCount;	
	}
	
	public int getNewEventCount() {
		return newEventCount;
	}
	
	public long getNewEventCutoff() {
		SharedPreferences sp = getSharedPreferences("UserInfo", 0);
		return sp.getLong("LatestEventStart", 0);
	}
	
	public void updateNewEventCutoff() {
		SharedPreferences sp = getSharedPreferences("UserInfo", 0);
		SharedPreferences.Editor e = sp.edit();
		e.putLong("LatestEventStart", sp.getLong("LatestEventEnd", 0));
		e.commit();
	}
	
	private class GetEventsTask extends AsyncTask<Void, Void, List<Event>> {

		@Override
		protected List<Event> doInBackground(Void... params) {
			try {				
				List<Event> events = mc.getEvents(0, 5, "date", true);
				return events;
			} catch(Throwable t) {
				t.printStackTrace();
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(List<Event> events) {
			MainActivity.this.onEventsLoaded(events);
		}
		
	}
	
	private class GetHostsTask extends AsyncTask<Void, Void, List<Host>> {

		@Override
		protected List<Host> doInBackground(Void... params) {
			try {
				return mc.getHostStatuses();
			} catch(Throwable t) {
				t.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(List<Host> hosts) {
			MainActivity.this.onHostsLoaded(hosts);
		}
		
	}

}
