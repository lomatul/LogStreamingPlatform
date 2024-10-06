package org.example;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.api.common.serialization.SimpleStringSchema;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FlinkConsumer {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        System.out.println("Running in local mode.");

        // Kafka properties
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092"); // Update this with your Kafka broker
        properties.setProperty("group.id", "test"); // Update as needed

        // Create a Kafka consumer
        FlinkKafkaConsumer<GenericRecord> consumer = new FlinkKafkaConsumer<>("logs_topic",
                new AvroDeserializationSchema(),
                properties);

        // Add the consumer as a source to the execution environment
        DataStream<GenericRecord> stream = env.addSource(consumer);

        // Example processing: print the stream to the console
        stream.print();


        // Execute the environment
        env.execute("Kafka Avro Status Code Alert Example");
    }

    public static class AvroDeserializationSchema implements DeserializationSchema<GenericRecord> {
        private final Schema schema;

        public AvroDeserializationSchema() {
            // Load the Avro schema from a file
            try (InputStream inputStream = new FileInputStream(new File("src/main/avro/LogEntry.avsc"))) {
                this.schema = new Schema.Parser().parse(inputStream);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load Avro schema", e);
            }
        }

        @Override
        public GenericRecord deserialize(byte[] message) {
            try {
                SpecificDatumReader<GenericRecord> reader = new SpecificDatumReader<>(schema);
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
