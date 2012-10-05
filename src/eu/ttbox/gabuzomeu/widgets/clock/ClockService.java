package eu.ttbox.gabuzomeu.widgets.clock;

import java.util.Calendar;
import java.util.TimeZone;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;

public class ClockService extends Service {
	private BroadcastReceiver mReceiver = null;
	private Handler m_cHandler = new Handler();
	private Runnable m_cHandlerCallBacks = new Runnable() {
		public void run() {
			updateTime();
			runTimer();
		}
	};

	@Override
	public void onCreate() {
		// REGISTER RECEIVER THAT HANDLES SCREEN ON AND SCREEN OFF LOGIC
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_TIME_CHANGED);
		filter.addAction(Intent.ACTION_DATE_CHANGED);
		filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		mReceiver = new ScreenReceiver();
		registerReceiver(mReceiver, filter);
	}

	private class ScreenReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				m_cHandler.removeCallbacks(m_cHandlerCallBacks);
			} else {
				updateTime();
			}
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		if (intent.getAction().equalsIgnoreCase(Widget.CLOCK_START)) {
			runTimer();
		} else if (intent.getAction().equalsIgnoreCase(Widget.CLOCK_STOP)) {
			m_cHandler.removeCallbacks(m_cHandlerCallBacks);
			stopSelf();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void updateTime() {
		Widget.updateTime(this);
	}

	private void runTimer() {
		Calendar c = Calendar.getInstance(TimeZone.getDefault());
		m_cHandler.postDelayed(m_cHandlerCallBacks,
				(60 - c.get(Calendar.SECOND)) * 1000);
	}
}
