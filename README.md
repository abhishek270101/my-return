# Stock Data Fetcher

A Spring Boot application that fetches stock data from multiple APIs (Twelve Data and Groww) for both NSE (National Stock Exchange) and BSE (Bombay Stock Exchange) and stores it in a MySQL database. Includes real-time data streaming via WebSocket and comprehensive historical data storage with multiple time intervals.

## Features

- **Real-Time Data Streaming**: WebSocket-based real-time stock price updates
- **Historical Data from Groww API**: Comprehensive historical candle data for Nifty stocks
- **Multiple Time Intervals**: Stores data at 10s, 30s, 1m, 5m, 15m, 1d, and 1w intervals
- **Separate Tables for Each Interval**: Organized data storage for different candle intervals
- **Nifty Index Support**: Pre-configured symbols for Nifty 50, 100, and 500
- **Rate Limiting**: Built-in rate limiting for Groww API to respect API limits
- **Automatic Data Fetching**: Fetches stock data daily at 2:00 AM from Twelve Data API
- **Dual Exchange Support**: Supports both NSE and BSE exchanges
- **RESTful API**: Provides endpoints to manually trigger data refresh and retrieve stock data
- **Database Storage**: Stores stock information in MySQL database
- **Scheduled Updates**: Automatically updates stock data daily

## Prerequisites

1. **Java 17 or higher**
   - Download from: https://adoptium.net/
   - Verify installation: `java -version`

2. **MySQL 8.0 or higher**
   - Download from: https://dev.mysql.com/downloads/mysql/
   - Or use Docker: `docker run --name mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=myreturn -p 3307:3306 -d mysql:8.0`

## Setup Instructions

### 1. Get API Keys
1. **Twelve Data API Key**:
   - Go to [Twelve Data](https://twelvedata.com/)
   - Sign up for a free account
   - Get your API key from the dashboard

2. **Groww API Key**:
   - Contact Groww for API access
   - Get your API key and secret from Groww

### 2. Configure Database
1. Start MySQL server
2. Create a database named `myreturn`:
   ```sql
   CREATE DATABASE myreturn;
   ```

### 3. Configure Application
1. Open `src/main/resources/application.properties`
2. Update the API keys:
   ```properties
   twelvedata.api.key=your_twelvedata_api_key_here
   groww.api.key=your_groww_api_key_here
   groww.api.secret=your_groww_api_secret_here
   ```
3. Update database credentials if needed:
   ```properties
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

### 4. Build and Run
```bash
# Build the application
./gradlew build

# Run the application
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## API Endpoints

### Stock Data Endpoints
- `GET /api/stocks/nse` - Get all NSE stocks from database
- `GET /api/stocks/bse` - Get all BSE stocks from database

### Manual Refresh Endpoints
- `POST /api/stocks/refresh` - Refresh both NSE and BSE stocks
- `POST /api/stocks/refresh/nse` - Refresh only NSE stocks
- `POST /api/stocks/refresh/bse` - Refresh only BSE stocks

### Real-Time Stock Data Endpoints
- `GET /api/stocks/realtime/{symbol}` - Get latest price for a symbol
- `GET /api/stocks/realtime/{symbol}/historical` - Get historical data for a symbol
- `GET /api/stocks/realtime/{symbol}/recent` - Get recent data for a symbol
- `POST /api/stocks/realtime/start` - Start real-time data fetching for symbols

### Groww Historical Data Endpoints
- `POST /api/groww/fetch-historical` - Fetch historical data for custom symbols
- `POST /api/groww/fetch-symbol` - Fetch historical data for a single symbol
- `POST /api/groww/fetch-nifty50` - Fetch historical data for Nifty 50 stocks
- `POST /api/groww/fetch-nifty100` - Fetch historical data for Nifty 100 stocks
- `POST /api/groww/fetch-nifty500` - Fetch historical data for Nifty 500 stocks
- `POST /api/groww/fetch-all-intervals` - Fetch all intervals for a symbol
- `POST /api/groww/fetch-interval` - Fetch specific interval for a symbol
- `GET /api/groww/rate-limit-status` - Check current rate limit status

### WebSocket Endpoints
- WebSocket URL: `ws://localhost:8080/ws`
- Subscribe to: `/topic/stock/{symbol}`

## Rate Limiting

The application implements rate limiting for the Groww API based on the following limits:

### Groww API Rate Limits (Live Data Type)
| Limit Type | Limit | Description |
|------------|-------|-------------|
| Per Second | 10 requests | Maximum requests per second |
| Per Minute | 300 requests | Maximum requests per minute |
| Per Day | 5000 requests | Maximum requests per day |

### Rate Limiting Features
- **Automatic Rate Limiting**: Built-in counters track requests per second, minute, and day
- **Batch Processing**: Symbols are processed in batches of 10 to respect rate limits
- **Automatic Delays**: 100ms delay between requests and 2-second delays between batches
- **Rate Limit Monitoring**: Endpoint to check current rate limit status
- **Graceful Handling**: Skips requests when rate limits are exceeded

## Database Schema

### Stock Tables
- `stock` - Basic stock information from Twelve Data API
- `stock_price` - Real-time and historical price data

### Candle Data Tables (Groww API)
- `candle_1min` - 1-minute candle data
- `candle_5min` - 5-minute candle data
- `candle_10min` - 10-minute candle data
- `candle_1hour` - 1-hour candle data
- `candle_4hour` - 4-hour candle data
- `candle_1day` - 1-day candle data
- `candle_1week` - 1-week candle data

## Time Intervals Supported

### Twelve Data API
| Interval | Description | Use Case |
|----------|-------------|----------|
| 10s | 10 seconds | Ultra-short term analysis |
| 30s | 30 seconds | Short-term monitoring |
| 1m | 1 minute | Intraday trading |
| 5m | 5 minutes | Short-term trends |
| 15m | 15 minutes | Medium-term analysis |
| 1d | 1 day | Daily analysis |
| 1w | 1 week | Weekly trends |

### Groww API
| Interval | Max Duration | Historical Data Available |
|----------|--------------|--------------------------|
| 1 min | 7 days | Last 3 months |
| 5 min | 15 days | Last 3 months |
| 10 min | 30 days | Last 3 months |
| 1 hour | 150 days | Last 3 months |
| 4 hours | 365 days | Last 3 months |
| 1 day | 1080 days (~3 years) | Full history |
| 1 week | No Limit | Full history |

## Testing

1. **Access the Web Interface**:
   - Open `http://localhost:8080` in your browser
   - Use the provided interface to test WebSocket connections and REST APIs

2. **Test Real-Time Data**:
   ```bash
   # Start real-time data for AAPL
   curl -X POST http://localhost:8080/api/stocks/realtime/start \
        -H "Content-Type: application/json" \
        -d '["AAPL"]'
   
   # Get latest price
   curl http://localhost:8080/api/stocks/realtime/AAPL
   ```

3. **Test Groww Historical Data**:
   ```bash
   # Check rate limit status
   curl http://localhost:8080/api/groww/rate-limit-status
   
   # Fetch Nifty 50 historical data
   curl -X POST http://localhost:8080/api/groww/fetch-nifty50
   
   # Fetch specific symbol
   curl -X POST "http://localhost:8080/api/groww/fetch-symbol?symbol=RELIANCE"
   
   # Fetch specific interval
   curl -X POST "http://localhost:8080/api/groww/fetch-interval?symbol=RELIANCE&intervalMinutes=5"
   ```

4. **Test WebSocket** (using the web interface):
   - Connect to WebSocket
   - Subscribe to a stock symbol
   - Watch real-time updates

## WebSocket Usage

### Connect to WebSocket
```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function (frame) {
    console.log('Connected: ' + frame);
});
```

### Subscribe to Real-Time Data
```javascript
stompClient.subscribe('/topic/stock/AAPL', function (stockPrice) {
    const data = JSON.parse(stockPrice.body);
    console.log('Real-time price:', data.price);
});
```

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Ensure MySQL is running
   - Check database credentials in `application.properties`
   - Verify database `myreturn` exists

2. **API Key Error**
   - Ensure you have valid API keys for both Twelve Data and Groww
   - Check the API keys in `application.properties`

3. **Rate Limit Issues**
   - Check rate limit status: `GET /api/groww/rate-limit-status`
   - Wait for rate limits to reset (automatic)
   - Reduce batch sizes if needed

4. **WebSocket Connection Issues**
   - Ensure the application is running
   - Check browser console for connection errors
   - Verify WebSocket endpoint is accessible

5. **Port Already in Use**
   - Change the port in `application.properties`:
     ```properties
     server.port=8081
     ```

6. **Java Version Error**
   - Ensure Java 17+ is installed
   - Set JAVA_HOME environment variable

### Performance Considerations

- The application uses multiple threads for different time intervals
- Database indexes are recommended for large datasets
- Consider implementing data cleanup for old records
- Monitor API rate limits from both Twelve Data and Groww
- Historical data fetching can be resource-intensive for large symbol lists
- Rate limiting ensures compliance with API limits and prevents service disruption 