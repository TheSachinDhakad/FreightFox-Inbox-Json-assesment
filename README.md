# JSON Dataset API (Spring Boot + MySQL)

A REST API that stores arbitrary JSON object records under a dataset name and supports dynamic `groupBy` and `sortBy` queries.

## Requirements
- Java 21
- Maven 3.9+
- MySQL 8+

## Database configuration

The default local configuration in `src/main/resources/application.properties` uses:

- JDBC URL: `jdbc:mysql://localhost:3306/json`
- Database username: `sachin`
- Database password: supplied through the `DB_PASSWORD` environment variable

Create the database in MySQL if it does not already exist:

```sql
CREATE DATABASE json;
```

Hibernate creates or updates the `dataset_records` table. MySQL 8 is recommended for its JSON data type support.

Set the password in your shell before starting the app. For example, in zsh/bash:

```bash
export DB_PASSWORD='your-local-mysql-password'
```

To use a different database URL or username, override the defaults:

```bash
export DB_URL='jdbc:mysql://localhost:3306/json'
export DB_USERNAME='sachin'
```

Keep database passwords in environment variables or a local, untracked configuration file; do not commit them to Git.

## 3. Run

```bash
mvn clean test
mvn spring-boot:run
```

The server runs at `http://localhost:9090`.

## Live endpoint

The Postman collection in this repository uses the following deployed API base URL:

```text
http://54.234.156.81:8080
```

For example, query the `employees` dataset with `groupBy`:

```bash
curl 'http://54.234.156.81:8080/api/dataset/employees/query?groupBy=department'
```

This address reflects the endpoint recorded in the checked-in Postman collection. If the deployment host changes, update it there and here.

## Swagger UI

The project includes Springdoc OpenAPI and Swagger UI. Once the application is running, open:

- Swagger UI: `http://localhost:9090/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:9090/v3/api-docs`

## API endpoints

### Insert record

`POST /api/dataset/{datasetName}/record`

Header: `Content-Type: application/json`

Request body:

```json
{
  "id": 1,
  "name": "John Doe",
  "age": 30,
  "department": "Engineering"
}
```

Example:

```bash
curl -X POST http://localhost:9090/api/dataset/employee_dataset/record \
  -H 'Content-Type: application/json' \
  -d '{"id":1,"name":"John Doe","age":30,"department":"Engineering"}'
```

Response (HTTP 201):

```json
{
  "message": "Record added successfully",
  "dataset": "employee_dataset",
  "recordId": 1
}
```

`recordId` is the database-generated ID, not necessarily the `id` field inside the submitted JSON.

### Group by a JSON field

`GET /api/dataset/employee_dataset/query?groupBy=department`

Response:

```json
{
  "groupedRecords": {
    "Engineering": [
      {"id":1,"name":"John Doe","age":30,"department":"Engineering"},
      {"id":2,"name":"Jane Smith","age":25,"department":"Engineering"}
    ],
    "Marketing": [
      {"id":3,"name":"Alice Brown","age":28,"department":"Marketing"}
    ]
  }
}
```

### Sort by a JSON field

`GET /api/dataset/employee_dataset/query?sortBy=age&order=asc`

Use `order=asc` or `order=desc`. If `order` is omitted, it defaults to `asc`.

Response:

```json
{
  "sortedRecords": [
    {"id":2,"name":"Jane Smith","age":25,"department":"Engineering"},
    {"id":3,"name":"Alice Brown","age":28,"department":"Marketing"},
    {"id":1,"name":"John Doe","age":30,"department":"Engineering"}
  ]
}
```

### Retrieve all records

`GET /api/dataset/employee_dataset/query`

Response:

```json
{
  "dataset": "employee_dataset",
  "records": [
    {"id":1,"name":"John Doe","age":30,"department":"Engineering"}
  ],
  "count": 1
}
```

A query for a dataset with no records returns HTTP 404. `groupBy` and `sortBy` cannot be used together in the same request in this implementation.

## Error response example

```json
{
  "timestamp": "2026-10-12T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "order must be either 'asc' or 'desc'.",
  "path": "/api/dataset/employee_dataset/query",
  "validationErrors": {}
}
```

## Design overview
- Controller: HTTP routing and response codes.
- Service: JSON validation, grouping, sorting and business rules.
- Repository: database persistence.
- Entity: dataset name, JSON payload and timestamp.
- Global exception handler: consistent error responses.
- Tests: service-level unit tests with JUnit 5 and Mockito.

## Notes / tradeoffs
- This starter implementation loads all records for one dataset into memory before grouping or sorting. That is simple and suitable for a small assignment, but large datasets should use pagination and/or database-side JSON functions.
- Sorting compares numeric JSON values numerically, booleans as booleans, and other values as case-insensitive strings. Missing/null sort values are placed last for ascending order.
- Dataset names are validated against `[A-Za-z0-9_-]{1,100}`.
