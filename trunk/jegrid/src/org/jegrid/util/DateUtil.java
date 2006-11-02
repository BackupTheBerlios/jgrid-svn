package org.jegrid.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;

/**
 * Miscelaneous date functions.
 * <br>User: jdavis
 * Date: Nov 2, 2006
 * Time: 4:13:52 PM
 */
public class DateUtil
{
    public static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss,SSS";
    private static DateFormat timeFormat;

    static
    {
        timeFormat = new SimpleDateFormat(TIME_FORMAT);
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static String formatTime(long time)
    {
        synchronized(timeFormat)
        {
            return timeFormat.format(new Date(time));
        }
    }
}
