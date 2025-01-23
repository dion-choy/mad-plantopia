package com.sp.madproj;

public class Chatroom {
    public String name;
    public String iconKey;
    public String code;

    public Chatroom() {
    }

    public Chatroom(String name, String iconKey) {
        this.name = name;
        this.iconKey = iconKey;
    }

    public Chatroom setCode(String code) {
        this.code = code;
        return this;
    }
}
