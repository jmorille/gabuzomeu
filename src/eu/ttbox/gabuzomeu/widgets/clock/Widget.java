package eu.ttbox.gabuzomeu.widgets.clock;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.TimeZone;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import eu.ttbox.gabuzomeu.R;
import eu.ttbox.gabuzomeu.service.GabuzomeuConverter;

public class Widget extends AppWidgetProvider {
	public final static String CLOCK_START = "ClockStart";
	public final static String CLOCK_STOP = "ClockStop";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		updateTime(context);
		Intent intent = new Intent();
		intent.setClass(context, ClockService.class);
		intent.setAction(CLOCK_START);
		context.startService(intent);
	}

	@Override
	public void onDisabled(Context context) {
		Intent intent = new Intent();
		intent.setClass(context, ClockService.class);
		intent.setAction(CLOCK_STOP);
		context.startService(intent);
	}

	static public void updateTime(Context context) {
		ComponentName thisWidget = new ComponentName(context, Widget.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);

		String dayNames[] = new DateFormatSymbols().getWeekdays();
		String monthNames[] = new DateFormatSymbols().getMonths();

		Calendar c = Calendar.getInstance(TimeZone.getDefault());
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.widget);

		// Converter
		GabuzomeuConverter converter = new GabuzomeuConverter(context);
		// TODO http://stackoverflow.com/questions/4318572/how-to-use-a-custom-typeface-in-a-widget ?
//	TODO	Typeface font = GabuzomeuConverter.getSymbolFont(context);
		// Do Converter
		String hourMinute = String.format("%1$tH:%1$tM", System.currentTimeMillis());
		StringBuilder shadokDigit= new StringBuilder(); // For Symbole
        StringBuilder shadokDigitName = new StringBuilder();
		converter.encodeEquationToShadokCode(hourMinute, shadokDigit, shadokDigitName);
		
//		String text;
//		Date time = c.getTime();
//		String hour = toGaBuZoMeu(c.get(Calendar.HOUR_OF_DAY));
//		String minute = toGaBuZoMeu(c.get(Calendar.MINUTE));
		// text = DateFormat.getTimeInstance(DateFormat.SHORT).format(time);
		// if (c.get(Calendar.HOUR_OF_DAY) < 10)
		// text = "0" + text;
		views.setTextViewText(R.id.time, shadokDigitName.toString());
		
		views.setTextViewText(R.id.weekday,
				dayNames[c.get(Calendar.DAY_OF_WEEK)].toUpperCase());
		views.setTextViewText(R.id.day,
				Integer.toString(c.get(Calendar.DAY_OF_MONTH)));
		views.setTextViewText(R.id.month,
				monthNames[c.get(Calendar.MONTH)].toUpperCase());
		views.setTextViewText(R.id.year, Integer.toString(c.get(Calendar.YEAR)));

		manager.updateAppWidget(thisWidget, views);
	}
}
