package zorio.mmonit;

import java.util.List;

import zorio.mmonit.model.Event;
import zorio.mmonit.model.Host;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class MainActivityListAdapter extends BaseExpandableListAdapter {

	private MainActivity ctx;
	
	private List<Event> events;
	private List<Host> hosts;
		
    private static final String[] GROUP_NAMES = { "Events", "Hosts" };
    
    private static final int EVENTS_IDX = 1;
    private static final int HOSTS_IDX = 0;
    
	public MainActivityListAdapter(MainActivity ctx, List<Event> events, List<Host> hosts) {
		this.ctx = ctx;
		this.events = events;
		this.hosts = hosts;
	}
	    
    public Object getChild(int groupPosition, int childPosition) {
    	switch(groupPosition) {
    		case EVENTS_IDX:
    			return events.get(childPosition);
    		case HOSTS_IDX:
    	        return hosts.get(childPosition);
    	    default:
    	    	return null;
    	}
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public int getChildrenCount(int groupPosition) {
        switch(groupPosition) {
        	case EVENTS_IDX:
        		return events.size();
        	case HOSTS_IDX:
        		return hosts.size();
        	default:
        		return 0;
        } 	
    }
    
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {
    	
    	switch(groupPosition) {
    		case EVENTS_IDX:
    			/* TODO: npe on convertview sometimes? */
    			View v = /*convertView != null ? convertView :*/ LayoutInflater.from(ctx).inflate(R.layout.item_event, null);
    			Event e = (Event) getChild(groupPosition, childPosition);
    			
    			boolean isNew = e.getDate().getTime() >= ctx.getNewEventCutoff();
    			
    			TextView dateView = (TextView)v.findViewById(R.id.eventDate); 			
    			dateView.setText(DateUtils.getRelativeDateTimeString(ctx, 
    					e.getDate().getTime(), 
    					DateUtils.MINUTE_IN_MILLIS, 
    					DateUtils.WEEK_IN_MILLIS, 
    					DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_12HOUR));    			
    			if(isNew) {
    				dateView.setTypeface(null, Typeface.BOLD);
    			}

    			TextView msgView = (TextView)v.findViewById(R.id.eventMsg);
    			msgView.setText(e.getEvent());
    			
    			TextView hostView = (TextView)v.findViewById(R.id.eventHost);
    			hostView.setText("from " + e.getHost());    			
    			
    			TextView serviceView = (TextView)v.findViewById(R.id.eventService);
    			serviceView.setText(e.getService());
    			
    			/* etypes:
    			 * 	1 = red
    			 * 	2,3 = orange
    			 * 	4,5 = grey
    			 * 	otherwise green
    			 */		
    			int color;
    			switch(e.getEtype()) {
    				case 1:
    					color = Color.parseColor(isNew ? "#fde0d6" : "#feece6");
    					break;
    				case 2:
    				case 3:
    					color = Color.parseColor(isNew ? "#fdefda" : "#fef6ea");
    					break;
    				case 4:
    				case 5:
    					color = Color.parseColor(isNew ? "#ebe9e4" : "#f2f1ee");    					
    					break;
    				default:
    					color = Color.parseColor(isNew ? "#d9f9d4" : "#e6fbe3");    					
    					break;
    			}			
    			v.setBackgroundColor(color);  			
    			return v;
    		case HOSTS_IDX:
    			v = LayoutInflater.from(ctx).inflate(R.layout.item_host, null);
    			Host h = (Host) getChild(groupPosition, childPosition);
    			    			
    			switch(h.getLed()) {
    				case 0:
    					color = Color.parseColor("#fde0d6");
    					break;
    				case 1:
    					color =  Color.parseColor("#fdefda");
    					break;
    				case 2:
    					color =  Color.parseColor("#d9f9d4");
    					break;
    				default:
    					color = Color.parseColor("#ebe9e4");
    			}
    			v.setBackgroundColor(color);
    			return v;
    		default:
    			return null;
    	}
    }

    public Object getGroup(int groupPosition) {
        return GROUP_NAMES[groupPosition];
    }

    public int getGroupCount() {
        return GROUP_NAMES.length;
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
            ViewGroup parent) {
    	switch(groupPosition) {
    		case EVENTS_IDX:
    			View v = LayoutInflater.from(ctx).inflate(R.layout.group_events, null);
    			TextView t = (TextView) v.findViewById(R.id.eventsGroupNew);
    			t.setText(ctx.getNewEventCount() + " new events");
    			t.setVisibility(View.VISIBLE);
    			return v;
    		case HOSTS_IDX:
    			v = LayoutInflater.from(ctx).inflate(R.layout.group_hosts, null);
    			return v;
    		default:
    			return new View(ctx);
    	}
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public boolean hasStableIds() {
        return true;
    }
}
