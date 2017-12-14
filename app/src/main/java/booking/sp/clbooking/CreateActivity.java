package booking.sp.clbooking;

/**
 * Created by Ty on 11/5/2017.
 */

import android.accounts.Account;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.v1.BindingException;
import com.clover.sdk.v1.ClientException;
import com.clover.sdk.v1.ServiceException;
import com.clover.sdk.v1.customer.Customer;
import com.clover.sdk.v1.customer.CustomerConnector;
import com.clover.sdk.v1.customer.EmailAddress;
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
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class CreateActivity extends AppCompatActivity {

    private TimePicker timeStart;
    private DatePicker dateStart;
    private TimePicker timeEnd;
    private TextView nameLabel;
    private TextView reasonLabel;
    private EditText reasonText;
    private TextView employeeLabel;
    private TextView locationLabel;
    private EditText locationText;
    public static List<String> employeeArray = new ArrayList<>();
    private Spinner employeeDropDown;
    private List<String> customerArray = new ArrayList<>();
    private List<String> customerEmailArray = new ArrayList<>();
    private Spinner customerDropDown;
    private List<CustomerSpinner> customerSpinners = new ArrayList<>();

    GoogleAccountCredential mCredential = MainActivity.mCredential;
    Calendar calendarStart;
    Calendar calendarEnd;
    String nameString;
    String emailString;
    String reasonString = "";
    String employeeString = "";
    String locationString;

    //Clover variables
    private Account mAccount;
    private EmployeeConnector mEmployeeConnector;
    private CustomerConnector mCustomerConnector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        //Assign all fields
        timeStart = findViewById(R.id.timeStart);
        dateStart = findViewById(R.id.dateStart);
        timeEnd = findViewById(R.id.timeEnd);
        nameLabel = findViewById(R.id.nameLabel);
        customerDropDown = findViewById(R.id.customerDropDown);
        reasonLabel = findViewById(R.id.reasonLabel);
        reasonText = findViewById(R.id.reasonText);
        locationLabel = findViewById(R.id.locationLabel);
        locationText = findViewById(R.id.locationText);

        employeeLabel = findViewById(R.id.employeeLabel);
        employeeDropDown = findViewById(R.id.employeeDropDown);

        //Call method to populate Spinner from ArrayList
        employeeArray.add("Any Available"); //Default, for any employee available.
        addEmployeesToSpinner();

        CustomerSpinner defaultCustomer = new CustomerSpinner("Choose Customer", "abc@example.com");
        customerSpinners.add(defaultCustomer);
        addCustomersToSpinner();

        //Method to populate Customer Spinner

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

        customerDropDown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                CustomerSpinner tempCustomer = (CustomerSpinner) customerDropDown.getSelectedItem();
                nameString = tempCustomer.getCustomerName();
                emailString = tempCustomer.getCustomerEmailAddress();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                nameString = "None";
            }
        });

        final Button saveButton = findViewById(R.id.saveButton);
        saveButton.setText("Create Event");
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
                //emailString = emailText.getText().toString();
                reasonString = reasonText.getText().toString()
                        + "\nWith employee: " + employeeString;
                locationString = locationText.getText().toString();
                new CreateEntryTask(mCredential).execute();
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
        connectCustomer();

        new EmployeeAsyncTask().execute();
        new CustomerAsyncTask().execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectEmployee();
        disconnectCustomer();
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

    private void connectCustomer() {
        disconnectCustomer();

        if (mAccount != null) {
            mCustomerConnector = new CustomerConnector(this, mAccount, null);
            mCustomerConnector.connect();
        }
    }

    private void disconnectCustomer() {
        if (mCustomerConnector != null) {
            mCustomerConnector.disconnect();
            mCustomerConnector = null;
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
                Collections.sort(employeeArray, String.CASE_INSENSITIVE_ORDER);
            }
        }

    }

    private class CustomerAsyncTask extends AsyncTask<Object, Object, List<Customer>> {

        @Override
        protected List<Customer> doInBackground(Object... voids) {
            try {
                List<Customer> customers = mCustomerConnector.getCustomers();
                List<Customer> newCustomers = new ArrayList<>();
                if (customers != null) {
                    for(Customer customer : customers)
                    {
                        Customer c = mCustomerConnector.getCustomer(customer.getId());
                        newCustomers.add(c);
                    }
                    return newCustomers;
                }
            } catch (RemoteException | ClientException | ServiceException | BindingException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected final void onPostExecute(List<Customer> newCustomers) {
            super.onPostExecute(newCustomers);
            if(newCustomers != null)
            {
                for(Customer customer : newCustomers)
                {
                    CustomerSpinner tempCustomer = new CustomerSpinner(customer.getFirstName() + " " + customer.getLastName()
                    , customer.getEmailAddresses().get(0).getEmailAddress());
                    customerSpinners.add(tempCustomer);
                    Log.i("Name", tempCustomer.customerName);
                    Log.i("Email", tempCustomer.emailAddress);
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
        ArrayAdapter<CustomerSpinner> dataAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, customerSpinners);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        customerDropDown.setAdapter(dataAdapter);
    }

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
                    .setSummary(nameString)
                    .setLocation(locationString)
                    .setDescription(reasonString);


            DateTime startDateTime = new DateTime(calendarStart.getTime());
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime);
            //.setTimeZone(TimeZone.getDefault().toString());
            //text.setText(""+(TimeZone.getDefault()));
            event.setStart(start);

            DateTime endDateTime = new DateTime(calendarEnd.getTime());
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime);
            event.setEnd(end);

            EventAttendee[] attendees = new EventAttendee[] {
                    new EventAttendee().setEmail(emailString)
            };
            event.setAttendees(Arrays.asList(attendees));

            EventReminder[] reminderOverrides = new EventReminder[] {
                    new EventReminder().setMethod("email").setMinutes(24 * 60) //email reminder 1 day before
            };
            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides));
            event.setReminders(reminders);

            String calendarId = "primary";
            event = mService.events().insert(calendarId, event).setSendNotifications(Boolean.TRUE).execute();
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

    public static class CustomerSpinner
    {
        public String customerName;
        public String emailAddress;

        public CustomerSpinner(String customerName, String emailAddress)
        {
            this.customerName = customerName;
            this.emailAddress = emailAddress;
        }

        public String getCustomerName()
        {
            return customerName;
        }

        public String getCustomerEmailAddress()
        {
            return emailAddress;
        }

        @Override
        public String toString() {
            return customerName;
        }

    }
}
