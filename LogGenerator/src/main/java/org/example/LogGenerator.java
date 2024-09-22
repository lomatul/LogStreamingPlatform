package org.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class LogGenerator {

    private static final String[] SOURCE_IPS = {
            "192.168.32.1", "192.168.32.2", "192.168.32.3", "192.168.32.4", "192.168.32.5", "192.168.32.6", "192.168.32.7","192.168.32.8", "192.168.32.9", "192.168.32.10"
    };

    private static final String[] API_ENDPOINTS = {
            "/api/v1/resource1", "/api/v1/resource2", "/api/v1/resource3", "/api/v1/resource4", "/api/v1/resource5",
            "/api/v1/resource6", "/api/v1/resource7", "/api/v1/resource8", "/api/v1/resource9", "/api/v1/resource10"
    };

    private static final int[] STATUS_CODES = {200, 301, 302, 401, 403, 404, 410, 500, 502, 503};

    private static Producer<String, GenericRecord> producer;

    private static Schema loadSchema() {
        Schema schema = null;
        try {
            File schemaFile = new File("src/main/resources/avro/log.avsc");
            schema = new Schema.Parser().parse(schemaFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return schema;
    }


    private static Connection connection;
    public static Schema schema = loadSchema();
    public static void main(String[] args) {

         initializeKafkaProducer();

        try (FileWriter csvWriter = new FileWriter("logs.csv")) {


            for (int i = 0; i < 1000; i++) {

                 Log kafkaLog = generateCurrentMonthLog();
                 sendToKafka(kafkaLog);

//                Log mysqlLog = generatePreviousMonthLog();
//                writeToCsv(mysqlLog, csvWriter);

                TimeUnit.MILLISECONDS.sleep(1);
            }

            System.out.println("Log generation completed. Starting bulk insert into MySQL...");
//            bulkInsertToMySQL();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

             closeKafkaProducer();
        }
    }

    private static void initializeKafkaProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", "http://localhost:8081"); // Schema Registry URL

        producer = new KafkaProducer<>(props);
    }

    private static Log generateCurrentMonthLog() {
        return generateLogWithMonthOffset(0);
    }

    private static Log generatePreviousMonthLog() {
        return generateLogWithMonthOffset(-1);
    }

    private static Calendar calendar = null;
    private static int initialMonth = -1;
    private static int maxDaysInInitialMonth = -1;

    private static Log generateLogWithMonthOffset(int monthOffset) {

        if (calendar == null) {
            calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, monthOffset);
            initialMonth = calendar.get(Calendar.MONTH);
            maxDaysInInitialMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        }
        calendar.add(Calendar.SECOND, 2000);

        if (calendar.get(Calendar.MONTH) != initialMonth) {

            calendar.set(Calendar.DAY_OF_MONTH, maxDaysInInitialMonth);
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 0);
        }

        return generateLogWithTime(calendar);


    }

    private static Log generateLogWithTime(Calendar calendar) {
        Random random = new Random();
        String sourceIp = SOURCE_IPS[random.nextInt(SOURCE_IPS.length)];

        Timestamp initialTime = new Timestamp(calendar.getTimeInMillis());


        String apiEndpoint = API_ENDPOINTS[random.nextInt(API_ENDPOINTS.length)];
        int responseTime = random.nextInt(901) + 100;
        int statusCode = STATUS_CODES[random.nextInt(STATUS_CODES.length)];

        return new Log(sourceIp, initialTime, apiEndpoint, responseTime, statusCode);
    }


    private static void writeToCsv(Log log, FileWriter csvWriter) throws IOException {
        csvWriter.append(log.sourceIp).append(',')
                .append(log.initialTime.toString()).append(',')
                .append(log.apiEndpoint).append(',')
                .append(String.valueOf(log.responseTime)).append(',')
                .append(String.valueOf(log.statusCode)).append('\n');
        csvWriter.flush();
    }

    private static void sendToKafka(Log log) {



        GenericRecord record = new GenericData.Record(schema);

        record.put("source_ip", log.sourceIp);
        record.put("initial_time", log.initialTime.toString());
        record.put("api_endpoint", log.apiEndpoint);
        record.put("response_time", log.responseTime);
        record.put("status_code", log.statusCode);

        ProducerRecord<String, GenericRecord> avroRecord = new ProducerRecord<>("logs_topic", "log", record);
        producer.send(avroRecord, (metadata, e) -> {
            if (e != null) {
                System.err.println("Error sending record: " + e.getMessage());
            } else {
                System.out.println("Sent: " + record);
            }
        });
    }

    private static void bulkInsertToMySQL() {
        initializeMySQLConnection();
        String query = "LOAD DATA LOCAL INFILE 'logs.csv' INTO TABLE logs " +
                "FIELDS TERMINATED BY ',' " +
                "LINES TERMINATED BY '\\n' " +
                "(source_api, initial_time, api_endpoint, response_time, status_code)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.executeUpdate();
            System.out.println("Bulk insert completed successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeMySQLConnection();
        }
    }

    private static void initializeMySQLConnection() {
        try {
            String url = "jdbc:mysql://localhost:3306/log_db?allowLoadLocalInfile=true";
            String user = "root";
            String password = "hello123";
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void closeKafkaProducer() {
        if (producer != null) {
            producer.close();
        }
    }

    private static void closeMySQLConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static class Log {
        String sourceIp;
        Timestamp initialTime;
        String apiEndpoint;
        int responseTime;
        int statusCode;

        Log(String sourceIp, Timestamp initialTime, String apiEndpoint, int responseTime, int statusCode) {
            this.sourceIp = sourceIp;
            this.initialTime = initialTime;
            this.apiEndpoint = apiEndpoint;
            this.responseTime = responseTime;
            this.statusCode = statusCode;
        }
    }
}
