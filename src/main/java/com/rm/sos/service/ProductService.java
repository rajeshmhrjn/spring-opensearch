package com.rm.sos.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rm.sos.model.Product;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.UpdateResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final String INDEX = "products";
    private final OpenSearchClient client;
    private final ObjectMapper objectMapper;

    public ProductService(OpenSearchClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    // 1. Create index (run once)
    public void createIndex() throws IOException {
        boolean exists = client.indices().exists(e -> e.index(INDEX)).value();
        if (!exists) {
            client.indices().create(c -> c
                    .index(INDEX)
                    .mappings(m -> m
                            .properties("name",     p -> p.text(t -> t))
                            .properties("category", p -> p.keyword(k -> k))
                            .properties("price",    p -> p.double_(d -> d))
                    )
            );
            System.out.println("Index '" + INDEX + "' created.");
        }
        else{
            System.out.println("Index '" + INDEX + "' already exists.");
        }
    }

    // 2. Index (insert/update) a document
    public void upsertProduct(Product product) throws IOException {
        IndexResponse response = client.index(i -> i.index(INDEX)
                                                                    .id(product.getId())
                                                                    .document(product)
                                        );
        System.out.println("Indexed: " + response.id() + " result=" + response.result());
    }

    /*****
     *
     * @param id
     * @param product
     * @return
     * @throws IOException
     */
    public String patchProduct(String id, Product product) throws IOException {
        Map<String, Object> fields = objectMapper.convertValue(product, new TypeReference<>() {});
        fields.values().removeIf(Objects::isNull);
        fields.remove("id");

        if (!fields.isEmpty()) {
            UpdateResponse<Product> updateResponse = client.update(u -> u
                            .index(INDEX)
                            .id(id)
                            .doc(fields),
                    Product.class
            );
            return updateResponse.id();
        }
        return "EMPTY";
    }

    // 3. Get a document by ID
    public Product getProduct(String id) throws IOException {
        GetResponse<Product> response = client.get(g -> g
                        .index(INDEX)
                        .id(id),
                Product.class
        );
        return response.found() ? response.source() : null;
    }

    // 4. Search by name (full-text match)
    public List<Product> searchByName(String name) throws IOException {
        SearchResponse<Product> response = client.search(s -> s
                        .index(INDEX)
                        .query(q -> q
                                .match(m -> m
                                        .field("name")
                                        .query(FieldValue.of(name))
                                )
                        ),
                Product.class
        );
        return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    // 5. Delete a document
    public void deleteProduct(String id) throws IOException {
        client.delete(d -> d.index(INDEX).id(id));
    }

    // 6. Delete the index entirely
    public void deleteIndex() throws IOException {
        client.indices().delete(d -> d.index(INDEX));
    }
}