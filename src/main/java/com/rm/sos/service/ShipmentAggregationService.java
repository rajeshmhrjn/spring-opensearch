package com.rm.sos.service;

import com.rm.sos.model.Shipment;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.aggregations.*;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class ShipmentAggregationService {

    private static final String INDEX = "shipments";
    private final OpenSearchClient client;

    public ShipmentAggregationService(OpenSearchClient client) {
        this.client = client;
    }

    // -------------------------------------------------------------------------
    // 1. TERMS AGGREGATION
    // Groups documents by a keyword field and counts each group.
    // Like SQL: SELECT status, COUNT(*) FROM shipments GROUP BY status
    // -------------------------------------------------------------------------
    public Map<String, Long> countByField(String field) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .size(0)                          // we only want agg results, not documents
                        .aggregations("group_by", a -> a
                                .terms(t -> t
                                        .field(field)
                                        .size(20)                 // max number of buckets to return
                                )
                        ),
                Shipment.class
        );

        Map<String, Long> result = new LinkedHashMap<>();
        response.aggregations()
                .get("group_by")
                .sterms()                         // string terms aggregation
                .buckets()
                .array()
                .forEach(bucket -> result.put(bucket.key(), bucket.docCount()));

        return result;
    }

    // -------------------------------------------------------------------------
    // 2. VALUE COUNT AGGREGATION
    // Counts the number of documents that have a value for a field.
    // Like SQL: SELECT COUNT(actual_delivery) FROM shipments
    // Useful to count non-null fields e.g. how many shipments are delivered
    // -------------------------------------------------------------------------
    public long valueCount(String field) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .size(0)
                        .aggregations("value_count", a -> a
                                .valueCount(v -> v
                                        .field(field)
                                )
                        ),
                Shipment.class
        );

        return (long) response.aggregations()
                .get("value_count")
                .valueCount()
                .value();
    }

    // -------------------------------------------------------------------------
    // 3. STATS AGGREGATION
    // Returns count, min, max, avg, sum for a numeric field in one query.
    // Like SQL: SELECT COUNT(*), MIN(declared_value), MAX(declared_value),
    //                  AVG(declared_value), SUM(declared_value) FROM shipments
    // -------------------------------------------------------------------------
    public Map<String, Double> stats(String field) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .size(0)
                        .aggregations("stats", a -> a
                                .stats(st -> st
                                        .field(field)
                                )
                        ),
                Shipment.class
        );

        StatsAggregate stats = response.aggregations()
                .get("stats")
                .stats();

        Map<String, Double> result = new LinkedHashMap<>();
        result.put("count", (double) stats.count());
        result.put("min",   stats.min());
        result.put("max",   stats.max());
        result.put("avg",   stats.avg());
        result.put("sum",   stats.sum());

        return result;
    }

    // -------------------------------------------------------------------------
    // 4. AVG / SUM / MIN / MAX AGGREGATIONS
    // Individual metric aggregations on a numeric field.
    // Like SQL: SELECT AVG(declared_value) FROM shipments
    // -------------------------------------------------------------------------
    public Map<String, Double> metrics(String field) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .size(0)
                        .aggregations("avg_value", a -> a
                                .avg(v -> v.field(field))
                        )
                        .aggregations("sum_value", a -> a
                                .sum(v -> v.field(field))
                        )
                        .aggregations("min_value", a -> a
                                .min(v -> v.field(field))
                        )
                        .aggregations("max_value", a -> a
                                .max(v -> v.field(field))
                        ),
                Shipment.class
        );

        Map<String, Double> result = new LinkedHashMap<>();
        result.put("avg", response.aggregations().get("avg_value").avg().value());
        result.put("sum", response.aggregations().get("sum_value").sum().value());
        result.put("min", response.aggregations().get("min_value").min().value());
        result.put("max", response.aggregations().get("max_value").max().value());

        return result;
    }

    // -------------------------------------------------------------------------
    // 5. RANGE AGGREGATION
    // Groups documents into user-defined numeric ranges (buckets).
    // Like SQL: SELECT
    //   SUM(CASE WHEN declared_value < 500 THEN 1 ELSE 0 END) as low,
    //   SUM(CASE WHEN declared_value BETWEEN 500 AND 2000 THEN 1 ELSE 0 END) as mid
    // -------------------------------------------------------------------------
    public Map<String, Long> rangeAggregation() throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .size(0)
                        .aggregations("value_ranges", a -> a
                                .range(r -> r
                                        .field("declared_value")
                                        .ranges(rb -> rb.to("500"))               // < 500
                                        .ranges(rb -> rb.from("500").to("2000"))  // 500 - 2000
                                        .ranges(rb -> rb.from("2000").to("5000")) // 2000 - 5000
                                        .ranges(rb -> rb.from("5000"))            // > 5000
                                )
                        ),
                Shipment.class
        );

        Map<String, Long> result = new LinkedHashMap<>();
        response.aggregations()
                .get("value_ranges")
                .range()
                .buckets()
                .array()
                .forEach(bucket -> result.put(bucket.key(), bucket.docCount()));

        return result;
    }

    // -------------------------------------------------------------------------
    // 6. DATE HISTOGRAM AGGREGATION
    // Groups documents by a date field into time-based buckets.
    // Like SQL: SELECT DATE_TRUNC('day', created_at), COUNT(*)
    //           FROM shipments GROUP BY 1
    // -------------------------------------------------------------------------
    public Map<String, Long> dateHistogram(String interval) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .size(0)
                        .aggregations("shipments_over_time", a -> a
                                .dateHistogram(d -> d
                                        .field("created_at")
                                        .calendarInterval(CalendarInterval.valueOf(interval)) // DAY, WEEK, MONTH
                                        .format("yyyy-MM-dd")
                                        .minDocCount(1)           // skip empty buckets
                                )
                        ),
                Shipment.class
        );

        Map<String, Long> result = new LinkedHashMap<>();
        response.aggregations()
                .get("shipments_over_time")
                .dateHistogram()
                .buckets()
                .array()
                .forEach(bucket -> result.put(bucket.keyAsString(), bucket.docCount()));

        return result;
    }

    // -------------------------------------------------------------------------
    // 7. NESTED (SUB) AGGREGATION
    // Aggregations inside aggregations.
    // Like SQL: SELECT carrier, AVG(declared_value)
    //           FROM shipments GROUP BY carrier
    // Groups by carrier, then calculates avg declared_value per carrier
    // -------------------------------------------------------------------------
    public Map<String, Double> avgValueByCarrier() throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .size(0)
                        .aggregations("by_carrier", a -> a
                                .terms(t -> t
                                        .field("carrier")
                                        .size(20)
                                )
                                .aggregations("avg_value", sa -> sa    // sub-aggregation
                                        .avg(avg -> avg
                                                .field("declared_value")
                                        )
                                )
                        ),
                Shipment.class
        );

        Map<String, Double> result = new LinkedHashMap<>();
        response.aggregations()
                .get("by_carrier")
                .sterms()
                .buckets()
                .array()
                .forEach(bucket -> result.put(
                        bucket.key(),
                        bucket.aggregations().get("avg_value").avg().value()
                ));

        return result;
    }

    // -------------------------------------------------------------------------
    // 8. FILTERED AGGREGATION
    // Aggregation scoped to a subset of documents using a filter.
    // Like SQL: SELECT AVG(declared_value) FROM shipments WHERE status='DELIVERED'
    // -------------------------------------------------------------------------
    public Map<String, Object> filteredAggregation(String status) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .size(0)
                        .aggregations("filtered_shipments", a -> a
                                .filter(f -> f
                                        .term(t -> t
                                                .field("status")
                                                .value(FieldValue.of(status))
                                        )
                                )
                                .aggregations("avg_value", sa -> sa
                                        .avg(avg -> avg.field("declared_value"))
                                )
                                .aggregations("total_weight", sa -> sa
                                        .sum(sum -> sum.field("weight_kg"))
                                )
                        ),
                Shipment.class
        );

        Aggregate filtered = response.aggregations().get("filtered_shipments");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("doc_count", filtered.filter().docCount());
        result.put("avg_declared_value", filtered.filter().aggregations()
                .get("avg_value").avg().value());
        result.put("total_weight_kg", filtered.filter().aggregations()
                .get("total_weight").sum().value());

        return result;
    }
}
