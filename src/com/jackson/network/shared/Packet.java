package com.jackson.network.shared;

import java.io.Serial;
import java.io.Serializable;

public class Packet implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final Object object;
    private final String msg;
    private String ext;

    public Packet(String msg, Object object) {
        this.object = object;
        this.msg = msg;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public Object getObject() {
        return object;
    }

    public String getMsg() {
        return msg;
    }
}
