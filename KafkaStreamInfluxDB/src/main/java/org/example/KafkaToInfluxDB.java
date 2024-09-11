package org.example;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.common.serialization.Serdes;

import java.util.Properties;

public class KafkaToInfluxDB {

    public static void main(String[] args) {
        // Kafka Streams configuration
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-to-influxdb");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"); // Kafka broker URL
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();

        // Read from your Kafka topic (replace "your-kafka-topic" with your actual topic name)
        KStream<String, String> stream = builder.stream("logs_topic");

        // Process and save to InfluxDB
        stream.foreach((key, value) -> {
            // Process and write each message (log) to InfluxDB
            saveToInfluxDB(value); // value contains the log data
        });

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        // Add shutdown hook to close the streams on exit
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static void saveToInfluxDB(String logMessage) {
        // Use the InfluxDBService to process the log and write it to InfluxDB bucket
        InfluxDBService.writeLogToInfluxDB(logMessage);
    }
}
