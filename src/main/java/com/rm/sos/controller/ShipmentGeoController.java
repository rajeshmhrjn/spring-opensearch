package com.rm.sos.controller;

import com.rm.sos.model.Shipment;
import com.rm.sos.service.ShipmentGeoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipments/geo")
public class ShipmentGeoController {

    private final ShipmentGeoService service;

    public ShipmentGeoController(ShipmentGeoService service) {
        this.service = service;
    }

    // Find shipments within X km of a point
    @GetMapping("/distance")
    public ResponseEntity<List<Shipment>> withinDistance(
            @RequestParam String field,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam double distanceKm) throws IOException {
        return ResponseEntity.ok(service.findWithinDistance(field, lat, lon, distanceKm));
    }

    // Find shipments within a bounding box (rectangle)
    @GetMapping("/bounding-box")
    public ResponseEntity<List<Shipment>> withinBoundingBox(
            @RequestParam String field,
            @RequestParam double topLeftLat,
            @RequestParam double topLeftLon,
            @RequestParam double bottomRightLat,
            @RequestParam double bottomRightLon) throws IOException {
        return ResponseEntity.ok(service.findWithinBoundingBox(
                field, topLeftLat, topLeftLon, bottomRightLat, bottomRightLon));
    }

    // Find shipments by status, sorted nearest first
    @GetMapping("/sorted-by-distance")
    public ResponseEntity<List<Shipment>> sortedByDistance(
            @RequestParam String field,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam String status) throws IOException {
        return ResponseEntity.ok(service.findSortedByDistance(field, lat, lon, status));
    }

    // Group shipments into distance rings from a point
    @GetMapping("/distance-rings")
    public ResponseEntity<Map<String, Long>> distanceRings(
            @RequestParam String field,
            @RequestParam double lat,
            @RequestParam double lon) throws IOException {
        return ResponseEntity.ok(service.aggregateByDistance(field, lat, lon));
    }
}
