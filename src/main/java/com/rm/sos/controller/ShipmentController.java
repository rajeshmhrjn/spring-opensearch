package com.rm.sos.controller;


import com.rm.sos.model.Shipment;
import com.rm.sos.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService service;

    @PostMapping("/init")
    public ResponseEntity<String> initIndex() throws IOException {
        service.createIndex();
        return ResponseEntity.ok("Shipments index ready");
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody Shipment shipment) throws IOException {
        String id = service.indexShipment(shipment);
        return ResponseEntity.ok("Indexed shipment: " + id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shipment> getById(@PathVariable String id) throws IOException {
        Shipment shipment = service.getShipment(id);
        return shipment != null
                ? ResponseEntity.ok(shipment)
                : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<Shipment>> getAll() throws IOException {
        return ResponseEntity.ok(service.getAllShipments());
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(
            @PathVariable String id,
            @RequestBody Shipment shipment) throws IOException {
        String result = service.updateShipment(id, shipment);
        return ResponseEntity.ok("Updated shipment " + id + ": " + result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id) throws IOException {
        service.deleteShipment(id);
        return ResponseEntity.ok("Deleted shipment: " + id);
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulkCreate(
            @RequestBody List<Shipment> shipments) throws IOException {

        List<String> indexedIds = service.bulkIndexShipments(shipments);

        Map<String, Object> response = new HashMap<>();
        response.put("total", indexedIds.size());
        response.put("ids", indexedIds);

        return ResponseEntity.ok(response);
    }
}
