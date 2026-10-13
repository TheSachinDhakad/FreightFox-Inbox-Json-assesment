# JSON Dataset API (Spring Boot + MySQL)

A REST API that stores arbitrary JSON object records under a dataset name and supports dynamic `groupBy` and `sortBy` queries.

## Requirements
- Java 21
- Maven 3.9+
- MySQL 8+

## 1. Create the database

Run in MySQL:

```sql
CREATE DATABASE json_dataset_db;
```

The application creates/updates the `dataset_records` table through Hibernate. The JSON column requires a MySQL version that supports the `JSON` data type (MySQL 5.7+; MySQL 8 recommended).

## 2. Configure Aiven Cloud MySQL

`application.properties` is preconfigured with your Aiven host, port, database (`defaultdb`), username (`avnadmin`), and required SSL mode. The password is intentionally read from the `DB_PASSWORD` environment variable and is not stored in this repository.

In the terminal, from the project folder, set the password securely:

```bash
read -s DB_PASSWORD
export DB_PASSWORD
```

Paste your **rotated/new Aiven password** when prompted, then press Enter. The input will be hidden.

Optional overrides:

```bash
export DB_URL='jdbc:mysql://YOUR_HOST:YOUR_PORT/defaultdb?sslMode=REQUIRED'
export DB_USERNAME='avnadmin'
```

Do not commit passwords to Git. Since the database password was shared in chat, rotate it in Aiven before running this project.

## 3. Run

```bash
mvn clean test
mvn spring-boot:run
```

The server runs at `http://localhost:9090`.

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
curl -X POST http://localhost:8080/api/dataset/employee_dataset/record \
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
