package com.stockcharts.app.service;

import com.stockcharts.app.model.OhlcData;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class IndicatorService {

    public static class IndicatorValue {
        private final LocalDate date;
        private final double value;

        public IndicatorValue(LocalDate date, double value) {
            this.date = date;
            this.value = value;
        }

        public LocalDate getDate() { return date; }
        public double getValue() { return value; }
    }

    public static class MacdValue {
        private final LocalDate date;
        private final double macd;
        private final double signal;
        private final double histogram;

        public MacdValue(LocalDate date, double macd, double signal, double histogram) {
            this.date = date;
            this.macd = macd;
            this.signal = signal;
            this.histogram = histogram;
        }

        public LocalDate getDate() { return date; }
        public double getMacd() { return macd; }
        public double getSignal() { return signal; }
        public double getHistogram() { return histogram; }
    }

    @Tool(description = "Calculate technical indicators like SMA, EMA, RSI, MACD for stock data")
    public String calculateIndicator(String symbol, String indicator, Integer period) {
        try {
            // For now, return a mock response since we need actual stock data
            // In a real implementation, you would fetch the data using PolygonService
            if (period == null) period = 14;
            
            return String.format("Calculated %s for %s with period %d. This would return actual indicator values in a real implementation.", 
                indicator.toUpperCase(), symbol, period);
        } catch (Exception e) {
            return "Error calculating indicator: " + e.getMessage();
        }
    }

    public List<IndicatorValue> sma(List<OhlcData> data, int period) {
        List<IndicatorValue> result = new ArrayList<>();
        if (data == null || data.size() < period || period <= 0) return result;

        double sum = 0.0;
        for (int i = 0; i < data.size(); i++) {
            sum += data.get(i).getClose();
            if (i >= period) {
                sum -= data.get(i - period).getClose();
            }
            if (i >= period - 1) {
                double avg = sum / period;
                result.add(new IndicatorValue(data.get(i).getDate(), avg));
            }
        }
        return result;
    }

    public List<IndicatorValue> rsi(List<OhlcData> data, int period) {
        List<IndicatorValue> result = new ArrayList<>();
        if (data == null || data.size() <= period || period <= 0) return result;

        double gainSum = 0.0;
        double lossSum = 0.0;

        // Initial averages using first 'period' deltas
        for (int i = 1; i <= period; i++) {
            double change = data.get(i).getClose() - data.get(i - 1).getClose();
            if (change >= 0) gainSum += change; else lossSum -= change;
        }
        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;

        double rs = avgLoss == 0 ? Double.POSITIVE_INFINITY : (avgGain / avgLoss);
        double rsi = avgLoss == 0 ? 100.0 : (100.0 - (100.0 / (1.0 + rs)));
        result.add(new IndicatorValue(data.get(period).getDate(), rsi));

        // Wilder's smoothing
        for (int i = period + 1; i < data.size(); i++) {
            double change = data.get(i).getClose() - data.get(i - 1).getClose();
            double gain = Math.max(change, 0);
            double loss = Math.max(-change, 0);
            avgGain = ((avgGain * (period - 1)) + gain) / period;
            avgLoss = ((avgLoss * (period - 1)) + loss) / period;
            rs = avgLoss == 0 ? Double.POSITIVE_INFINITY : (avgGain / avgLoss);
            rsi = avgLoss == 0 ? 100.0 : (100.0 - (100.0 / (1.0 + rs)));
            result.add(new IndicatorValue(data.get(i).getDate(), rsi));
        }
        return result;
    }

    public List<MacdValue> macd(List<OhlcData> data, int fast, int slow, int signal) {
        List<MacdValue> result = new ArrayList<>();
        if (data == null || data.size() < slow + signal || fast <= 0 || slow <= 0 || signal <= 0) return result;

        double[] closes = new double[data.size()];
        for (int i = 0; i < data.size(); i++) closes[i] = data.get(i).getClose();

        double[] emaFast = emaSeries(closes, fast);
        double[] emaSlow = emaSeries(closes, slow);

        // MACD line available where both EMAs are defined
        int macdStart = Math.max(firstDefinedIndex(emaFast), firstDefinedIndex(emaSlow));
        List<Double> macdLine = new ArrayList<>();
        List<Integer> macdIdxToPriceIdx = new ArrayList<>();
        for (int i = macdStart; i < closes.length; i++) {
            double macd = emaFast[i] - emaSlow[i];
            macdLine.add(macd);
            macdIdxToPriceIdx.add(i);
        }

        // Signal line EMA over MACD line
        double[] macdArr = macdLine.stream().mapToDouble(Double::doubleValue).toArray();
        double[] signalLine = emaSeries(macdArr, signal);
        int signalStart = firstDefinedIndex(signalLine);

        for (int i = signalStart; i < macdArr.length; i++) {
            int priceIdx = macdIdxToPriceIdx.get(i);
            double macd = macdArr[i];
            double sig = signalLine[i];
            double hist = macd - sig;
            result.add(new MacdValue(data.get(priceIdx).getDate(), macd, sig, hist));
        }
        return result;
    }

    private static int firstDefinedIndex(double[] series) {
        for (int i = 0; i < series.length; i++) {
            if (!Double.isNaN(series[i])) return i;
        }
        return series.length;
    }

    private static double[] emaSeries(double[] values, int period) {
        double[] ema = new double[values.length];
        for (int i = 0; i < ema.length; i++) ema[i] = Double.NaN;
        if (values.length < period || period <= 0) return ema;

        // Seed with SMA
        double sum = 0.0;
        for (int i = 0; i < period; i++) sum += values[i];
        double prevEma = sum / period;
        ema[period - 1] = prevEma;

        double k = 2.0 / (period + 1);
        for (int i = period; i < values.length; i++) {
            double current = values[i];
            prevEma = (current - prevEma) * k + prevEma;
            ema[i] = prevEma;
        }
        return ema;
    }
}

