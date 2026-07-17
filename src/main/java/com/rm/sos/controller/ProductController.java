package com.rm.sos.controller;

import com.rm.sos.model.Product;
import com.rm.sos.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @PostMapping("/init")
    public ResponseEntity<String> init() throws IOException {
        service.createIndex();
        return ResponseEntity.ok("Product Index ready");
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody Product product) throws IOException {
        service.upsertProduct(product);
        return ResponseEntity.ok("Indexed: " + product.getId());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<String> patch(@PathVariable String id, @RequestBody Product product) throws IOException {
        String updateId = service.patchProduct(id, product);
        return ResponseEntity.ok("Patched product id: " + updateId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable String id) throws IOException {
        Product p = service.getProduct(id);
        return p != null ? ResponseEntity.ok(p) : ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> search(@RequestParam String name) throws IOException {
        return ResponseEntity.ok(service.searchByName(name));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id) throws IOException {
        service.deleteProduct(id);
        return ResponseEntity.ok("Deleted: " + id);
    }
}
