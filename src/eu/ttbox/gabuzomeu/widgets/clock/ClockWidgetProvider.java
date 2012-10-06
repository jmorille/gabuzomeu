package eu.ttbox.gabuzomeu.widgets.clock;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.TimeZone;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;
import eu.ttbox.gabuzomeu.R;
import eu.ttbox.gabuzomeu.service.GabuzomeuConverter;

public class ClockWidgetProvider extends AppWidgetProvider {

	public static String WEEKDAYS_NAMES[] = new DateFormatSymbols()
			.getWeekdays();
	public static String MONTHS_NAMES[] = new DateFormatSymbols().getMonths();

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		Toast.makeText(context, "TimeWidgetRemoved id(s):" + appWidgetIds,
				Toast.LENGTH_SHORT).show();
		super.onDeleted(context, appWidgetIds);
	}

	@Override
	public void onDisabled(Context context) {
		//Toast.makeText(context, "onDisabled():last widget instance removed",
		//		Toast.LENGTH_SHORT).show();
		Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
		PendingIntent sender = PendingIntent
				.getBroadcast(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
		super.onDisabled(context);
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
		// After after 3 seconds
		am.setRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + 100 * 3, 1000, pi);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		ComponentName thisWidget = new ComponentName(context,
				ClockWidgetProvider.class);

		for (int widgetId : appWidgetManager.getAppWidgetIds(thisWidget)) {
			onUpdate(context, appWidgetManager, widgetId);
		}
	}

	private void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int widgetId) {
		// Get the remote views
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
				R.layout.widget);
		updateRemoveViews(remoteViews, context);
		appWidgetManager.updateAppWidget(widgetId, remoteViews);
	}

	public static void updateRemoveViews(RemoteViews remoteViews,
			Context context) {
		GabuzomeuConverter converter = new GabuzomeuConverter(context);
		Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
		String hourMinute = String.format("%1$tH:%1$tM",
				calendar.getTimeInMillis());
		StringBuilder shadokDigit = new StringBuilder();
		StringBuilder shadokDigitName = new StringBuilder();
		converter.encodeEquationToShadokCode(hourMinute, shadokDigit,
				shadokDigitName);
		
		remoteViews.setTextViewText(R.id.time, shadokDigitName.toString() + ":"
				+ calendar.get(Calendar.SECOND));
		remoteViews.setTextViewText(R.id.weekday, WEEKDAYS_NAMES[calendar
				.get(Calendar.DAY_OF_WEEK)].toUpperCase());
		remoteViews.setTextViewText(R.id.day,
				Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)));
		remoteViews.setTextViewText(R.id.month,
				MONTHS_NAMES[calendar.get(Calendar.MONTH)].toUpperCase());
		remoteViews.setTextViewText(R.id.year,
				Integer.toString(calendar.get(Calendar.YEAR)));
	}

	@Override
	public void onAppWidgetOptionsChanged(Context context,
			AppWidgetManager appWidgetManager, int appWidgetId,
			Bundle newOptions) {
		// Do some operation here, once you see that the widget has change its
		// size or position.
		// Toast.makeText(context, "onAppWidgetOptionsChanged() called",
		// Toast.LENGTH_SHORT).show();
	}

}
