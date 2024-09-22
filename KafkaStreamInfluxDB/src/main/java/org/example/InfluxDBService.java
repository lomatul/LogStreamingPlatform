package org.example;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


public class InfluxDBService {

    private static final String TOKEN = "Xt2SdfMNcIoV6qfudNjiHZZBKuKG9J2R68Cm6snlNAkTAFSgwiraAqTGD_ya8N1nOBzoBA2jyRRrXqsVZQ7JBA==";
    private static final String BUCKET = "logs_data2";
    private static final String ORG = "fddd887d19c7b064";
    private static final String INFLUXDB_URL = "http://localhost:8086";

    private static final InfluxDBClient influxDBClient = InfluxDBClientFactory.create(INFLUXDB_URL, TOKEN.toCharArray());

    public static void writeLogToInfluxDB(String sourceIp, String initialTime, String apiEndpoint, int responseTime, int statusCode) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");

        // Parse the initialTime to a LocalDateTime
        LocalDateTime localDateTime = LocalDateTime.parse(initialTime, formatter);
        Instant timestamp = localDateTime.toInstant(ZoneOffset.UTC);

        // Create a Point for InfluxDB
        Point point = Point.measurement("api_logs")
                .addTag("source_ip", sourceIp)
                .addField("initial_time", initialTime)
                .addTag("api_endpoint", apiEndpoint)
                .addField("status_code", statusCode)
                .addField("response_time", responseTime)
                .time(timestamp, WritePrecision.S);

        try (WriteApi writeApi = influxDBClient.getWriteApi()) {
            writeApi.writePoint(BUCKET, ORG, point);
        } catch (Exception e) {
            System.err.println("Error while writing to InfluxDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
}