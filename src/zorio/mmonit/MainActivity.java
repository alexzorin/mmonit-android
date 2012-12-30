package zorio.mmonit;

import java.util.ArrayList;
import java.util.List;

import zorio.mmonit.model.Event;
import zorio.mmonit.model.Host;

import android.os.AsyncTask;
import android.os.Bundle;
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
	
	private MenuItem animatingItem;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
				
		events = new ArrayList<Event>();
		hosts = new ArrayList<Host>();

		if(!loginIfRequired()) {
			refreshData();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(!loginIfRequired()) {
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
			setNewEventCutoff(getNewEventCutoff() + 1);
		}
		new GetEventsTask().execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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
			case R.id.menu_refresh:
				// Start async task to refresh data
				refreshData();			
				// Spinny loader and disable subsequent refreshes
				ImageView newIcon = (ImageView)LayoutInflater.from(this).inflate(R.layout.icon_refresh, null);
				
				Animation anim = AnimationUtils.loadAnimation(this, R.anim.anim_rotate);
				anim.setRepeatMode(Animation.RESTART);
				anim.setRepeatCount(Animation.INFINITE);
				newIcon.startAnimation(anim);
				
				item.setActionView(newIcon);
				item.setEnabled(false);		
				animatingItem = item;
				
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private boolean loginIfRequired() {
		if(mc == null) {
			Intent i = new Intent().setClass(this, LoginActivity.class);
			startActivity(i);
			return true;
		} else {
			return false;
		}
	}
	
	private void clearAnimsAndModals() {
		if(animatingItem != null) {
			animatingItem.getActionView().clearAnimation();
			animatingItem.setActionView(null);
			animatingItem.setEnabled(true);
			animatingItem = null;
		}
	}
	
	private void onEventsLoaded(List<Event> newEvents) {
		clearAnimsAndModals();
		if(newEvents == null) {
			
		} else {
			events.clear();
			events.addAll(newEvents);
			newEventCount = countNewEvents();
			((BaseAdapter)((ExpandableListView)findViewById(R.id.viewCategories)).getAdapter()).notifyDataSetChanged();			
		}
	}
	
	protected int countNewEvents() {
		SharedPreferences sp = getSharedPreferences("UserInfo", 0);
	
		int newCount = 0;
		long latest = 0;
		long latestOrig = sp.getLong("LatestEvent", 0);
				
		for(Event e : events) {
			long time = e.getDate().getTime();
			if(time > latestOrig) {
				newCount++;
				if(time > latest) {
					latest = time;
				}
			}
		}
		
		SharedPreferences.Editor editor = sp.edit();
		editor.putLong("LatestEvent", latest == 0 ? latestOrig : latest);
		editor.commit();
		
		return newCount;
	}
	
	public int getNewEventCount() {
		return newEventCount;
	}
	
	public long getNewEventCutoff() {
		SharedPreferences sp = getSharedPreferences("UserInfo", 0);
		return sp.getLong("LatestEvent", 0);
	}
	
	public void setNewEventCutoff(long time) {
		SharedPreferences sp = getSharedPreferences("UserInfo", 0);
		SharedPreferences.Editor e = sp.edit();
		e.putLong("LatestEvent", time);
		e.commit();
	}
	
	private class GetEventsTask extends AsyncTask<Void, Void, List<Event>> {

		@Override
		protected List<Event> doInBackground(Void... params) {
			try {
				List<Event> events = mc.getEvents(0, 10, "date", true);
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

}
