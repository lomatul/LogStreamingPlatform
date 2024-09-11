package org.example;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.write.Point;



public class InfluxDBService {

    private static final String TOKEN = "your-influxdb-token";
    private static final String BUCKET = "your-bucket-name";
    private static final String ORG = "your-org-name";
    private static final String INFLUXDB_URL = "http://localhost:8086"; // Adjust the URL if necessary

    private static final InfluxDBClient influxDBClient = InfluxDBClientFactory.create(INFLUXDB_URL, TOKEN.toCharArray());

    // Method to write log data to InfluxDB
    public static void writeLogToInfluxDB(String logMessage) {
        // Parse your logMessage (assume it's a JSON string or a CSV, adjust accordingly)
        // Extract the fields from logMessage (e.g., responseTime, statusCode, apiEndpoint)
        // Example (assuming CSV for simplicity):
        String[] logParts = logMessage.split(",");
        String sourceApi = logParts[0];
        String initialTime = logParts[1];
        String apiEndpoint = logParts[2];
        int statusCode = Integer.parseInt(logParts[3]);
        long responseTime = Long.parseLong(logParts[4]);

        // Create a Point object to write to InfluxDB
        Point point = Point.measurement("api_logs")
                .addTag("source_api", sourceApi)
                .addTag("initial_time", initialTime)
                .addTag("api_endpoint", apiEndpoint)
                .addField("status_code", statusCode)
                .addField("response_time", responseTime);


        // Use WriteApi to send the point to InfluxDB
        try (WriteApi writeApi = influxDBClient.getWriteApi()) {
            writeApi.writePoint(BUCKET, ORG, point);
        }
    }
}
