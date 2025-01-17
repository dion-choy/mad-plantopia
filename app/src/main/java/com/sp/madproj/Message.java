package com.sp.madproj;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Message {
    String username;
    String email;
    String messageTxt;

    Message(String username, String email, String messageTxt) {
        this.username = username;
        this.email = email;
        this.messageTxt = messageTxt;
    }
}
