# Kiosk Orders API

Backend for restaurant kiosks: products, orders and order status flow for the kitchen.

Java 21, Spring Boot 3.3, Spring Data JPA (Hibernate), SQLite, Flyway.

## Running

Requirements: JDK 21. Maven is not needed, the wrapper is included.

```bash
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`.

The API starts on `http://localhost:8080`. The database file `kiosk.db` is created in the working directory on first start,
and Flyway creates the schema and seed data. To start from scratch just delete `kiosk.db`.
`PORT` and `KIOSK_DB_PATH` environment variables can be used to change the port and the database file.

Tests:

```bash
./mvnw test
```

### Seed data

| id | Restaurant      | Products                                   |
|----|-----------------|--------------------------------------------|
| 1  | Maitre Downtown | Classic Burger, Cheese Burger, French Fries, Cola |
| 2  | Maitre Airport  | Chicken Wrap, Orange Juice                 |
| 3  | Maitre Seaside  | none                                       |

## API

Everything is scoped by restaurant: `/api/restaurants/{restaurantId}/...`

| # | Operation                | Request                                                    |
|---|--------------------------|------------------------------------------------------------|
| 1 | Create product           | `POST /api/restaurants/{rid}/products`                     |
| 2 | Update product           | `PATCH /api/restaurants/{rid}/products/{productId}`        |
| 3 | Create order             | `POST /api/restaurants/{rid}/orders`                       |
| 4 | Change order status      | `PATCH /api/restaurants/{rid}/orders/{orderId}/status`     |
| 5 | List orders              | `GET /api/restaurants/{rid}/orders?status=&page=0&size=20` |
| 6 | Top 5 best sellers       | `GET /api/restaurants/{rid}/products/top-selling`          |

### Examples

```bash
curl -X POST localhost:8080/api/restaurants/1/products \
  -H 'Content-Type: application/json' \
  -d '{"name": "Ice Cream", "category": "Desserts", "price": 2.50}'
```

```bash
curl -X PATCH localhost:8080/api/restaurants/1/products/1 \
  -H 'Content-Type: application/json' \
  -d '{"price": 9.40, "available": false}'
```

Both fields are optional, but at least one is required.

```bash
curl -X POST localhost:8080/api/restaurants/1/orders \
  -H 'Content-Type: application/json' \
  -d '{"items": [{"productId": 1, "quantity": 2}, {"productId": 3, "quantity": 1}]}'
```

Response (`201 Created`):

```json
{
  "id": 1,
  "restaurantId": 1,
  "status": "PENDING",
  "lines": [
    {"productId": 1, "productName": "Classic Burger", "quantity": 2, "unitPrice": 8.90, "lineTotal": 17.80},
    {"productId": 3, "productName": "French Fries", "quantity": 1, "unitPrice": 3.50, "lineTotal": 3.50}
  ],
  "total": 21.30,
  "createdAt": "2026-10-31T19:22:45.177Z",
  "updatedAt": "2026-10-31T19:22:45.177Z"
}
```

```bash
curl -X PATCH localhost:8080/api/restaurants/1/orders/1/status \
  -H 'Content-Type: application/json' \
  -d '{"status": "Preparing"}'
```

```bash
curl 'localhost:8080/api/restaurants/1/orders?status=pending&page=0&size=10'
```

The list response is `{"items": [...], "page": 0, "size": 10, "totalItems": 1, "totalPages": 1}`, each item has the same shape as above.

```bash
curl localhost:8080/api/restaurants/1/products/top-selling
```

```json
[{"productId": 1, "name": "Classic Burger", "category": "Burgers", "quantitySold": 2}]
```

### Errors

Errors are returned as RFC 7807 problem details (`application/problem+json`):

| Status | When                                                                                   |
|--------|----------------------------------------------------------------------------------------|
| 400    | Invalid body or parameters (validation errors are listed in `errors`)                   |
| 404    | Restaurant, product or order does not exist, or belongs to a different restaurant       |
| 409    | Status transition not allowed, duplicate product name, concurrent modification          |
| 422    | Order references a product that is unknown in this restaurant or is not on sale         |

## Main decisions

- **Layering.** Controller, service, repository, split by feature (`product`, `order`, `restaurant`) rather than by layer.
  Business rules live in the entities: `OrderStatus.canTransitionTo` holds the whole state machine, `CustomerOrder`
  computes the totals. The services only orchestrate and the controllers only map HTTP.
- **Money as integer cents.** SQLite has no exact decimal type, a `BigDecimal` column would be stored as a floating
  point number. A JPA converter maps `BigDecimal` (scale 2) to cents, so the domain code keeps working with
  `BigDecimal` and the database stays exact. Timestamps are stored as epoch milliseconds for the same reason.
- **Price and name are copied into the order line.** An order is a historical record. If the price of a product changes
  later, old orders must still show what the customer paid. Line total and order total are also stored, so listing
  never recomputes them.
- **Flyway owns the schema** (`ddl-auto: none`). Schema has `CHECK` constraints and foreign keys, and indexes for the
  queries that are actually run: `(restaurant_id, status, created_at)` and `(restaurant_id, created_at)` for listing,
  `order_lines(order_id)` and `order_lines(product_id)` for loading lines and the top-5 aggregation.
- **Listing without N+1 and without in-memory paging.** It is done with two queries: a page of order ids
  (with a count query), then the orders with their lines in a single `join fetch`. Fetch-joining a collection directly
  together with `LIMIT/OFFSET` makes Hibernate load everything and paginate in memory.
- **Top 5 is a single aggregate query** (`SUM(quantity) ... GROUP BY ... ORDER BY ... LIMIT 5`), the database does the work.
- **Optimistic locking on orders** (`@Version`). If two clients change the status of the same order at the same time,
  one of them gets `409` instead of silently overwriting the other.
- **Injected `Clock`** so time is not read from static calls and can be controlled.
- **SQLite** in WAL mode with a busy timeout, which is fine for a task like this. Moving to PostgreSQL would mean
  changing the driver, the dialect and a couple of SQLite specific bits in the migration.

## Decisions on open points

- **URLs are nested under the restaurant.** A product or order from another restaurant returns `404`, so one restaurant
  can never read or change another one's data, even with a valid id.
- **Product name is unique per restaurant**, case-insensitive (`409` on duplicates). Two "Cola" products in the
  same menu would be confusing on a kiosk. Name and category cannot be changed, the task only asks for price and availability.
- **Price** must be greater than 0 and have at most 2 decimals.
- **Order validation:** at least 1 and at most 50 lines, quantity 1..99 per line. If the same product appears more than once,
  the quantities are merged into a single line (and the merged quantity must still be at most 99).
  The order is rejected as a whole (`422`) if any product is unknown in this restaurant or is off sale; the error lists the ids.
- **Status values** are accepted in any case (`Pending`, `pending`, `PENDING`) and returned in upper case.
  Setting the same status again (e.g. `Pending → Pending`) is treated as an invalid transition (`409`).
  `Completed` and `Cancelled` are final.
- **Taking a product off sale** does not affect orders that were already placed, it only blocks new ones.
- **Order list** is sorted newest first. `page` is zero based, `size` defaults to 20 and is limited to 100.
- **Top 5** counts all non-cancelled orders, including `Pending` ones, because those are sales that have been made;
  they stop counting only if the order is cancelled. Products that are currently off sale are still counted,
  since they were sold. Ties are broken by product id so the result is stable. Fewer than 5 items are returned if fewer products were sold.
  Name and category come from the current product.
- **Restaurants** only exist as seed data, there are no endpoints for them.

## Tests

- `OrderStatusTest` covers every allowed and forbidden transition.
- `ProductApiTest`, `OrderApiTest`, `TopSellingApiTest` are integration tests through HTTP (MockMvc) against a real SQLite
  database in a temp file, with the same Flyway migrations as production.

## Time spent

About 5 hours.

## What I would do with more time

- `GET` endpoints for a single order and for the product list (the kiosk needs the menu).
- Idempotency key on order creation, so a kiosk that retries after a network error does not create a duplicate order.
- Status change history table (who/when), useful for the kitchen screen and for reports.
- Cursor (keyset) pagination instead of offset for large order lists.
- PostgreSQL with Testcontainers, and a Dockerfile / docker-compose.
- OpenAPI (springdoc) documentation.
- Date range filter for the top-5 report (e.g. today, this week), since all-time best sellers are rarely what a restaurant asks for.
