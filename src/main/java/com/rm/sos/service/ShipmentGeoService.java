package com.rm.sos.service;

import com.rm.sos.model.Shipment;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.DistanceUnit;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.GeoLocation;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipmentGeoService {
    private static final String INDEX = "shipments";
    private final OpenSearchClient client;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private List<Shipment> extractHits(SearchResponse<Shipment> response) {
        return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    private GeoLocation geoLocation(double lat, double lon) {
        return GeoLocation.of(g -> g
                .latlon(ll -> ll
                        .lat(lat)
                        .lon(lon)
                )
        );
    }

    // -------------------------------------------------------------------------
    // 1. GEO DISTANCE
    // Finds documents within a radius (circle) around a central point.
    // Most common geo query in logistics — "find all shipments within 500km of Sydney"
    // -------------------------------------------------------------------------
    public List<Shipment> findWithinDistance(
            String field, double lat, double lon, double distanceKm) throws IOException {

        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .geoDistance(g -> g
                                        .field(field)
                                        .location(geoLocation(lat, lon))
                                        .distance(distanceKm + "km")
                                )
                        ),
                Shipment.class
        );
        return extractHits(response);
    }

    // -------------------------------------------------------------------------
    // 2. GEO BOUNDING BOX
    // Finds documents within a rectangle defined by top-left and bottom-right corners.
    // Faster than geo_distance — used when you want to search within a map viewport.
    // e.g. find all shipments currently located within eastern Australia
    // -------------------------------------------------------------------------
    public List<Shipment> findWithinBoundingBox(
            String field,
            double topLeftLat, double topLeftLon,
            double bottomRightLat, double bottomRightLon) throws IOException {

        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .geoBoundingBox(g -> g
                                        .field(field)
                                        .boundingBox(bb -> bb
                                                .tlbr(tlbr -> tlbr
                                                        .topLeft(geoLocation(topLeftLat, topLeftLon))
                                                        .bottomRight(geoLocation(bottomRightLat, bottomRightLon))
                                                )
                                        )
                                )
                        ),
                Shipment.class
        );
        return extractHits(response);
    }

    // -------------------------------------------------------------------------
    // 3. GEO DISTANCE SORTING
    // Returns shipments sorted by distance from a given point, nearest first.
    // e.g. "show me all IN_TRANSIT shipments sorted by proximity to Melbourne"
    // Combines a bool filter with geo sort
    // -------------------------------------------------------------------------
    public List<Shipment> findSortedByDistance(
            String field, double lat, double lon, String status) throws IOException {

        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .bool(b -> b
                                        .filter(f -> f
                                                .term(t -> t
                                                        .field("status")
                                                        .value(FieldValue.of(status))
                                                )
                                        )
                                )
                        )
                        .sort(so -> so
                                .geoDistance(gd -> gd
                                        .field(field)
                                        .location(geoLocation(lat, lon))
                                        .order(SortOrder.Asc)
                                        .unit(DistanceUnit.Kilometers)
                                )
                        ),
                Shipment.class
        );
        return extractHits(response);
    }

    // -------------------------------------------------------------------------
    // 4. GEO DISTANCE AGGREGATION
    // Groups shipments into distance-based rings (buckets) from a central point.
    // Like range aggregation but for geo distance.
    // e.g. how many shipments are within 0-500km, 500-1000km, 1000km+ of Sydney
    // -------------------------------------------------------------------------
    public Map<String, Long> aggregateByDistance(
            String field, double lat, double lon) throws IOException {

        Request request = getRequest(field, lat, lon);

        Response response = restClient.performRequest(request);

        JsonNode root = objectMapper.readTree(response.getEntity().getContent());
        JsonNode buckets = root
                .path("aggregations")
                .path("distance_rings")
                .path("buckets");

        Map<String, Long> result = new LinkedHashMap<>();
        for (JsonNode bucket : buckets) {
            result.put(
                    bucket.path("key").asText(),
                    bucket.path("doc_count").asLong()
            );
        }

        return result;
    }

    private static @NonNull Request getRequest(String field, double lat, double lon) {
        String body = String.format("""
            {
              "size": 0,
              "aggs": {
                "distance_rings": {
                  "geo_distance": {
                    "field": "%s",
                    "origin": { "lat": %s, "lon": %s },
                    "unit": "km",
                    "ranges": [
                      { "key": "within 500km",    "to": 500 },
                      { "key": "500km to 1000km", "from": 500, "to": 1000 },
                      { "key": "beyond 1000km",   "from": 1000 }
                    ]
                  }
                }
              }
            }
            """, field, lat, lon);

        Request request = new Request("GET", "/" + INDEX + "/_search");
        request.setJsonEntity(body);
        return request;
    }
}