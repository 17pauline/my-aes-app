package com.example.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

@RestController
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);
    private static final String TOPIC = "image-topic";

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file,
                              @RequestParam("operation") String operation,
                              @RequestParam("mode") String mode,
                              @RequestParam("key") String key) throws IOException {

        if (file.isEmpty()) {
            logger.error("File is empty");
            return "File not found.";
        }

        byte[] imageBytes = toByteArray(file.getInputStream());
        logger.info("Received BMP file: {}", file.getOriginalFilename());
        logger.info("Size: {} bytes", imageBytes.length);
        logger.info("Operation: {}, Mode: {}, Key: {}", operation, mode, key);

        try {
            // Serialize parameters and image into a single byte array
            byte[] operationBytes = operation.getBytes();
            byte[] modeBytes = mode.getBytes();
            byte[] keyBytes = key.getBytes();

            ByteBuffer buffer = ByteBuffer.allocate(
                    4 + operationBytes.length +
                    4 + modeBytes.length +
                    4 + keyBytes.length +
                    imageBytes.length
            );

            buffer.putInt(operationBytes.length);
            buffer.put(operationBytes);
            buffer.putInt(modeBytes.length);
            buffer.put(modeBytes);
            buffer.putInt(keyBytes.length);
            buffer.put(keyBytes);
            buffer.put(imageBytes);

            kafkaTemplate.send(TOPIC, buffer.array());
            logger.info("Message sent to Kafka topic: {}", TOPIC);
        } catch (Exception e) {
            logger.error("Error sending to Kafka", e);
            return "Failed to send to Kafka.";
        }

        return "Upload sent to Kafka successfully.";
    }

    private byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
