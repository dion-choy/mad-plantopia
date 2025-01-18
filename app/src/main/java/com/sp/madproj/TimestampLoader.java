package com.sp.madproj;


import com.google.firebase.Timestamp;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class TimestampLoader {
    public int nanoseconds;
    public float seconds;

    public TimestampLoader() {
        // Required empty constructor
    }

    public TimestampLoader(Timestamp timestamp) {
        this.nanoseconds = timestamp.getNanoseconds();
        this.seconds = timestamp.getSeconds();
    }
}
