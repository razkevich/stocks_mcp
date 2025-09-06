package com.stockcharts.app.model;

public class IndicatorSpec {
    public enum Display { OVERLAY, PANEL }

    private String type; // e.g., SMA, RSI, DPO, MACD
    private int period;  // e.g., 20, 14
    private Display display; // OVERLAY or PANEL

    public IndicatorSpec() {}

    public IndicatorSpec(String type, int period, Display display) {
        this.type = type;
        this.period = period;
        this.display = display;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getPeriod() { return period; }
    public void setPeriod(int period) { this.period = period; }

    public Display getDisplay() { return display; }
    public void setDisplay(Display display) { this.display = display; }
}

