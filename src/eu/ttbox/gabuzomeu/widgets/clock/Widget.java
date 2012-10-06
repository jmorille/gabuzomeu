package eu.ttbox.gabuzomeu.widgets.clock;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import eu.ttbox.gabuzomeu.R;
import eu.ttbox.gabuzomeu.service.GabuzomeuConverter;

public class Widget extends AppWidgetProvider {
	DateFormat df = new SimpleDateFormat("hh:mm:ss");

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		String dayNames[] = new DateFormatSymbols().getWeekdays();
		String monthNames[] = new DateFormatSymbols().getMonths();
		GabuzomeuConverter converter = new GabuzomeuConverter(context);
		Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
		// Perform this loop procedure for each App Widget that belongs to this
		// provider
		for (int i = 0; i < appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];
			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.widget);
			String hourMinute = String.format("%1$tH:%1$tM",
					System.currentTimeMillis());
			StringBuilder shadokDigit = new StringBuilder(); // For Symbole
			StringBuilder shadokDigitName = new StringBuilder();
			converter.encodeEquationToShadokCode(hourMinute, shadokDigit,
					shadokDigitName);
			views.setTextViewText(R.id.time, shadokDigitName.toString() + ":"
					+ calendar.get(Calendar.SECOND));

			views.setTextViewText(R.id.weekday,
					dayNames[calendar.get(Calendar.DAY_OF_WEEK)].toUpperCase());
			views.setTextViewText(R.id.day,
					Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)));
			views.setTextViewText(R.id.month,
					monthNames[calendar.get(Calendar.MONTH)].toUpperCase());
			views.setTextViewText(R.id.year,
					Integer.toString(calendar.get(Calendar.YEAR)));
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}

	@Override
	public void onDisabled(Context context) {
		Intent intent = new Intent(context, Widget.class);
		PendingIntent sender = PendingIntent
				.getBroadcast(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
		super.onDisabled(context);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, Widget.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
		// After after 3 seconds
		am.setRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + 1000, 1000, pi);
	}


}
