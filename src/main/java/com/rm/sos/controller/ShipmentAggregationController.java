package com.rm.sos.controller;


import com.rm.sos.service.ShipmentAggregationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/shipments/aggregations")
public class ShipmentAggregationController {

    private final ShipmentAggregationService service;

    public ShipmentAggregationController(ShipmentAggregationService service) {
        this.service = service;
    }

    // GROUP BY any keyword field
    @GetMapping("/count-by")
    public ResponseEntity<Map<String, Long>> countBy(@RequestParam String field) throws IOException {
        return ResponseEntity.ok(service.countByField(field));
    }

    // Count non-null values for a field
    @GetMapping("/value-count")
    public ResponseEntity<Long> valueCount(@RequestParam String field) throws IOException {
        return ResponseEntity.ok(service.valueCount(field));
    }

    // count/min/max/avg/sum in one shot
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Double>> stats(@RequestParam String field) throws IOException {
        return ResponseEntity.ok(service.stats(field));
    }

    // Individual avg/sum/min/max
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Double>> metrics(@RequestParam String field) throws IOException {
        return ResponseEntity.ok(service.metrics(field));
    }

    // Group into value ranges
    @GetMapping("/range")
    public ResponseEntity<Map<String, Long>> range() throws IOException {
        return ResponseEntity.ok(service.rangeAggregation());
    }

    // Group by time interval (Day/Week/Month)
    @GetMapping("/date-histogram")
    public ResponseEntity<Map<String, Long>> dateHistogram(@RequestParam(defaultValue = "Day") String interval) throws IOException {
        return ResponseEntity.ok(service.dateHistogram(interval));
    }

    // Avg declared value per carrier (nested agg)
    @GetMapping("/avg-value-by-carrier")
    public ResponseEntity<Map<String, Double>> avgValueByCarrier() throws IOException {
        return ResponseEntity.ok(service.avgValueByCarrier());
    }

    // Aggregation scoped to a status filter
    @GetMapping("/filtered")
    public ResponseEntity<Map<String, Object>> filtered(@RequestParam String status) throws IOException {
        return ResponseEntity.ok(service.filteredAggregation(status));
    }
}
