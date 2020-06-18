package com.beijingepidial.entity;

public class Circle {
    public int id;
    public int x;
    public int y;
    public String name;
    public int radius;
    public int xDelta;
    public int yDelta;

    public Circle() {
        this.xDelta = 35;
        this.yDelta = 45;
    }

    public Circle(int x, int y) {
        this.x = x;
        this.y = y;
        this.xDelta = 35;
        this.yDelta = 45;
    }

    public void xDeltaAdd() {
        this.xDelta++;
    }

    public void xDeltaLow() {
        this.xDelta--;
    }

    public void yDeltaAdd() {
        this.yDelta++;
    }

    public void yDeltaLow() {
        this.yDelta--;
    }
}
