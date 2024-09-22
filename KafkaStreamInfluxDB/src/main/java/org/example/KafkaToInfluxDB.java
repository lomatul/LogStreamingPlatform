package org.example;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.common.serialization.Serdes;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;

import java.util.Properties;

public class KafkaToInfluxDB {

    public static void main(String[] args) {

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-to-influxdb");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, GenericAvroSerde.class.getName());
        props.put("schema.registry.url", "http://localhost:8081");

        StreamsBuilder builder = new StreamsBuilder();


        KStream<String, GenericRecord> stream = builder.stream("logs_topic");



        stream.foreach((key, value) -> {
            String sourceIp = value.get("source_ip").toString();
            String initialTime = value.get("initial_time").toString();
            String apiEndpoint = value.get("api_endpoint").toString();
            int responseTime = (int) value.get("response_time");
            int statusCode = (int) value.get("status_code");

            // Save to InfluxDB
            saveToInfluxDB(sourceIp, initialTime, apiEndpoint, responseTime, statusCode);
        });

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static void saveToInfluxDB(String sourceIp, String initialTime, String apiEndpoint, int responseTime, int statusCode) {
        InfluxDBService.writeLogToInfluxDB(sourceIp, initialTime, apiEndpoint, responseTime, statusCode);
    }
}

