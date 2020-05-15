package com.beijingepidial.entity;

public class GridCol {
    private  int x;
    private int y;
    private String name;
    private  int xDelta;
    private int yDelta;

    public GridCol(String name){
        this.xDelta=20;
        this.yDelta=-10;
        this.name=name;
    }
    public GridCol(int x, int y, String name) {
        this.x = x;
        this.y = y;
        this.name=name;
        this.xDelta=20;
        this.yDelta=-10;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
