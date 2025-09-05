package com.stockcharts.app.config;

import com.stockcharts.app.service.PolygonService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {
    
    @Bean
    public PolygonService polygonService() {
        return new PolygonService();
    }
}