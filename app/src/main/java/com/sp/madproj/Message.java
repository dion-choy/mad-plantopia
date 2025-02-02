package com.sp.madproj;

import android.net.Uri;

import com.google.firebase.Timestamp;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class Message {
    public String username;
    public String uid;
    public String pfp;
    public String messageTxt;
    public TimestampLoader timestamp;
    public List<String> imageKeys;

    public Message() {
        // Required empty constructor
        imageKeys = new ArrayList<>();
    }

    public Message(String username, String uid, Uri pfp, String messageTxt, Timestamp timestamp) {
        this.username = username;
        this.uid = uid;
        this.pfp = pfp.toString();
        this.messageTxt = messageTxt;
        this.timestamp = new TimestampLoader(timestamp);
        imageKeys = new ArrayList<>();
    }
}
