# Stock Data Fetcher

A Spring Boot application that fetches stock data from the Twelve Data API for both NSE (National Stock Exchange) and BSE (Bombay Stock Exchange) and stores it in a MySQL database.

## Prerequisites

1. **Java 17 or higher**
   - Download from: https://adoptium.net/
   - Verify installation: `java -version`

2. **MySQL 8.0 or higher**
   - Download from: https://dev.mysql.com/downloads/mysql/
   - Or use Docker: `docker run --name mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=myreturn -p 3307:3306 -d mysql:8.0`

## Setup Instructions

### 1. Get Twelve Data API Key
1. Go to [Twelve Data](https://twelvedata.com/)
2. Sign up for a free account
3. Get your API key from the dashboard

### 2. Configure Database
1. Start MySQL server
2. Create a database named `myreturn`:
   ```sql
   CREATE DATABASE myreturn;
   ```

### 3. Configure Application
1. Open `src/main/resources/application.properties`
2. Update the API key:
   ```properties
   twelvedata.api.key=your_actual_api_key_here
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

## Testing

1. **Test manual refresh** (using curl or Postman):
   ```bash
   curl -X POST http://localhost:8080/api/stocks/refresh
   ```

2. **Check the data**:
   ```bash
   curl http://localhost:8080/api/stocks/nse
   curl http://localhost:8080/api/stocks/bse
   ```

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Ensure MySQL is running
   - Check database credentials in `application.properties`
   - Verify database `myreturn` exists

2. **API Key Error**
   - Ensure you have a valid Twelve Data API key
   - Check the API key in `application.properties`

3. **Port Already in Use**
   - Change the port in `application.properties`:
     ```properties
     server.port=8081
     ```

4. **Java Version Error**
   - Ensure Java 17+ is installed
   - Set JAVA_HOME environment variable 