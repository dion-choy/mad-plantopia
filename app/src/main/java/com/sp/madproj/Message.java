package com.sp.madproj;

import android.net.Uri;

import com.google.firebase.Timestamp;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Message {
    public String username;
    public String pfp;
    public String messageTxt;
    public TimestampLoader timestamp;

    public Message() {
        // Required empty constructor
    }

    public Message(String username, Uri pfp, String messageTxt, Timestamp timestamp) {
        this.username = username;
        this.pfp = pfp.toString();
        this.messageTxt = messageTxt;
        this.timestamp = new TimestampLoader(timestamp);
    }
}
