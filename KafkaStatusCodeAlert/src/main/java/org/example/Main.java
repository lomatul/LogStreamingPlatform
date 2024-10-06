package org.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.util.Collector;

public class Main {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
//        env.getConfig().setRestartStrategy(RestartStrategies.fixedDelayRestart(
//                3, // number of restart attempts
//                Time.of(10, TimeUnit.SECONDS) // delay between restart attempts
//        ));
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092");
        properties.setProperty("group.id", "kafka-to-flink");
        properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty("value.deserializer", "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        properties.setProperty("schema.registry.url", "http://localhost:8081");

        FlinkKafkaConsumer<GenericRecord> kafkaConsumer = new FlinkKafkaConsumer<>(
                "logs_topic",
                new AvroDeserializationSchema(),
                properties
        );

        DataStream<GenericRecord> stream = env.addSource(kafkaConsumer);

        // Process the stream and generate alerts for specific status codes
        stream.flatMap(new StatusCodeAlertFunction()).print();
        System.out.println("here1");

        env.execute("Flink Kafka Avro Consumer");
    }

    public static final class StatusCodeAlertFunction implements FlatMapFunction<GenericRecord, String> {
        @Override
        public void flatMap(GenericRecord record, Collector<String> out) {
            System.out.println("here4");
            String initialTime = record.get("initial_time").toString();
            String statusCode = record.get("status_code").toString();

            System.out.println("Received: " + record);
            System.out.println("Initial Time: " + initialTime);
            System.out.println("Status Code: " + statusCode);

//            // Check for specific status codes to generate alerts
//            if ("500".equals(statusCode)) {
//                out.collect("Alert: Internal Server Error (500) detected!");
//            } else if ("404".equals(statusCode)) {
//                out.collect("Alert: Page Not Found (404) detected!");
//            }
        }
    }

    public static class AvroDeserializationSchema implements DeserializationSchema<GenericRecord> {
        private final Schema schema;

        public AvroDeserializationSchema() {
            try (InputStream inputStream = new FileInputStream(new File("src/main/avro/LogEntry.avsc"))) {
                this.schema = new Schema.Parser().parse(inputStream);
                System.out.println(schema);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load Avro schema", e);
            }
        }

        @Override
        public GenericRecord deserialize(byte[] message) {
            if (message == null || message.length == 0) {
                return null; // Handle empty message case
            }

            try {
                SpecificDatumReader<GenericRecord> reader = new SpecificDatumReader<>(schema);
                System.out.println(message);
                return reader.read(null, DecoderFactory.get().binaryDecoder(message, null));

            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize Avro message", e);
            }
        }

        @Override
        public boolean isEndOfStream(GenericRecord nextElement) {
            return false;
        }

        @Override
        public TypeInformation<GenericRecord> getProducedType() {
            return TypeExtractor.getForClass(GenericRecord.class);
        }
    }
}
