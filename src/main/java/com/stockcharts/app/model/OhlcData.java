package com.stockcharts.app.model;

import java.time.LocalDate;

public class OhlcData {
    private LocalDate date;
    private double open;
    private double high;
    private double low;
    private double close;
    private double percentReturn; // Current day's close / previous day's close

    public OhlcData() {}

    public OhlcData(LocalDate date, double open, double high, double low, double close) {
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.percentReturn = 1.0; // Default to no change
    }

    public OhlcData(LocalDate date, double open, double high, double low, double close, double percentReturn) {
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.percentReturn = percentReturn;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public double getPercentReturn() {
        return percentReturn;
    }

    public void setPercentReturn(double percentReturn) {
        this.percentReturn = percentReturn;
    }
}