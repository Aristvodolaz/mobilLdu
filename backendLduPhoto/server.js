require('dotenv').config();
const express = require('express');
const cors = require('cors');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const sql = require('mssql');

const app = express();
const PORT = process.env.PORT || 3000;

// Enable CORS for all requests (crucial for mobile apps and dev setups)
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Ensure upload directories exist
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// Multer configuration for file uploads
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    // Generate a unique filename: timestamp + original filename clean of special characters
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    const ext = path.extname(file.originalname);
    cb(null, 'photo-' + uniqueSuffix + ext);
  }
});

const upload = multer({
  storage: storage,
  limits: { fileSize: 10 * 1024 * 1024 } // 10MB limit
});

// Serve uploads folder static files
app.use('/uploads', express.static(uploadDir));
// Serve public frontend
app.use(express.static(path.join(__dirname, 'public')));

// Database configuration
const dbConfig = {
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  server: process.env.DB_SERVER,
  port: parseInt(process.env.DB_PORT || 59587),
  database: process.env.DB_NAME,
  options: {
    encrypt: process.env.DB_ENCRYPT === 'true',
    trustServerCertificate: true // necessary for local dev connectivity issues
  }
};

let dbPool = null;

// Connect to Database and Ensure Table exists
async function initDatabase() {
  try {
    console.log('Connecting to SQL Server at:', dbConfig.server);
    dbPool = await sql.connect(dbConfig);
    console.log('SQL Server Connected Successfully.');

    // Create table if it doesn't exist
    const createTableQuery = `
      IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='LduPhotos' and xtype='U')
      BEGIN
        CREATE TABLE LduPhotos (
          Id INT IDENTITY(1,1) PRIMARY KEY,
          Article NVARCHAR(100) NOT NULL,
          PhotoPath NVARCHAR(500) NOT NULL,
          UploadedAt DATETIME DEFAULT GETDATE()
        );
        PRINT 'Table LduPhotos created successfully.';
      END
      ELSE
      BEGIN
        PRINT 'Table LduPhotos already exists.';
      END
    `;
    const result = await dbPool.request().query(createTableQuery);
    if (result.output) {
      console.log(result.output);
    }
  } catch (err) {
    console.error('Database connection / initialization failed:', err);
    process.exit(1);
  }
}

// POST endpoint to upload photo with article
app.post('/api/photos', upload.single('photo'), async (req, res) => {
  try {
    const article = req.body.article;
    const file = req.file;

    if (!article) {
      return res.status(400).json({ error: 'Article field is required.' });
    }
    if (!file) {
      return res.status(400).json({ error: 'Photo file is required.' });
    }

    // Relative web URL/path to the saved photo
    const photoUrlPath = `/uploads/${file.filename}`;

    console.log(`Uploading: Article = ${article}, FilePath = ${photoUrlPath}`);

    // Insert record into Database
    const query = `
      INSERT INTO LduPhotos (Article, PhotoPath, UploadedAt)
      VALUES (@article, @photoPath, GETDATE())
    `;

    const request = dbPool.request();
    request.input('article', sql.NVarChar(100), article);
    request.input('photoPath', sql.NVarChar(500), photoUrlPath);

    await request.query(query);

    return res.status(200).json({
      success: true,
      message: 'Photo uploaded and saved to DB successfully!',
      data: {
        article: article,
        photoPath: photoUrlPath
      }
    });
  } catch (err) {
    console.error('Failed to process upload:', err);
    return res.status(500).json({ error: 'Internal Server Error' });
  }
});

// GET endpoint to fetch all photos
app.get('/api/photos', async (req, res) => {
  try {
    const query = `
      SELECT Id, Article, PhotoPath, UploadedAt
      FROM LduPhotos
      ORDER BY UploadedAt DESC
    `;
    const result = await dbPool.request().query(query);
    return res.status(200).json(result.recordset);
  } catch (err) {
    console.error('Failed to fetch photos:', err);
    return res.status(500).json({ error: 'Internal Server Error' });
  }
});

// Start application
app.listen(PORT, async () => {
  await initDatabase();
  console.log(`Backend server running on http://localhost:${PORT}`);
});
