package com.beijingepidial.entity;

import java.util.Objects;

public class Well {
    public int id;
    public String name;
    public String barcode;
    public long scantime;
    public int row;
    public int col;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Well well = (Well) o;
        return id == well.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
