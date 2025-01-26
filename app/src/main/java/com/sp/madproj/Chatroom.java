package com.sp.madproj;

public class Chatroom {
    public String name;
    public String iconKey;
    public String key;

    public Chatroom() {
    }

    public Chatroom(String name, String iconKey) {
        this.name = name;
        this.iconKey = iconKey;
    }

    public Chatroom setKey(String key) {
        this.key = key;
        return this;
    }
}
