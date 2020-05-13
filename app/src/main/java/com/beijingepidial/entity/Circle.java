package com.beijingepidial.entity;

public class Circle {
    private  int x;
    private int y;
    private  int xDelta;
    private int yDelta;
    public Circle(){
        this.xDelta=35;
        this.yDelta=45;
    }
    public Circle(int x, int y) {
        this.x = x;
        this.y = y;
        this.xDelta=35;
        this.yDelta=45;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
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
    public void xDeltaAdd(){
        this.xDelta++;
    }
    public void xDeltaLow(){
        this.xDelta--;
    }
    public void yDeltaAdd(){
        this.yDelta++;
    }
    public void yDeltaLow(){
        this.yDelta--;
    }
}
