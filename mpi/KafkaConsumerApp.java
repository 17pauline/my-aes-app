import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

public class KafkaConsumerApp {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "image-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

        KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("image-topic"));

        while (true) {
            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, byte[]> record : records) {
                try {
                    byte[] payload = record.value();
                    ByteBuffer buffer = ByteBuffer.wrap(payload);

                    int opLen = buffer.getInt();
                    byte[] opBytes = new byte[opLen];
                    buffer.get(opBytes);
                    String operation = new String(opBytes);

                    int modeLen = buffer.getInt();
                    byte[] modeBytes = new byte[modeLen];
                    buffer.get(modeBytes);
                    String mode = new String(modeBytes);

                    int keyLen = buffer.getInt();
                    byte[] keyBytes = new byte[keyLen];
                    buffer.get(keyBytes);
                    String key = new String(keyBytes);

                    byte[] imageBytes = new byte[buffer.remaining()];
                    buffer.get(imageBytes);

                    String inputFile = "/tmp/image.bmp";
                    try (FileOutputStream fos = new FileOutputStream(inputFile)) {
                        fos.write(imageBytes);
                    }

                    System.out.printf("[KafkaConsumerApp] Got image: op=%s, mode=%s, key=%s%n", operation, mode, key);

                    ProcessBuilder pb = new ProcessBuilder("mpirun", "-np", "4", "./mpi_app", operation, key, inputFile);
                    pb.inheritIO();
                    Process process = pb.start();
                    int exitCode = process.waitFor();

                    if (exitCode != 0) {
                        System.err.println("[KafkaConsumerApp] MPI process failed with code: " + exitCode);
                        continue;
                    }

                    System.out.println("[KafkaConsumerApp] MPI process completed.");

                    byte[] resultImage = Files.readAllBytes(Paths.get("output.bmp"));
                    String imageId = UUID.randomUUID().toString();

                    String jdbcUrl = "jdbc:mysql://mysql:3306/image_db";
                    try (Connection conn = DriverManager.getConnection(jdbcUrl, "root", "password")) {
                        String sql = "INSERT INTO images (id, image_data) VALUES (?, ?)";
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setString(1, imageId);
                            stmt.setBytes(2, resultImage);
                            stmt.executeUpdate();
                            System.out.println("[KafkaConsumerApp] Image inserted into MySQL with ID: " + imageId);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
