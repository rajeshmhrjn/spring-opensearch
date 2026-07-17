package com.rm.sos.service;


import com.rm.sos.model.Shipment;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShipmentSearchService {

    private static final String INDEX = "shipments";
    private final OpenSearchClient client;

    public ShipmentSearchService(OpenSearchClient client) {
        this.client = client;
    }

    private List<Shipment> extractHits(SearchResponse<Shipment> response) {
        return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // 1. MATCH
    // Analyzed full-text search. The query text is tokenized and matched
    // against tokenized field values. Order doesn't matter.
    // e.g. "glassware care" will match "Handle with care, contains glassware"
    // -------------------------------------------------------------------------
    public List<Shipment> matchSearch(String field, String text) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .match(m -> m
                                        .field(field)
                                        .query(FieldValue.of(text))
                                )
                        ),
                Shipment.class
        );
        return extractHits(response);
    }

    // -------------------------------------------------------------------------
    // 2. MATCH PHRASE
    // The query text must appear as an exact phrase in the same order.
    // e.g. "contains glassware" matches but "glassware contains" does NOT
    // -------------------------------------------------------------------------
    public List<Shipment> matchPhraseSearch(String field, String phrase) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .matchPhrase(m -> m
                                        .field(field)
                                        .query(phrase)
                                )
                        ),
                Shipment.class
        );
        return extractHits(response);
    }

    // -------------------------------------------------------------------------
    // 3. MATCH PHRASE WITH SLOP
    // Like match_phrase but allows words to be N positions apart.
    // slop=1: "care glassware" can match "care, contains glassware" (1 word between)
    // slop=2: allows up to 2 words between the phrase terms
    // -------------------------------------------------------------------------
    public List<Shipment> matchPhraseWithSlopSearch(String field, String phrase, int slop) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .matchPhrase(m -> m
                                        .field(field)
                                        .query(phrase)
                                        .slop(slop)
                                )
                        ),
                Shipment.class
        );
        return extractHits(response);
    }

    // -------------------------------------------------------------------------
    // 4. TERM
    // Exact match on a keyword field. NOT analyzed — case sensitive.
    // Use for status, carrier, priority, IDs — never on text fields.
    // e.g. term on status="IN_TRANSIT" works; "in_transit" will NOT match
    // -------------------------------------------------------------------------
    public List<Shipment> termSearch(String field, String value) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .term(t -> t
                                        .field(field)
                                        .value(FieldValue.of(value))
                                )
                        ),
                Shipment.class
        );
        return extractHits(response);
    }

    // -------------------------------------------------------------------------
    // 5. TERMS
    // Exact match against multiple values — like SQL IN (...)
    // e.g. status IN ("IN_TRANSIT", "OUT_FOR_DELIVERY")
    // -------------------------------------------------------------------------
    public List<Shipment> termsSearch(String field, List<String> values) throws IOException {
        List<org.opensearch.client.opensearch._types.FieldValue> fieldValues = values.stream()
                .map(org.opensearch.client.opensearch._types.FieldValue::of)
                .collect(Collectors.toList());

        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .terms(t -> t
                                        .field(field)
                                        .terms(tv -> tv.value(fieldValues))
                                )
                        ),
                Shipment.class
        );
        return extractHits(response);
    }

    // -------------------------------------------------------------------------
    // 6. RANGE
    // Matches documents where a field value falls within a range.
    // Works on numbers, dates, and keyword fields.
    // e.g. declared_value between 500 and 3000
    // -------------------------------------------------------------------------

    public List<Shipment> rangeSearch(String field, double gte, double lte) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .range(r -> r
                                        .field(field)
                                        .gte(JsonData.of(gte))
                                        .lte(JsonData.of(lte))
                                )
                        ),
                Shipment.class
        );
        return extractHits(response);
    }

    // -------------------------------------------------------------------------
    // 7. MATCH ALL
    // Returns all documents. Equivalent to SELECT * FROM table.
    // Useful as a base query combined with filters.
    // -------------------------------------------------------------------------
    public List<Shipment> matchAllSearch() throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .matchAll(m -> m)
                        ),
                Shipment.class
        );
        return extractHits(response);
    }

    // -------------------------------------------------------------------------
    // 8. MULTI MATCH
    // Runs the same match query across multiple fields at once.
    // e.g. search "Sydney" in both sender_name and notes simultaneously
    // -------------------------------------------------------------------------
    public List<Shipment> multiMatchSearch(String text, List<String> fields) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .multiMatch(m -> m
                                        .fields(fields)
                                        .query(text)
                                )
                        ),
                Shipment.class
        );
        return extractHits(response);
    }

    // -------------------------------------------------------------------------
    // 9. BOOL QUERY
    // Combines multiple queries with logical operators:
    //   must    = AND (affects score)
    //   filter  = AND (no scoring, faster — use for status/carrier filters)
    //   should  = OR  (at least one should match, boosts score)
    //   must_not = NOT
    // e.g. status=IN_TRANSIT AND carrier=FEDEX AND notes contains "medical"
    // -------------------------------------------------------------------------
    public List<Shipment> boolSearch(String status, String carrier, String notes) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .bool(b -> b
                                        .filter(f -> f                          // exact match, no scoring
                                                .term(t -> t
                                                        .field("status")
                                                        .value(FieldValue.of(status))
                                                )
                                        )
                                        .filter(f -> f                          // exact match, no scoring
                                                .term(t -> t
                                                        .field("carrier")
                                                        .value(FieldValue.of(carrier))
                                                )
                                        )
                                        .must(m -> m                            // full-text, affects score
                                                .match(mt -> mt
                                                        .field("notes")
                                                        .query(FieldValue.of(notes))
                                                )
                                        )
                                )
                        ),
                Shipment.class
        );
        return extractHits(response);
    }

    // -------------------------------------------------------------------------
    // 10. PREFIX
    // Matches documents where the field value starts with the given prefix.
    // Works on keyword fields. e.g. tracking_number starting with "TRK-2024"
    // -------------------------------------------------------------------------
    public List<Shipment> prefixSearch(String field, String prefix) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .prefix(p -> p
                                        .field(field)
                                        .value(prefix)
                                )
                        ),
                Shipment.class
        );
        return extractHits(response);
    }

    // -------------------------------------------------------------------------
    // 11. WILDCARD
    // Pattern matching on keyword fields.
    // * = any number of characters, ? = single character
    // e.g. "TRK-2024-AU-000*" matches all tracking numbers starting with that
    // WARNING: leading wildcards (*something) are very slow — avoid in prod
    // -------------------------------------------------------------------------
    public List<Shipment> wildcardSearch(String field, String pattern) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .wildcard(w -> w
                                        .field(field)
                                        .value(pattern)
                                )
                        ),
                Shipment.class
        );
        return extractHits(response);
    }

    // -------------------------------------------------------------------------
    // 12. EXISTS
    // Matches documents where a field has any non-null value.
    // e.g. find all shipments that have an actual_delivery date (i.e. delivered)
    // -------------------------------------------------------------------------
    public List<Shipment> existsSearch(String field) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .exists(e -> e
                                        .field(field)
                                )
                        ),
                Shipment.class
        );
        return extractHits(response);
    }

    // -------------------------------------------------------------------------
    // 13. FUZZY
    // Matches documents with terms similar to the search term.
    // Accounts for typos using edit distance (Levenshtein).
    // fuzziness=AUTO: 0 edits for 1-2 chars, 1 edit for 3-5, 2 edits for 6+
    // e.g. "FEDX" will still match "FEDEX"
    // -------------------------------------------------------------------------
    public List<Shipment> fuzzySearch(String field, String value) throws IOException {
        SearchResponse<Shipment> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .fuzzy(f -> f
                                        .field(field)
                                        .value(FieldValue.of(value))
                                        .fuzziness("AUTO")
                                )
                        ),
                Shipment.class
        );
        return extractHits(response);
    }
}
