package tk.hugo4715.golemamc.jump.util;

import java.util.concurrent.TimeUnit;

public class TimeUtil {
	public static String intervalToHumanReadableTime(long intervalMs) {

	    if(intervalMs <= 0) {
	        return "0";
	    } else {


	        long days = TimeUnit.MILLISECONDS.toDays(intervalMs);
	        intervalMs -= TimeUnit.DAYS.toMillis(days);
	        long hours = TimeUnit.MILLISECONDS.toHours(intervalMs);
	        intervalMs -= TimeUnit.HOURS.toMillis(hours);
	        long minutes = TimeUnit.MILLISECONDS.toMinutes(intervalMs);
	        intervalMs -= TimeUnit.MINUTES.toMillis(minutes);
	        long seconds = TimeUnit.MILLISECONDS.toSeconds(intervalMs);
	        intervalMs -= TimeUnit.SECONDS.toMillis(seconds);
	        long millis = intervalMs;
	        
	        
	        StringBuilder sb = new StringBuilder(12);

	        if (days >= 1) {
	            sb.append(days).append(" jour").append(pluralize(days)).append(", ");
	        }

	        if (hours >= 1) {
	            sb.append(hours).append(" heure").append(pluralize(hours)).append(", ");
	        }

	        if (minutes >= 1) {
	            sb.append(minutes).append(" minute").append(pluralize(minutes)).append(", ");
	        }
	        
	        if(seconds >= 1){
	        	sb.append(seconds).append(" seconde").append(pluralize(seconds)).append(", ");
	        }
	        
	        if(millis >= 1){
	        	sb.append(millis).append(" milliseconde").append(pluralize(millis));
	        }
	        

	        return(sb.toString());          
	    }

	}

	public static String pluralize(long val) {
	    return (Math.round(val) > 1 ? "s" : "");
	}
}
