package booking.sp.clbooking;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.calendar.CalendarScopes;
import com.google.api.client.util.DateTime;

import com.google.api.services.calendar.model.*;


/**
 * Created by Ty on 11/5/2017.
 */

public class ViewActivity extends AppCompatActivity {
    private Context mContext;
    private Activity mActivity;
    public List<Event> events;
    SimpleDateFormat sdf = new SimpleDateFormat("MMMM-dd KK:mm a");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        // Get the application context
        mContext = getApplicationContext();
        // Get the activity
        mActivity = ViewActivity.this;
        final ListView listview = findViewById(R.id.listView);

        //final StableArrayAdapter adapter = new StableArrayAdapter(this,
         //       android.R.layout.simple_list_item_1, list);
        events = MainActivity.events;
        if(events != null) {

            final MySimpleArrayAdapter adapter = new MySimpleArrayAdapter(this, events.toArray(new Event[events.size()]));

            listview.setAdapter(adapter);

            listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, final View view,
                                        int position, long id) {
                    final String item = (String) parent.getItemAtPosition(position);
                   // view.animate().setDuration(2000).alpha(0)
                            //.withEndAction(new Runnable() {
                                //@Override
                                //public void run() {
                                    //list.remove(item);
                                    //adapter.notifyDataSetChanged();
                                    //view.setAlpha(1);
                               // }
                            //});
                }

            });
        }
        final Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        final Button createButton = findViewById(R.id.createButton);
        createButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                    Intent myIntent = new Intent(mContext, CreateActivity.class);
                    startActivityForResult(myIntent, 0);
            }
        });
    }
    protected ListView getListView(){
        return findViewById(R.id.listView);
    }

    //region arrayadapter
    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }

    //endregion

    public class MySimpleArrayAdapter extends ArrayAdapter<Event> {
        private final Context context;
        private final Event[] values;

        public MySimpleArrayAdapter(Context context, Event[] values) {
            super(context, -1, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = null;
            if (inflater != null) {
                rowView = inflater.inflate(R.layout.daylayout, parent, false);
            }

            //for(Event event : events){
            //    EventDateTime evt = event.getStart().getDate();
            //}
            TextView dayText = rowView.findViewById(R.id.dayText);
            TextView timeText = rowView.findViewById(R.id.timeText);
            TextView descriptionText = rowView.findViewById(R.id.descriptionText);
            Button entryButton = rowView.findViewById(R.id.entryButton);
            Button reminderButton = rowView.findViewById(R.id.reminderButton);

            Event e = values[position];
            DateTime start = e.getStart().getDateTime();
            if (start == null) {
                // All-day events don't have start times, so just use
                // the start date.
                start = e.getStart().getDate();
            }
            Date sd = new Date(start.getValue());
            Date ed = new Date(e.getEnd().getDateTime().getValue());
            //Title of Event.
            dayText.setText(e.getSummary());
            //Date and Time of Event.
            timeText.setText(""+ sdf.format(sd) +" - "+ sdf.format(ed));
            //Get the description of the event.
            descriptionText.setText(e.getDescription());
            entryButton.setText("Edit Entry");
            entryButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent myIntent = new Intent(mContext, EditActivity.class);
                    startActivityForResult(myIntent, 0);
                }
            });
            reminderButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"recipient@example.com"});
                    i.putExtra(Intent.EXTRA_SUBJECT, "subject of email");
                    i.putExtra(Intent.EXTRA_TEXT   , "body of email");
                    try {
                        startActivity(Intent.createChooser(i, "Send mail..."));
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(ViewActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            //timeText2.setText("TIME TEXT2");
            //entryButton.setText("ENTRY");
            //entryButton2.setText("ENTRY");

            return rowView;
        }
    }

}

