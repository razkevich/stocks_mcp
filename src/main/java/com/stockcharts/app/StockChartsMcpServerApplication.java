package com.stockcharts.app;

import com.stockcharts.app.service.ChartService;
import com.stockcharts.app.service.PolygonService;
import com.stockcharts.app.service.IndicatorService;
import com.stockcharts.app.service.RatioService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class StockChartsMcpServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(StockChartsMcpServerApplication.class, args);
    }
    
    @Bean
    public ToolCallbackProvider stockChartTools(ChartService chartService, 
                                                PolygonService polygonService,
                                                IndicatorService indicatorService,
                                                RatioService ratioService) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(chartService, polygonService, indicatorService, ratioService)
            .build();
    }
    
}