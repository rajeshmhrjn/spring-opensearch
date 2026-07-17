package com.rm.sos.service;

import com.rm.sos.model.Shipment;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShipmentService {

    private static final String INDEX = "shipments";
    private final OpenSearchClient client;

    public ShipmentService(OpenSearchClient client) {
        this.client = client;
    }

    public void createIndex() throws IOException {
        boolean exists = client.indices()
                .exists(e -> e.index(INDEX))
                .value();
        if (!exists) {
            client.indices().create(c -> c
                    .index(INDEX)
                    .mappings(m -> m
                            .properties("tracking_number",     p -> p.keyword(k -> k))
                            .properties("reference_number",    p -> p.keyword(k -> k))
                            .properties("status",              p -> p.keyword(k -> k))
                            .properties("carrier",             p -> p.keyword(k -> k))
                            .properties("service",             p -> p.keyword(k -> k))
                            .properties("priority",            p -> p.keyword(k -> k))
                            .properties("sender_name",         p -> p.text(t -> t
                                    .fields("keyword", f -> f.keyword(k -> k))))
                            .properties("recipient_name",      p -> p.text(t -> t
                                    .fields("keyword", f -> f.keyword(k -> k))))
                            .properties("sender_id",           p -> p.keyword(k -> k))
                            .properties("recipient_id",        p -> p.keyword(k -> k))
                            .properties("created_at",          p -> p.date(d -> d))
                            .properties("updated_at",          p -> p.date(d -> d))
                            .properties("estimated_delivery",  p -> p.date(d -> d))
                            .properties("actual_delivery",     p -> p.date(d -> d))
                            .properties("weight_kg",           p -> p.double_(d -> d))
                            .properties("volume_cbm",          p -> p.double_(d -> d))
                            .properties("declared_value",      p -> p.double_(d -> d))
                            .properties("package_count",       p -> p.integer(i -> i))
                            .properties("notes",               p -> p.text(t -> t))
                            .properties("tags",                p -> p.keyword(k -> k))
                            .properties("current_location",    p -> p.geoPoint(g -> g))
                            .properties("origin_address",      p -> p.object(o -> o
                                    .properties("city",            cp -> cp.keyword(k -> k))
                                    .properties("state",           cp -> cp.keyword(k -> k))
                                    .properties("country",         cp -> cp.keyword(k -> k))
                                    .properties("postal_code",     cp -> cp.keyword(k -> k))
                                    .properties("location",        cp -> cp.geoPoint(g -> g))
                            ))
                            .properties("destination_address", p -> p.object(o -> o
                                    .properties("city",            cp -> cp.keyword(k -> k))
                                    .properties("state",           cp -> cp.keyword(k -> k))
                                    .properties("country",         cp -> cp.keyword(k -> k))
                                    .properties("postal_code",     cp -> cp.keyword(k -> k))
                                    .properties("location",        cp -> cp.geoPoint(g -> g))
                            ))
                    )
            );
            System.out.println("Index '" + INDEX + "' created.");
        } else {
            System.out.println("Index '" + INDEX + "' already exists.");
        }
    }

    public String indexShipment(Shipment shipment) throws IOException {
        IndexResponse response = client.index(i -> i
                .index(INDEX)
                .id(shipment.getId())
                .document(shipment)
        );
        return response.id();
    }

    public Shipment getShipment(String id) throws IOException {
        GetResponse<Shipment> response = client.get(g -> g
                        .index(INDEX)
                        .id(id),
                Shipment.class
        );
        return response.found() ? response.source() : null;
    }

    public List<Shipment> getAllShipments() throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q.matchAll(m -> m)),
                Shipment.class
        );
        return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    public String updateShipment(String id, Shipment shipment) throws IOException {
        shipment.setId(id);
        IndexResponse response = client.index(i -> i
                .index(INDEX)
                .id(id)
                .document(shipment)
        );
        return response.result().toString();
    }

    public void deleteShipment(String id) throws IOException {
        client.delete(d -> d.index(INDEX).id(id));
    }

    public List<String> bulkIndexShipments(List<Shipment> shipments) throws IOException {

        BulkRequest.Builder bulkRequest = new BulkRequest.Builder();

        for (Shipment shipment : shipments) {
            bulkRequest.operations(op -> op.index(idx -> idx.index(INDEX)
                                                                                    .id(shipment.getId())
                                                                                    .document(shipment)
                                                        )
            );
        }

        BulkResponse response = client.bulk(bulkRequest.build());

        // Collect any errors
        if (response.errors()) {
            response.items().stream()
                    .filter(item -> item.error() != null)
                    .forEach(item -> System.err.println(
                            "Failed to index " + item.id() + ": " + item.error().reason()
                    ));
        }

        // Return the indexed IDs
        return response.items().stream()
                .map(BulkResponseItem::id)
                .collect(Collectors.toList());
    }
}
