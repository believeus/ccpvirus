package com.beijingepidial.entity;

public class Circle {
    private  int x;
    private int y;
    private  int xDelta;
    private int yDelta;
    public Circle(){}
    public Circle(int x, int y) {
        this.x = x;
        this.y = y;
        this.xDelta=35;
        this.yDelta=45;
    }

    public int getX() {
        return x+xDelta;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y+yDelta;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getxDelta() {
        return xDelta;
    }

    public void setxDelta(int xDelta) {
        this.xDelta = xDelta;
    }

    public int getyDelta() {
        return yDelta;
    }

    public void setyDelta(int yDelta) {
        this.yDelta = yDelta;
    }
}
