package com.stockcharts.app.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class RatioService {
    
    @Tool(description = "Calculate financial ratios and perform basic mathematical operations")
    public String calculateRatio(String operation, Double dividend, Double divisor) {
        try {
            if (dividend == null || divisor == null) {
                return "Error: Both dividend and divisor must be provided";
            }
            
            double result;
            
            switch (operation.toLowerCase()) {
                case "divide":
                case "ratio":
                    if (divisor == 0) {
                        return "Error: Division by zero";
                    }
                    result = dividend / divisor;
                    break;
                case "multiply":
                    result = dividend * divisor;
                    break;
                case "add":
                    result = dividend + divisor;
                    break;
                case "subtract":
                    result = dividend - divisor;
                    break;
                case "percentage":
                    if (divisor == 0) {
                        return "Error: Division by zero";
                    }
                    result = (dividend / divisor) * 100;
                    break;
                default:
                    return "Error: Unsupported operation: " + operation + ". Supported operations: divide, multiply, add, subtract, percentage";
            }
            
            return String.format("Calculated %s: %.4f", operation, result);
        } catch (Exception e) {
            return "Error in calculation: " + e.getMessage();
        }
    }
}