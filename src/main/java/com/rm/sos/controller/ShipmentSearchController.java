package com.rm.sos.controller;

import com.rm.sos.model.Shipment;
import com.rm.sos.service.ShipmentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/shipments/search")
@RequiredArgsConstructor
public class ShipmentSearchController {

    private final ShipmentSearchService service;

    @GetMapping("/match")
    public ResponseEntity<List<Shipment>> match(
            @RequestParam String field,
            @RequestParam String text) throws IOException {
        return ResponseEntity.ok(service.matchSearch(field, text));
    }

    @GetMapping("/match-phrase")
    public ResponseEntity<List<Shipment>> matchPhrase(
            @RequestParam String field,
            @RequestParam String phrase) throws IOException {
        return ResponseEntity.ok(service.matchPhraseSearch(field, phrase));
    }

    @GetMapping("/match-phrase-slop")
    public ResponseEntity<List<Shipment>> matchPhraseSlop(
            @RequestParam String field,
            @RequestParam String phrase,
            @RequestParam int slop) throws IOException {
        return ResponseEntity.ok(service.matchPhraseWithSlopSearch(field, phrase, slop));
    }

    @GetMapping("/term")
    public ResponseEntity<List<Shipment>> term(
            @RequestParam String field,
            @RequestParam String value) throws IOException {
        return ResponseEntity.ok(service.termSearch(field, value));
    }

    @GetMapping("/terms")
    public ResponseEntity<List<Shipment>> terms(
            @RequestParam String field,
            @RequestParam List<String> values) throws IOException {
        return ResponseEntity.ok(service.termsSearch(field, values));
    }

    @GetMapping("/range")
    public ResponseEntity<List<Shipment>> range(
            @RequestParam String field,
            @RequestParam double gte,
            @RequestParam double lte) throws IOException {
        return ResponseEntity.ok(service.rangeSearch(field, gte, lte));
    }

    @GetMapping("/match-all")
    public ResponseEntity<List<Shipment>> matchAll() throws IOException {
        return ResponseEntity.ok(service.matchAllSearch());
    }

    @GetMapping("/multi-match")
    public ResponseEntity<List<Shipment>> multiMatch(
            @RequestParam String text,
            @RequestParam List<String> fields) throws IOException {
        return ResponseEntity.ok(service.multiMatchSearch(text, fields));
    }

    @GetMapping("/bool")
    public ResponseEntity<List<Shipment>> bool(
            @RequestParam String status,
            @RequestParam String carrier,
            @RequestParam String notes) throws IOException {
        return ResponseEntity.ok(service.boolSearch(status, carrier, notes));
    }

    @GetMapping("/prefix")
    public ResponseEntity<List<Shipment>> prefix(
            @RequestParam String field,
            @RequestParam String prefix) throws IOException {
        return ResponseEntity.ok(service.prefixSearch(field, prefix));
    }

    @GetMapping("/wildcard")
    public ResponseEntity<List<Shipment>> wildcard(
            @RequestParam String field,
            @RequestParam String pattern) throws IOException {
        return ResponseEntity.ok(service.wildcardSearch(field, pattern));
    }

    @GetMapping("/exists")
    public ResponseEntity<List<Shipment>> exists(
            @RequestParam String field) throws IOException {
        return ResponseEntity.ok(service.existsSearch(field));
    }

    @GetMapping("/fuzzy")
    public ResponseEntity<List<Shipment>> fuzzy(
            @RequestParam String field,
            @RequestParam String value) throws IOException {
        return ResponseEntity.ok(service.fuzzySearch(field, value));
    }
}
