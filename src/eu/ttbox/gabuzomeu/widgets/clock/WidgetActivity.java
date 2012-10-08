package eu.ttbox.gabuzomeu.widgets.clock;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import eu.ttbox.gabuzomeu.R;

public class WidgetActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_widget, menu);
        return true;
    }

    
}
