package com.stockcharts.app.controller;

import com.stockcharts.app.service.PolygonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock")
public class StockDataController {

    @Autowired
    private PolygonService polygonService;

    @GetMapping("/data")
    public ResponseEntity<String> getStockData(
            @RequestParam String ticker,
            @RequestParam(defaultValue = "1") String multiplier,
            @RequestParam(defaultValue = "day") String timespan,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "true") boolean adjusted,
            @RequestParam(defaultValue = "asc") String sort,
            @RequestParam(defaultValue = "120") int limit) {
        
        try {
            String stockData = polygonService.getStockDataAsText(ticker, multiplier, timespan, from, to, adjusted, sort, limit);
            return ResponseEntity.ok()
                .header("Content-Type", "text/plain")
                .body(stockData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error fetching stock data: " + e.getMessage());
        }
    }
}