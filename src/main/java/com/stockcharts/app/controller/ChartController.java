package com.stockcharts.app.controller;

import com.stockcharts.app.model.ChartRequest;
import com.stockcharts.app.service.ChartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/chart")
public class ChartController {

    @Autowired
    private ChartService chartService;

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateChart(@RequestBody ChartRequest request) {
        try {
            byte[] chartImage = chartService.generateChart(request);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(chartImage.length);
            headers.set("Content-Disposition", "inline; filename=chart.jpg");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(chartImage);
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}