package zorio.mmonit;

import java.util.List;

import zorio.mmonit.model.Event;

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
		
    private static final String[] GROUP_NAMES = { "Events", "Hosts" };
    
    private static final int EVENTS_IDX = 0;
    
    private String[][] children = {
            { "Arnold", "Barry", "Chuck", "David" },
            { "Ace", "Bandit", "Cha-Cha", "Deuce" },
    };

	public MainActivityListAdapter(MainActivity ctx, List<Event> events) {
		this.ctx = ctx;
		this.events = events;
	}
	    
    public Object getChild(int groupPosition, int childPosition) {
    	switch(groupPosition) {
    		case EVENTS_IDX:
    			return events.get(childPosition);
    		default:
    	        return "Barry " + groupPosition + "," + childPosition;
    	}
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public int getChildrenCount(int groupPosition) {
        switch(groupPosition) {
        	case EVENTS_IDX:
        		return events.size();
        	default:
        		return children[groupPosition].length;
        } 	
    }
    
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {
    	
    	switch(groupPosition) {
    		case EVENTS_IDX:
    			/* TODO: npe on convertview sometimes? */
    			View v = /*convertView != null ? convertView :*/ LayoutInflater.from(ctx).inflate(R.layout.item_event, null);
    			Event e = (Event) getChild(groupPosition, childPosition);
    			
    			if(e == null)
    				System.out.println(childPosition + "," + groupPosition);
    			
    			TextView dateView = (TextView)v.findViewById(R.id.eventDate); 			
    			dateView.setText(DateUtils.getRelativeDateTimeString(ctx, 
    					e.getDate().getTime(), 
    					DateUtils.MINUTE_IN_MILLIS, 
    					DateUtils.WEEK_IN_MILLIS, 
    					DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_12HOUR));
    			
    			boolean isNew = false;
    			if(e.getDate().getTime() >= ctx.getNewEventCutoff()) {
    				isNew = true;
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
    		default:
    	        return new TextView(ctx);
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
    		default:
    			View v = LayoutInflater.from(ctx).inflate(R.layout.group_events, null);
    			TextView t = (TextView) v.findViewById(R.id.eventsGroupNew);
    			if(ctx.getNewEventCount() > 0) {
    				t.setText(ctx.getNewEventCount() + " new events");
    				t.setVisibility(View.VISIBLE);
    			} else {
    				t.setVisibility(View.INVISIBLE);
    			}
    			return v;
    	}
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public boolean hasStableIds() {
        return true;
    }
}
