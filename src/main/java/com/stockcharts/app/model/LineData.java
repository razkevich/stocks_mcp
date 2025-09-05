package com.stockcharts.app.model;

import java.awt.Color;
import java.time.LocalDate;

public class LineData {
    private LocalDate startDate;
    private LocalDate endDate;
    private double startValue;
    private double endValue;
    private String color = "#000000";
    private float strokeWidth = 2.0f;
    private String label;

    public LineData() {}

    public LineData(LocalDate startDate, LocalDate endDate, double startValue, double endValue) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.startValue = startValue;
        this.endValue = endValue;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public double getStartValue() {
        return startValue;
    }

    public void setStartValue(double startValue) {
        this.startValue = startValue;
    }

    public double getEndValue() {
        return endValue;
    }

    public void setEndValue(double endValue) {
        this.endValue = endValue;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Color getAwtColor() {
        return Color.decode(color);
    }
}