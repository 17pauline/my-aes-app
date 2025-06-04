const express = require('express');
const mysql = require('mysql2');
const app = express();
const port = 3000;

// load mysql connection settings from environment variables
const db = mysql.createConnection({
  host: process.env.MYSQL_HOST || 'localhost',
  user: process.env.MYSQL_USER || 'root',
  password: process.env.MYSQL_PASSWORD || '',
  database: process.env.MYSQL_DATABASE || 'image_db'
});

// connect to mysql
db.connect(err => {
  if (err) {
    console.error('MySQL connection error:', err);
    process.exit(1);
  }
  console.log('Connected to MySQL');
});

// GET /image/:id - Retrieve image by ID
app.get('/image/:id', (req, res) => {
  const imageId = req.params.id;

  db.query('SELECT image_data FROM images WHERE id = ?', [imageId], (err, results) => {
    if (err) {
      console.error('Database query failed:', err);
      return res.status(500).send('Internal server error.');
    }

    if (results.length === 0) {
      return res.status(404).send('Image not found.');
    }

    res.setHeader('Content-Type', 'image/bmp');
    res.send(results[0].image_data);
  });
});

// health check
app.get('/', (req, res) => {
  res.send('Node.js image API is running.');
});

// start server
app.listen(port, () => {
  console.log(`Server listening at http://localhost:${port}`);
  console.log(`Retrieving image at endpoint: http://localhost:${port}/image/:id`);
});
