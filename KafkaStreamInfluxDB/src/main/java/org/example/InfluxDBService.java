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
    private static final String BUCKET = "logs_data";
    private static final String ORG = "fddd887d19c7b064";
    private static final String INFLUXDB_URL = "http://localhost:8086";

    private static final InfluxDBClient influxDBClient = InfluxDBClientFactory.create(INFLUXDB_URL, TOKEN.toCharArray());


    public static void writeLogToInfluxDB(String logMessage) {

        String[] logParts = logMessage.split(",");
        String sourceIp = logParts[0];
        String initialTime = logParts[1];
        String apiEndpoint = logParts[2];
        String responseTime = logParts[3];
        String statusCode= logParts[4];

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");

        LocalDateTime localDateTime = LocalDateTime.parse(initialTime, formatter);

        Instant timestamp = localDateTime.toInstant(ZoneOffset.UTC);


        Point point = Point.measurement("api_logs")
                .addTag("source_ip", sourceIp)
                .addTag("initial_time", initialTime)
                .addTag("api_endpoint", apiEndpoint)
                .addField("status_code", Integer.parseInt(statusCode))
                .addField("response_time",Integer.parseInt(responseTime))
                .time(Instant.now(), WritePrecision.MS);


        try (WriteApi writeApi = influxDBClient.getWriteApi()) {
            writeApi.writePoint(BUCKET, ORG, point);

        }
        catch(Exception e) {
            System.err.println("Error while writing to InfluxDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
