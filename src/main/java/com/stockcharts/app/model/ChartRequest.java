package com.stockcharts.app.model;

import java.util.List;

public class ChartRequest {
    private List<OhlcData> ohlcData;
    private List<LineData> lines;
    private String title = "Stock Chart";
    private int width = 800;
    private int height = 600;

    public ChartRequest() {}

    public ChartRequest(List<OhlcData> ohlcData, List<LineData> lines) {
        this.ohlcData = ohlcData;
        this.lines = lines;
    }

    public List<OhlcData> getOhlcData() {
        return ohlcData;
    }

    public void setOhlcData(List<OhlcData> ohlcData) {
        this.ohlcData = ohlcData;
    }

    public List<LineData> getLines() {
        return lines;
    }

    public void setLines(List<LineData> lines) {
        this.lines = lines;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}