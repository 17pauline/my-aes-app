CREATE DATABASE IF NOT EXISTS image_db;

USE image_db;

CREATE TABLE IF NOT EXISTS images (
  id VARCHAR(255) PRIMARY KEY,
  image_data LONGBLOB
);
