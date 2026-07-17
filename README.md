# SpringOpenSearch

A learner project exploring the [OpenSearch Java client](https://github.com/opensearch-project/opensearch-java) from a Spring Boot application. It covers CRUD operations and a range of search query types (match, term, range, bool, fuzzy, wildcard, geo, etc.) against two OpenSearch indices: `products` and `shipments`.

This project was built as a hands-on way to learn how Spring Boot and OpenSearch fit together — not a production-ready template.

## Tech Stack

- Java 21
- Spring Boot 4.0.6 (`spring-boot-starter-webmvc`)
- OpenSearch Java Client 2.13.0
- Lombok
- Jackson (with `jackson-datatype-jsr310` for `Instant` serialization)
- Maven (wrapper included, no local install required)

## Project Structure

```
src/main/java/com/rm/sos/
├── config/
│   └── OpenSearchConfig.java          # RestClient, OpenSearchClient, ObjectMapper beans
├── controller/
│   ├── ProductController.java         # CRUD for /api/products
│   ├── ShipmentController.java        # CRUD + bulk for /api/shipments
│   └── ShipmentSearchController.java  # Search queries for /api/shipments/search
├── service/
│   ├── ProductService.java            # Product index operations
│   ├── ShipmentService.java           # Shipment index operations
│   └── ShipmentSearchService.java     # All search query implementations
└── model/
    ├── Product.java
    ├── Shipment.java
    ├── Address.java
    └── GeoPoint.java
```

## Prerequisites

- Java 21+
- A running OpenSearch instance reachable at the host/port configured in `application.yml`

If you don't already have OpenSearch running locally, the quickest way to get one is via Docker:

```bash
docker run -p 9200:9200 -p 9600:9600 \
  -e "discovery.type=single-node" \
  -e "DISABLE_SECURITY_PLUGIN=true" \
  opensearchproject/opensearch:2.13.0
```

For a full walkthrough (including Docker install and Docker Compose setup on Linux Mint), see [docs/opensearch-docker-setup.md](docs/opensearch-docker-setup.md).

## Configuration

`src/main/resources/application.yml`:

```yaml
opensearch:
  host: localhost
  port: 9200
  scheme: http
```

`OpenSearchConfig` reads `opensearch.host` and `opensearch.port` via `@Value` and builds the `RestClient` and `OpenSearchClient` beans. `ObjectMapper` is also declared as a bean here (required by `ProductService`).

## Running the App

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`.

## Getting Started

Before hitting any endpoint, create the indices:

```bash
curl -X POST http://localhost:8080/api/products/init
curl -X POST http://localhost:8080/api/shipments/init
```

## API Endpoints

### Products (`/api/products`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/products/init` | Create the products index |
| POST | `/api/products` | Index a product |
| GET | `/api/products/{id}` | Get product by ID |
| PATCH | `/api/products/{id}` | Partial update |
| GET | `/api/products/search?name=` | Full-text search by name |
| DELETE | `/api/products/{id}` | Delete a product |

### Shipments (`/api/shipments`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/shipments/init` | Create the shipments index |
| POST | `/api/shipments` | Index a shipment |
| GET | `/api/shipments/{id}` | Get shipment by ID |
| GET | `/api/shipments` | Get all shipments |
| PUT | `/api/shipments/{id}` | Full update |
| DELETE | `/api/shipments/{id}` | Delete a shipment |
| POST | `/api/shipments/bulk` | Bulk index shipments |

### Shipment Search (`/api/shipments/search`)

| Endpoint | Query Params | Search Type |
|----------|-------------|-------------|
| `/match` | `field`, `text` | Full-text match |
| `/match-phrase` | `field`, `phrase` | Exact phrase |
| `/match-phrase-slop` | `field`, `phrase`, `slop` | Phrase with slop |
| `/term` | `field`, `value` | Exact keyword match |
| `/terms` | `field`, `values` | Multiple exact values |
| `/range` | `field`, `gte`, `lte` | Numeric range |
| `/match-all` | — | Return all documents |
| `/multi-match` | `text`, `fields` | Search across multiple fields |
| `/bool` | `status`, `carrier`, `notes` | Bool query (must + should) |
| `/prefix` | `field`, `prefix` | Prefix match |
| `/wildcard` | `field`, `pattern` | Wildcard pattern |
| `/exists` | `field` | Field existence check |
| `/fuzzy` | `field`, `value` | Fuzzy match |

### Shipment Aggregations (`/api/shipments/aggregations`)

| Endpoint | Query Params | Description |
|----------|-------------|-------------|
| `/count-by` | `field` | Group by any keyword field (count per value) |
| `/value-count` | `field` | Count non-null values for a field |
| `/stats` | `field` | Count/min/max/avg/sum in one shot |
| `/metrics` | `field` | Individual avg/sum/min/max |
| `/range` | — | Group into value ranges |
| `/date-histogram` | `interval` (default `Day`) | Group by time interval (Day/Week/Month) |
| `/avg-value-by-carrier` | — | Avg declared value per carrier (nested aggregation) |
| `/filtered` | `status` | Aggregation scoped to a status filter |

### Shipment Geo (`/api/shipments/geo`)

| Endpoint | Query Params | Description |
|----------|-------------|-------------|
| `/distance` | `field`, `lat`, `lon`, `distanceKm` | Shipments within X km of a point |
| `/bounding-box` | `field`, `topLeftLat`, `topLeftLon`, `bottomRightLat`, `bottomRightLon` | Shipments within a bounding box |
| `/sorted-by-distance` | `field`, `lat`, `lon`, `status` | Shipments by status, sorted nearest first |
| `/distance-rings` | `field`, `lat`, `lon` | Group shipments into distance rings from a point |

## Postman Collection

A ready-to-import Postman collection with every endpoint (grouped into folders per controller) is available at [docs/SpringOpenSearch.postman_collection.json](docs/SpringOpenSearch.postman_collection.json). In Postman: **Import** → select the file. It uses a `baseUrl` collection variable (defaults to `http://localhost:8080`).

## Example Requests

Create a product:

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Wireless Mouse", "category": "Electronics", "price": 25.99}'
```

Create a shipment:

```bash
curl -X POST http://localhost:8080/api/shipments \
  -H "Content-Type: application/json" \
  -d '{
    "tracking_number": "TRK123456",
    "status": "IN_TRANSIT",
    "carrier": "FedEx",
    "origin_address": {"city": "New York", "country": "US"},
    "destination_address": {"city": "Los Angeles", "country": "US"},
    "weight_kg": 2.5
  }'
```

Search shipments by carrier (match query):

```bash
curl "http://localhost:8080/api/shipments/search/match?field=carrier&text=FedEx"
```

## Production Considerations

This is a learner project, and several things have been intentionally simplified or left out to keep the focus on the OpenSearch client. Before using any of this in a real deployment, consider addressing:

- **OpenSearch security is disabled.** `DISABLE_SECURITY_PLUGIN=true` and plain HTTP (`scheme: http`) are used throughout for local-dev simplicity. In production, enable the security plugin, use HTTPS/TLS, and authenticate the client with real credentials (not hardcoded in `application.yml`).
- **No authentication/authorization on the API itself.** There's no Spring Security in place, so every CRUD endpoint — including deletes — is open to anyone who can reach the app.
- **No input validation.** Request bodies (`Product`, `Shipment`) aren't validated (no `@Valid`/bean validation constraints), so malformed or incomplete payloads can be indexed as-is.
- **No centralized error handling.** Service methods throw raw `IOException`s; there's no `@ControllerAdvice` translating OpenSearch/client errors into meaningful HTTP status codes and error bodies.
- **No pagination.** `GET /api/shipments` and search endpoints return full result sets rather than paging through hits, which won't scale to large indices.
- **Config is not environment-aware.** Host/port/scheme are static values in `application.yml`; production would need profile-based config or externalized secrets (env vars, a vault, etc.) instead.
- **No rate limiting or CORS configuration.**
- **Minimal test coverage.** Only the default context-load test is present — no unit or integration tests around the service/controller logic.
- **Default index settings.** Index creation doesn't tune shard/replica counts, refresh intervals, or mappings for production data volumes.

## License

This project is for personal learning purposes.
