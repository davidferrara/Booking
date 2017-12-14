package booking.sp.clbooking;

/**
 * Created by Ty on 11/5/2017.
 */

import android.accounts.Account;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.v1.BindingException;
import com.clover.sdk.v1.ClientException;
import com.clover.sdk.v1.ServiceException;
import com.clover.sdk.v1.customer.Customer;
import com.clover.sdk.v3.employees.Employee;
import com.clover.sdk.v3.employees.EmployeeConnector;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static booking.sp.clbooking.MainActivity.currentItem;

public class EditActivity extends AppCompatActivity {
    private TimePicker timeStart;
    private DatePicker dateStart;
    private TimePicker timeEnd;
    private TextView nameLabel;
    private TextView emailLabel;
    private TextView reasonLabel;
    private EditText reasonText;
    private TextView employeeLabel;
    private TextView locationLabel;
    private EditText locationText;
    private List<String> employeeArray = new ArrayList<>();
    private List<String> testArray = new ArrayList<>();
    private Spinner employeeDropDown;
    private Spinner customerDropDown;
    private List<CreateActivity.CustomerSpinner> customerSpinners = new ArrayList<>();

    GoogleAccountCredential mCredential = MainActivity.mCredential;
    Calendar calendarStart;
    Calendar calendarEnd;
    String nameString;
    String emailString;
    String reasonString = "";
    String employeeString = "";
    String locationString;
    String tempReasonString;
    String[] tempReasonArray;

    //Clover variables
    private Account mAccount;
    private EmployeeConnector mEmployeeConnector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        //Assign all fields
        timeStart = findViewById(R.id.timeStart);
        dateStart = findViewById(R.id.dateStart);
        timeEnd = findViewById(R.id.timeEnd);
        nameLabel = findViewById(R.id.nameLabel);
        reasonLabel = findViewById(R.id.reasonLabel);
        reasonText = findViewById(R.id.reasonText);
        locationLabel = findViewById(R.id.locationLabel);
        locationText = findViewById(R.id.locationText);

        employeeLabel = findViewById(R.id.employeeLabel);
        employeeDropDown = findViewById(R.id.employeeDropDown);
        customerDropDown = findViewById(R.id.customerDropDown);

        tempReasonArray = currentItem.getDescription().split("\n");
        tempReasonString = tempReasonArray[0];
        reasonText.setText(tempReasonString);
        locationText.setText(currentItem.getLocation());

        employeeArray.add("Any Available");
        //Call method to populate Spinner from ArrayList
        addEmployeesToSpinner();

        employeeDropDown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                employeeString = employeeDropDown.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                employeeString = "Any Available";
            }

        });



        CreateActivity.CustomerSpinner defaultCustomer = new CreateActivity.CustomerSpinner("Choose Customer",
                "abc@example.com");
        CreateActivity.CustomerSpinner currentCustomer = new CreateActivity.CustomerSpinner(currentItem.getSummary()
        , currentItem.getAttendees().get(0).toString());
        customerSpinners.add(defaultCustomer);
        customerSpinners.add(currentCustomer);

        addCustomersToSpinner();
        customerDropDown.setSelection(1);

        customerDropDown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                CreateActivity.CustomerSpinner tempCustomer = (CreateActivity.CustomerSpinner) customerDropDown.getSelectedItem();
                nameString = tempCustomer.getCustomerName();
                emailString = tempCustomer.getCustomerEmailAddress();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                nameString = "None";
            }
        });

        final Button saveButton = findViewById(R.id.saveButton);
        saveButton.setText("Save Changes");
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                calendarStart = new GregorianCalendar(
                        dateStart.getYear(),
                        dateStart.getMonth(),
                        dateStart.getDayOfMonth(),
                        timeStart.getCurrentHour(),
                        timeStart.getCurrentMinute(),
                        00);
                calendarEnd = new GregorianCalendar(
                        dateStart.getYear(),
                        dateStart.getMonth(),
                        dateStart.getDayOfMonth(),
                        timeEnd.getCurrentHour(),
                        timeEnd.getCurrentMinute(),
                        00);
                reasonString = reasonText.getText().toString()
                        + "\nWith employee: " + employeeString;
                locationString = locationText.getText().toString();
                new EditActivity.EditEntryTask(mCredential).execute();
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Retrieve Clover account
        if (mAccount == null) {
            mAccount = CloverAccount.getAccount(this);

            if (mAccount == null) {
                return;
            }
        }

        connectEmployee();

        new EditActivity.EmployeeAsyncTask().execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectEmployee();
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

    private class EmployeeAsyncTask extends AsyncTask<Object, Object, List<Employee>> {

        @Override
        protected List<Employee> doInBackground(Object... voids) {
            try {
                return mEmployeeConnector.getEmployees();
            } catch (RemoteException | ClientException | ServiceException | BindingException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected final void onPostExecute(List<Employee> employees) {
            super.onPostExecute(employees);
            if (employees != null) {
                for(Employee employee : employees)
                {
                    employeeArray.add(employee.getNickname());
                    Log.i("Employee Added", "Adding " + employee.getNickname() + " to the arraylist.");
                }

            }
        }

    }

    private void addEmployeesToSpinner()
    {
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, employeeArray);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        employeeDropDown.setAdapter(dataAdapter);
    }

    private void addCustomersToSpinner()
    {
        ArrayAdapter<CreateActivity.CustomerSpinner> dataAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, customerSpinners);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        customerDropDown.setAdapter(dataAdapter);
    }

    private class EditEntryTask extends AsyncTask<Void, Void, Event> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        EditEntryTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        //right now only changes time
        public Event editEntryTest() throws IOException {

            // Retrieve the event from the API
            Event event = mService.events().get("primary", currentItem.getId()).execute();

            // Make changes
            DateTime startDateTime = new DateTime(calendarStart.getTime());
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime);
            event.setStart(start);

            //event.setSummary("Appointment at Somewhere");

            DateTime endDateTime = new DateTime(calendarEnd.getTime());
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime);
            event.setEnd(end);
            event.setSummary(nameString)
                    .setLocation(locationString)
                    .setDescription(reasonString);

            // Update the event
            Event updatedEvent = mService.events().update("primary", event.getId(), event).execute();
            return updatedEvent;
        }

        /**
         * Background task to call Google Calendar API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected Event doInBackground(Void... params) {
            try {
                return editEntryTest();
            } catch (Exception e) {
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Event output) {
            if (output == null) {
                //text.setText("Event is null");
            } else {
                //text.setText(""+output.toString());
            }
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    //text.setText("The following error occurred:\n"
                    // + mLastError.getMessage());
                }
            } else {
                //text.setText("Request cancelled.");
            }
        }
    }

    private class GetEntryTask extends AsyncTask<Void, Void, Event> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        GetEntryTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }
        public Event getEntryTest() throws IOException {

            // Retrieve the event from the API
            Event event = mService.events().get("primary", currentItem.getId()).execute();
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
                return getEntryTest();
            } catch (Exception e) {
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Event output) {

            //set ui to event values
            EventDateTime start = output.getStart();
            final Calendar calS = Calendar.getInstance();
            calS.setTimeInMillis(start.getDateTime().getValue());
            final int minuteS = calS.get(Calendar.MINUTE);
            final int hourS = calS.get(Calendar.HOUR_OF_DAY);
            timeStart.setCurrentHour(hourS);
            timeStart.setCurrentMinute(minuteS);
            dateStart.updateDate(calS.get(Calendar.YEAR), calS.get(Calendar.MONTH), calS.get(Calendar.DAY_OF_MONTH));


            EventDateTime end = output.getEnd();
            final Calendar calE = Calendar.getInstance();
            calE.setTimeInMillis(end.getDateTime().getValue());
            final int minuteE = calE.get(Calendar.MINUTE);
            final int hourE = calE.get(Calendar.HOUR_OF_DAY);
            timeEnd.setCurrentHour(hourE);
            timeEnd.setCurrentMinute(minuteE);
            dateStart.updateDate(calE.get(Calendar.YEAR), calE.get(Calendar.MONTH), calE.get(Calendar.DAY_OF_MONTH));

            if (output == null) {
                //text.setText("Event is null");
            } else {
                //text.setText(""+output.toString());
            }
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    //text.setText("The following error occurred:\n"
                    // + mLastError.getMessage());
                }
            } else {
                //text.setText("Request cancelled.");
            }
        }
    }
}
