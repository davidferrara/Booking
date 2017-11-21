package booking.sp.clbooking;

/**
 * Created by Ty on 11/5/2017.
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.CalendarScopes;

import java.util.Arrays;
import java.util.Date;

public class CreateActivity extends AppCompatActivity {

    //Start Time and Date
    private TimePicker timePicker2;
    private DatePicker datePicker3;
    //End Time and Date
    private TimePicker timePicker3;
    private DatePicker datePicker4;
    //Customer's email
    private EditText customerEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        timePicker2 = findViewById(R.id.timePicker2);
        datePicker3 = findViewById(R.id.datePicker3);
        timePicker3 = findViewById(R.id.timePicker3);
        datePicker4 = findViewById(R.id.datePicker4);
        customerEmail = findViewById(R.id.emailText);
        customerEmail.getText();

        final Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                createEvent(timePicker2, datePicker3, timePicker3, datePicker4, customerEmail);

                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });

    }

    public void createEvent(TimePicker timePicker2, DatePicker datePicker3, TimePicker timePicker3,
    DatePicker datePicker4, EditText customerEmail)
    {
        //"2017-11-20T00:00:00-5:00"
        this.timePicker2 = timePicker2;
        this.datePicker3 = datePicker3;
        this.timePicker3 = timePicker3;
        this.datePicker4 = datePicker4;
        this.customerEmail = customerEmail;

        Event event = new Event();

    //Sets date and time
    //datetime must have this form of parameter
        DateTime startDateTime = new DateTime(new Date(datePicker3.getYear(), datePicker3.getMonth(),
                datePicker3.getDayOfMonth(), timePicker2.getCurrentHour(), timePicker2.getCurrentMinute()));
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime);
        event.setStart(start);

        DateTime endDateTime = new DateTime(new Date(datePicker4.getYear(), datePicker4.getMonth(),
                datePicker4.getDayOfMonth(), timePicker3.getCurrentHour(), timePicker3.getCurrentMinute()));
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime);
        event.setEnd(end);

    //Set attendees
    //Attendees takes an array
        EventAttendee[] attendees = new EventAttendee[] {
                new EventAttendee().setEmail(customerEmail.getText().toString()), //replace this string
        };
        event.setAttendees(Arrays.asList(attendees));

        //String calendarId = "primary";
        //event = service.events().insert(calendarId, event).execute();

    }
}
