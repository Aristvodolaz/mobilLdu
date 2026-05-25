console.log('[boot] ldu-photo-backend loading, pid=%s', process.pid);
const path = require('path');
const fs = require('fs');
const envPath = path.join(__dirname, '.env');
if (!fs.existsSync(envPath)) {
  console.error('[startup] .env not found:', envPath);
  console.error('[startup] Run: cp .env.example .env  and set DB_* / PORT');
  process.exit(1);
}
require('dotenv').config({ path: envPath });

const requiredEnv = ['DB_USER', 'DB_PASSWORD', 'DB_SERVER', 'DB_NAME'];
const missingEnv = requiredEnv.filter((key) => !process.env[key]);
if (missingEnv.length) {
  console.error('[startup] Missing variables in .env:', missingEnv.join(', '));
  process.exit(1);
}

const express = require('express');
const cors = require('cors');
const multer = require('multer');
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
    throw err;
  }
}

// POST endpoint to upload photo(s) with article
app.post('/api/photos', upload.array('photo', 10), async (req, res) => {
  try {
    const article = req.body.article;
    const files = req.files || [];

    // Fallback if single file handler was somehow triggered
    if (req.file) {
      files.push(req.file);
    }

    if (!article) {
      return res.status(400).json({ error: 'Article field is required.' });
    }
    if (files.length === 0) {
      return res.status(400).json({ error: 'Photo file(s) are required.' });
    }

    const insertedData = [];

    // Insert each photo record into Database
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const photoUrlPath = `/uploads/${file.filename}`;

      console.log(`Uploading [${i + 1}/${files.length}]: Article = ${article}, FilePath = ${photoUrlPath}`);

      const request = dbPool.request();
      request.input('article', sql.NVarChar(100), article);
      request.input('photoPath', sql.NVarChar(500), photoUrlPath);

      const query = `
        INSERT INTO LduPhotos (Article, PhotoPath, UploadedAt)
        VALUES (@article, @photoPath, GETDATE())
      `;
      await request.query(query);

      insertedData.push({
        article: article,
        photoPath: photoUrlPath
      });
    }

    return res.status(200).json({
      success: true,
      message: `${files.length} photo(s) uploaded and saved to DB successfully!`,
      data: insertedData
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

// DELETE endpoint to delete a photo and its record
app.delete('/api/photos/:id', async (req, res) => {
  try {
    const { id } = req.params;
    if (!id) {
      return res.status(400).json({ error: 'ID parameter is required.' });
    }

    // 1. Fetch record first to get photo path
    const selectQuery = 'SELECT PhotoPath FROM LduPhotos WHERE Id = @id';
    const selectRequest = dbPool.request();
    selectRequest.input('id', sql.Int, id);
    const selectResult = await selectRequest.query(selectQuery);

    if (selectResult.recordset.length === 0) {
      return res.status(404).json({ error: 'Photo not found.' });
    }

    const photoPath = selectResult.recordset[0].PhotoPath;

    // 2. Delete file from disk if it exists
    if (photoPath) {
      const fileName = path.basename(photoPath);
      const filePath = path.join(uploadDir, fileName);
      if (fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
        console.log(`Deleted file from disk: ${filePath}`);
      } else {
        console.log(`File not found on disk, skipping delete: ${filePath}`);
      }
    }

    // 3. Delete from DB
    const deleteQuery = 'DELETE FROM LduPhotos WHERE Id = @id';
    const deleteRequest = dbPool.request();
    deleteRequest.input('id', sql.Int, id);
    await deleteRequest.query(deleteQuery);

    console.log(`Deleted record from DB: ID = ${id}`);

    return res.status(200).json({
      success: true,
      message: 'Photo deleted successfully!'
    });
  } catch (err) {
    console.error('Failed to delete photo:', err);
    return res.status(500).json({ error: 'Internal Server Error' });
  }
});

// Start application (DB first so PM2 logs show connection errors)
async function startServer() {
  console.log('[startup] ldu-photo-backend starting, PORT=%s', PORT);
  await initDatabase();
  app.listen(PORT, () => {
    console.log(`Backend server running on http://localhost:${PORT}`);
  });
}

startServer().catch((err) => {
  console.error('[startup] failed:', err);
  process.exit(1);
});
