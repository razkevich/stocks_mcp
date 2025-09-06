package com.stockcharts.app.service;

import com.stockcharts.app.model.OhlcData;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class IndicatorService {
    
    private final PolygonService polygonService;
    
    public IndicatorService(PolygonService polygonService) {
        this.polygonService = polygonService;
    }

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

    @Tool(description = "Calculate technical indicators (SMA, EMA, RSI, MACD, DPO) for a stock symbol or ratio. " +
          "Supports ratios like 'AAPL/SPY' to analyze relative performance. " +
          "Parameters: symbol (e.g., 'AAPL' or 'AAPL/SPY'), indicator ('SMA'|'EMA'|'RSI'|'MACD'|'DPO'), period (e.g., 14, 20, 50). " +
          "Returns formatted table with indicator values for the last 10 data points.")
    public String calculateIndicator(String symbol, String indicator, Integer period) {
        try {
            if (period == null) period = 14;
            
            List<OhlcData> stockData;
            String title;
            
            // Check if this is a ratio (contains '/')
            if (symbol.contains("/")) {
                String[] symbols = symbol.split("/");
                if (symbols.length != 2) {
                    return "Invalid ratio format. Use format: SYMBOL1/SYMBOL2 (e.g., AAPL/SPY)";
                }
                
                String numerator = symbols[0].trim().toUpperCase();
                String denominator = symbols[1].trim().toUpperCase();
                title = numerator + "/" + denominator + " Ratio";
                
                // Get data for both symbols and calculate ratio
                stockData = calculateRatioData(numerator, denominator);
            } else {
                title = symbol.toUpperCase();
                // Fetch stock data for single symbol
                stockData = polygonService.getAggregates(
                    symbol, "1", "day", 
                    "2025-08-01", "2025-09-05", 
                    true, "asc", 100);
            }
                
            if (stockData.isEmpty()) {
                return "No data available for " + symbol;
            }
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("%s Indicator for %s (period: %d):\n\n", 
                indicator.toUpperCase(), title, period));
            
            switch (indicator.toLowerCase()) {
                case "sma":
                    List<IndicatorValue> smaValues = sma(stockData, period);
                    result.append("Date       | SMA\n");
                    result.append("-----------|--------\n");
                    for (IndicatorValue value : smaValues.subList(Math.max(0, smaValues.size() - 10), smaValues.size())) {
                        result.append(String.format("%-10s | %7.2f\n", value.getDate(), value.getValue()));
                    }
                    break;
                    
                case "ema":
                    List<IndicatorValue> emaValues = ema(stockData, period);
                    result.append("Date       | EMA\n");
                    result.append("-----------|--------\n");
                    for (IndicatorValue value : emaValues.subList(Math.max(0, emaValues.size() - 10), emaValues.size())) {
                        result.append(String.format("%-10s | %7.2f\n", value.getDate(), value.getValue()));
                    }
                    break;
                    
                case "rsi":
                    List<IndicatorValue> rsiValues = rsi(stockData, period);
                    result.append("Date       | RSI\n");
                    result.append("-----------|--------\n");
                    for (IndicatorValue value : rsiValues.subList(Math.max(0, rsiValues.size() - 10), rsiValues.size())) {
                        result.append(String.format("%-10s | %7.2f\n", value.getDate(), value.getValue()));
                    }
                    break;
                    
                case "dpo":
                case "detrended":
                    List<IndicatorValue> dpoValues = detrendedPriceOscillator(stockData, period);
                    result.append("Date       | DPO\n");
                    result.append("-----------|--------\n");
                    for (IndicatorValue value : dpoValues.subList(Math.max(0, dpoValues.size() - 10), dpoValues.size())) {
                        result.append(String.format("%-10s | %7.2f\n", value.getDate(), value.getValue()));
                    }
                    break;
                    
                case "macd":
                    List<MacdValue> macdValues = macd(stockData, 12, 26, 9);
                    result.append("Date       | MACD   | Signal | Histogram\n");
                    result.append("-----------|--------|--------|----------\n");
                    for (MacdValue value : macdValues.subList(Math.max(0, macdValues.size() - 10), macdValues.size())) {
                        result.append(String.format("%-10s | %6.3f | %6.3f | %9.3f\n", 
                            value.getDate(), value.getMacd(), value.getSignal(), value.getHistogram()));
                    }
                    break;
                    
                default:
                    return "Unsupported indicator: " + indicator + ". Supported: SMA, EMA, RSI, DPO/DETRENDED, MACD";
            }
            
            result.append(String.format("\nTotal data points: %d", stockData.size()));
            return result.toString();
            
        } catch (Exception e) {
            return "Error calculating indicator: " + e.getMessage();
        }
    }

    private List<OhlcData> calculateRatioData(String numerator, String denominator) throws Exception {
        // Get data for both symbols
        List<OhlcData> numeratorData = polygonService.getAggregates(
            numerator, "1", "day", "2025-08-01", "2025-09-05", true, "asc", 100);
            
        List<OhlcData> denominatorData = polygonService.getAggregates(
            denominator, "1", "day", "2025-08-01", "2025-09-05", true, "asc", 100);
        
        if (numeratorData.isEmpty() || denominatorData.isEmpty()) {
            throw new Exception("No data available for one or both symbols: " + numerator + ", " + denominator);
        }
        
        // Calculate ratio data
        List<OhlcData> ratioData = new ArrayList<>();
        int minSize = Math.min(numeratorData.size(), denominatorData.size());
        
        for (int i = 0; i < minSize; i++) {
            OhlcData num = numeratorData.get(i);
            OhlcData den = denominatorData.get(i);
            
            // Create ratio OHLC data
            double open = num.getOpen() / den.getOpen();
            double high = num.getHigh() / den.getHigh();
            double low = num.getLow() / den.getLow();
            double close = num.getClose() / den.getClose();
            
            ratioData.add(new OhlcData(num.getDate(), open, high, low, close));
        }
        
        return ratioData;
    }

    // Technical indicator calculation methods
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

    public List<IndicatorValue> ema(List<OhlcData> data, int period) {
        List<IndicatorValue> result = new ArrayList<>();
        if (data == null || data.size() < period || period <= 0) return result;

        double[] closes = new double[data.size()];
        for (int i = 0; i < data.size(); i++) {
            closes[i] = data.get(i).getClose();
        }

        double[] emaValues = emaSeries(closes, period);
        
        for (int i = 0; i < emaValues.length; i++) {
            if (!Double.isNaN(emaValues[i])) {
                result.add(new IndicatorValue(data.get(i).getDate(), emaValues[i]));
            }
        }
        return result;
    }

    public List<IndicatorValue> detrendedPriceOscillator(List<OhlcData> data, int period) {
        List<IndicatorValue> result = new ArrayList<>();
        if (data == null || data.size() < period || period <= 0) return result;

        // Calculate SMA first
        List<IndicatorValue> smaValues = sma(data, period);
        
        // DPO = Current Price - SMA(period)
        // Since SMA values start from index (period-1), align them with price data
        for (int i = 0; i < smaValues.size(); i++) {
            int priceIndex = i + period - 1; // Align with original data index
            if (priceIndex < data.size()) {
                double close = data.get(priceIndex).getClose();
                double smaValue = smaValues.get(i).getValue();
                double dpo = close - smaValue;
                result.add(new IndicatorValue(data.get(priceIndex).getDate(), dpo));
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