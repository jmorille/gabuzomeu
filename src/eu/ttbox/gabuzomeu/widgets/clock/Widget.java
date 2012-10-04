package eu.ttbox.gabuzomeu.widgets.clock;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import eu.ttbox.gabuzomeu.R;

@TargetApi(3)
public class Widget extends AppWidgetProvider {
	public final static String CLOCK_START = "TypographicClock.ClockStart";
	public final static String CLOCK_STOP = "TypographicClock.ClockStop";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		UpdateTime(context);
		Intent i = new Intent();
		i.setClass(context, ClockService.class);
		i.setAction(CLOCK_START);
		context.startService(i);
	}

	@Override
	public void onDisabled(Context context) {
		Intent i = new Intent();
		i.setClass(context, ClockService.class);
		i.setAction(CLOCK_STOP);
		context.startService(i);
	}

	static public void UpdateTime(Context context) {
		ComponentName thisWidget = new ComponentName(context, Widget.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);

		String dayNames[] = new DateFormatSymbols().getWeekdays();
		String monthNames[] = new DateFormatSymbols().getMonths();

		Calendar c = Calendar.getInstance(TimeZone.getDefault());
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.widget);

		String text;
		Date time = c.getTime();
		String hour = toGaBuZoMeu(c.get(Calendar.HOUR_OF_DAY));
		String minute = toGaBuZoMeu(c.get(Calendar.MINUTE));
		// text = DateFormat.getTimeInstance(DateFormat.SHORT).format(time);
		// if (c.get(Calendar.HOUR_OF_DAY) < 10)
		// text = "0" + text;
		views.setTextViewText(R.id.time, hour + ":" + minute);
		views.setTextViewText(R.id.weekday,
				dayNames[c.get(Calendar.DAY_OF_WEEK)].toUpperCase());
		views.setTextViewText(R.id.day,
				Integer.toString(c.get(Calendar.DAY_OF_MONTH)));
		views.setTextViewText(R.id.month,
				monthNames[c.get(Calendar.MONTH)].toUpperCase());
		views.setTextViewText(R.id.year, Integer.toString(c.get(Calendar.YEAR)));

		manager.updateAppWidget(thisWidget, views);
	}

	static private String toGaBuZoMeu(int n) {

		return Integer.toString(n, 4).replaceAll("0", "GA")
				.replaceAll("1", "BU").replaceAll("2", "ZO")
				.replaceAll("3", "MEU");

	}

	static private int toBase10(String s) {
		String ss = s.replaceAll("GA", "0").replaceAll("BU", "1")
				.replaceAll("ZO", "2").replaceAll("MEU", "3");
		return Integer.valueOf(ss, 4);

	}
}
