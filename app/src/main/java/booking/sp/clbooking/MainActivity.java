package booking.sp.clbooking;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.v1.ResultStatus;
import com.clover.sdk.v1.ServiceConnector;
import com.clover.sdk.v1.merchant.MerchantConnector;
import com.clover.sdk.v3.employees.Employee;
import com.clover.sdk.v3.employees.EmployeeConnector;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements ServiceConnector.OnServiceConnectedListener{
    //region variables
    private Context mContext;
    private Activity mActivity;
    private Account mAccount;
    private MerchantConnector mMerchantConnector;
    SimpleArrayAdapter adapter;
    ListView listview;
    static Event currentItem;
    SimpleDateFormat sdf = new SimpleDateFormat("MMMM-dd KK:mm a");
    SimpleDateFormat currentDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
    TextView currentDate;
    Date date = new Date();
    Calendar cal;

    public static GoogleAccountCredential mCredential;
    public static TextView mOutputText;
    private TextView mEmployeeTextView;
    ProgressDialog mProgress;
    public static List<Event> events;

    private Spinner employeeSpinner;
    private EmployeeConnector mEmployeeConnector;
    private ArrayList<Employee> employees;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {CalendarScopes.CALENDAR_READONLY};
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();
        mActivity = MainActivity.this;
        listview = findViewById(R.id.listView);
        cal = Calendar.getInstance();  //Gets a calendar using the default time zone and locale.
        currentDate = findViewById(R.id.currentDate);

        //Set the current date in the textView on the main screen
        currentDate.setText(currentDateFormat.format(date));



        if (events != null) {
            adapter = new MainActivity.SimpleArrayAdapter(this, events);
            listview.setAdapter(adapter);
        }

        // When you click on the create button on the main screen, it will start the create activity.
        final Button createButton = findViewById(R.id.createButton);
        createButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent myIntent = new Intent(mContext, CreateActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });


        //Button to change the date back
        final Button previousButton = findViewById(R.id.previousDayButton);
        previousButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            currentDate.setText(currentDateFormat.format(getPreviousDate(date)));
            }
        });

        //Button to change the date forward
        final Button nextButton = findViewById(R.id.nextDayButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            currentDate.setText(currentDateFormat.format(getNextDate(date)));
            }
        });


        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        //endregion

    }

    @Override
    protected void onResume() {
        Log.i("test", "...Resumed.");
        super.onResume();

        //Get API results (events)
        getResultsFromApi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectEmployee();
    }

    //Method to get the previous date
    private Date getPreviousDate(Date d) {
        cal.setTime(d);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date prevDate = cal.getTime();
        date = prevDate;
        return prevDate;
    }

    //Method to get the next date
    private Date getNextDate(Date d) {
        cal.setTime(d);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date nextDate = cal.getTime();
        date = nextDate;
        return nextDate;
    }


    public class SimpleArrayAdapter extends ArrayAdapter<Event> {
        private final Context context;
        private final List<Event> values;

        public SimpleArrayAdapter(Context context, List<Event> values) {
            super(context, -1, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final int mPosition = position;
            final View view = convertView;
            final AdapterView<?> mParent = (AdapterView<?>) parent;

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = null;
            if (inflater != null) {
                rowView = inflater.inflate(R.layout.daylayout, parent, false);
            }

            TextView dayText = rowView.findViewById(R.id.dayText);
            TextView timeText = rowView.findViewById(R.id.timeText);
            TextView descriptionText = rowView.findViewById(R.id.descriptionText);
            Button entryButton = rowView.findViewById(R.id.entryButton);
            Button reminderButton = rowView.findViewById(R.id.reminderButton);
            Button deleteButton = rowView.findViewById(R.id.deleteButton);

            Event e = values.get(position);
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
            timeText.setText("" + sdf.format(sd) + " - " + sdf.format(ed));
            //Get the description of the event.
            descriptionText.setText(e.getDescription());
            entryButton.setText("Edit Entry");
            entryButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    currentItem = (Event) mParent.getItemAtPosition(mPosition);
                    Intent myIntent = new Intent(mContext, EditActivity.class);
                    startActivityForResult(myIntent, 0);
                }
            });
            reminderButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_EMAIL, new String[]{"recipient@example.com"});
                    i.putExtra(Intent.EXTRA_SUBJECT, "subject of email");
                    i.putExtra(Intent.EXTRA_TEXT, "body of email");
                    try {
                        startActivity(Intent.createChooser(i, "Send mail..."));
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            deleteButton.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    final Event item = (Event) mParent.getItemAtPosition(mPosition);
                    final String itemLabel = item.getSummary();
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Title")
                            .setMessage("Do you really want to delete " + itemLabel +"?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    new MainActivity.DeleteEntryTask(mCredential, item).execute();
                                    values.remove(item);
                                    adapter.notifyDataSetChanged();
                                    Toast.makeText(MainActivity.this, itemLabel + " Successfully Deleted", Toast.LENGTH_SHORT).show();
                                }})
                            .setNegativeButton(android.R.string.no, null).show();
                }
            });
            return rowView;
        }
    }

    private class DeleteEntryTask extends AsyncTask<Void, Void, Event> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Event mEvent;

        DeleteEntryTask(GoogleAccountCredential credential, Event event) {
            mEvent = event;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        } public Event DeleteEntry() throws IOException {

            String calendarId = "primary";
            mService.events().delete(calendarId, mEvent.getId()).execute();
            return mEvent;
        }

        /**
         * Background task to call Google Calendar API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected Event doInBackground(Void... params) {
            try {
                return DeleteEntry();
            } catch (Exception e) {
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onCancelled() {
        }

    }




    private void connect() {
        disconnect();
        Log.i("test", "Connecting...");
        if (mAccount != null) {
            Log.i("test", "Account is not null");
            mEmployeeConnector = new EmployeeConnector(this, mAccount, this);
            mEmployeeConnector.connect();
        }
    }

    private void disconnect() {   //remember to disconnect!
        Log.i("test", "Disconnecting...");
        if (mEmployeeConnector != null) {
            mEmployeeConnector.disconnect();
            mEmployeeConnector = null;
        }
    }

    private void getEmployee() {
        // Show progressBar while waiting
        //progressBar.setVisibility(View.VISIBLE);

        mEmployeeConnector.getEmployee(new EmployeeConnector.EmployeeCallback<Employee>() {
            @Override
            public void onServiceSuccess(Employee result, ResultStatus status) {
                super.onServiceSuccess(result, status);

                // Hide the progressBar
                //progressBar.setVisibility(View.GONE);

                mEmployeeTextView.setText(result.getName());
                Log.i("name test", result.getName());
               // role.setText(result.getRole().toString());
            }
        });
    }

    private void connectEmployee() {
        disconnectEmployee();

        if (mAccount != null) {
            mEmployeeConnector = new EmployeeConnector(this, mAccount, null);
            mEmployeeConnector.connect();
        }
    }

    private void disconnectEmployee() {
        if (mEmployeeConnector != null) {
            mEmployeeConnector.disconnect();
            mEmployeeConnector = null;
        }
    }

    /*private class EmployeeAsyncTask extends AsyncTask<Object, Object, Employee> {

        @Override
        protected Employee doInBackground(Object... voids) {
            try {
                return mEmployeeConnector.getEmployee();
            } catch (RemoteException | ClientException | ServiceException | BindingException e) {
                e.printStackTrace();
            }

            return null;
        }


        @Override
        protected final void onPostExecute(Employee employee) {
            super.onPostExecute(employee);
            Log.i("test", employee.getName());
            if(employee != null) {
                mEmployeeTextView.setText("First employee: " + employee.getName());
            }
        }

    }*/

    @Override
    public void onServiceConnected(ServiceConnector<? extends IInterface> serviceConnector) {

    }

    @Override
    public void onServiceDisconnected(ServiceConnector<? extends IInterface> serviceConnector) {

    }


    //region Google Calendar API Methods

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    public void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
            //mOutputText.setText("No network connection available.");
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }
//endregion

    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class CreateEntryTask extends AsyncTask<Void, Void, Event> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        CreateEntryTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }


        public Event createEntryTest() throws IOException {
            Event event = new Event()
                    .setSummary("Google I/O 2015")
                    .setLocation("800 Howard St., San Francisco, CA 94103")
                    .setDescription("A chance to hear more about Google's developer products.");

            DateTime startDateTime = new DateTime("2017-11-28T09:00:00-07:00");
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone("America/Los_Angeles");
            event.setStart(start);

            DateTime endDateTime = new DateTime("2017-11-28T17:00:00-07:00");
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime)
                    .setTimeZone("America/Los_Angeles");
            event.setEnd(end);

            EventAttendee[] attendees = new EventAttendee[] {
                    new EventAttendee().setEmail("lpage@example.com"),
                    new EventAttendee().setEmail("sbrin@example.com"),
            };
            event.setAttendees(Arrays.asList(attendees));

            EventReminder[] reminderOverrides = new EventReminder[] {
                    new EventReminder().setMethod("email").setMinutes(24 * 60),
                    new EventReminder().setMethod("popup").setMinutes(10),
            };
            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides));
            event.setReminders(reminders);

            String calendarId = "primary";
            event = mService.events().insert(calendarId, event).execute();
            return event;
        }
        /**
         * Background task to call Google Calendar API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected Event doInBackground(Void... params) {
            try {
                return createEntryTest();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(Event output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                mOutputText.setText("Error Creating Event");
            } else {
                mOutputText.setText("Event created: "+ output.getHtmlLink());
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }

    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<Event>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<Event> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the next 10 events from the primary calendar.
         *
         * @return List of Strings describing returned events.
         * @throws IOException
         */
        private List<Event> getDataFromApi() throws IOException {
            // List the next 10 events from the primary calendar.
            DateTime now = new DateTime(System.currentTimeMillis());
            List<String> eventStrings = new ArrayList<String>();
            Events events = mService.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            return events.getItems();
        }


        @Override
        protected void onPreExecute() {
            //mOutputText.setText("");
            //mProgress.show();
        }

        @Override
        protected void onPostExecute(List<Event> output) {
            //mProgress.hide();
            //if (output == null || output.size() == 0) {
            //    mOutputText.setText("No results returned.");
            //} else {
            //    List<String> list = new ArrayList<>();
            //    for (Event event: output) {
            //        list.add(event.toString()+"\n");
            //    }
            //    mOutputText.setText(TextUtils.join("\n", list));
            //    events = output;
            //}
        }

        @Override
        protected void onCancelled() {
            //mProgress.hide();
            //if (mLastError != null) {
            //    if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
            //        showGooglePlayServicesAvailabilityErrorDialog(
            //                ((GooglePlayServicesAvailabilityIOException) mLastError)
            //                        .getConnectionStatusCode());
            //    } else if (mLastError instanceof UserRecoverableAuthIOException) {
            //        startActivityForResult(
            //                ((UserRecoverableAuthIOException) mLastError).getIntent(),
            //                MainActivity.REQUEST_AUTHORIZATION);
            //    } else {
            //        mOutputText.setText("The following error occurred:\n"
            //                + mLastError.getMessage());
            //    }
            //} else {
            //    mOutputText.setText("Request cancelled.");
            //}
        }
    }


}