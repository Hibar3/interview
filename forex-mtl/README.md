# Forex Proxy Service

A local proxy service for fetching currency exchange rates, built with Scala.This service acts as a middleware for the One-Frame API, providing caching and freshness guarantees to overcome upstream limitations.

## 🚀 Quick Start

### 1. Prerequisites
- **Java 11+**
- **sbt** (Scala Build Tool)
- **Docker** (to run the One-Frame upstream service)

### 2. Run the Upstream Service (One-Frame)
The application requires the One-Frame API to be running locally.
```bash
docker pull paidyinc/one-frame
docker run -p 8080:8080 paidyinc/one-frame
```

### 3. Build and Run the Application
Navigate to the `forex-mtl` directory and start the server:
```bash
cd forex-mtl
sbt run
```
The server starts on `http://localhost:3000`by default. You can change the port in `application.conf`.

## 🛠 Usage

### Get Exchange Rate
Fetch the current exchange rate between two supported currencies.

**Endpoint:** `GET /rates`

**Parameters:**
- `from`: Source currency code (e.g., USD)
- `to`: Target currency code (e.g., JPY)

**Example Request:**
```bash
curl "http://localhost:3000/rates?from=USD&to=JPY"
``` 

**Example Response:**
```json
{
  "from": "USD",
  "to": "JPY",
  "bid": 0.61,
  "ask": 0.82,
  "price": 0.71,
  "timestamp": "2023-10-27T10:00:00Z"
}
```

## 🧪 Testing
Run the unit tests to verify caching and rate retrieval logic:
```bash
sbt test
```
## ⚙️ Configuration
Settings for the HTTP server, cache TTL, and One-Frame API can be found in:
`src/main/resources/application.conf`

## 🔍 Test the Service via Curl
Run curl command to test the service:
```bash
curl "http://localhost:{your-port}/rates?from=USD&to=JPY"
```

## 💡 Key Features
- **Intelligent Caching**: Implements an in-memory cache with a 5-minute TTL.
- **API Limit Management**: Designed to support 10,000+ requests/day by minimizing calls to the One-Frame API (which has a 1,000 request/day limit).
- **Rate Freshness**: Automatically refreshes rates older than 5 minutes to ensure data accuracy.
- **Descriptive Errors**: Provides clear feedback for invalid currencies or upstream failures.


