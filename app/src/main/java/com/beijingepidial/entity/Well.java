package com.beijingepidial.entity;

import java.io.Serializable;

public class Well implements Serializable {
    public String parent;
    public String name;
    public String barcode;
    public long scantime;
    public String color;

    @Override
    public String toString() {
        return "Well{" +
                "parent='" + parent + '\'' +
                ", name='" + name + '\'' +
                ", barcode='" + barcode + '\'' +
                ", scantime=" + scantime +
                ", color='" + color + '\'' +
                '}';
    }
}
