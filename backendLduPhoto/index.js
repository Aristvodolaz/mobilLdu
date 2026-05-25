require('dotenv').config();
const express = require('express');
const multer = require('multer');
const cors = require('cors');
const sql = require('mssql');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;

// Enable CORS for mobile application connections
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Setup uploads directory for disk backup
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// Multer memory storage (we need buffer for DB insertion, and we also write to disk as backup)
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 } // limit 10MB
});

// Database configuration
const dbConfig = {
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  server: process.env.DB_SERVER,
  port: parseInt(process.env.DB_PORT || '1433'),
  database: process.env.DB_NAME,
  options: {
    encrypt: process.env.DB_ENCRYPT === 'true',
    trustServerCertificate: process.env.DB_TRUST_CERT === 'true'
  }
};

let dbPool = null;

// Initialize database and create table if it doesn't exist
async function initDatabase() {
  try {
    console.log('Connecting to MSSQL database...');
    dbPool = await sql.connect(dbConfig);
    console.log('Connected to MSSQL database successfully.');

    const createTableQuery = `
      IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='LduPhotos' AND xtype='U')
      CREATE TABLE LduPhotos (
          Id INT IDENTITY(1,1) PRIMARY KEY,
          Sku NVARCHAR(100) NOT NULL,
          Marketplace NVARCHAR(50) NOT NULL,
          PhotoName NVARCHAR(255) NOT NULL,
          PhotoData VARBINARY(MAX) NOT NULL,
          CreatedAt DATETIME DEFAULT GETDATE()
      );
    `;
    await dbPool.request().query(createTableQuery);
    console.log('Database table verification/creation complete.');
  } catch (err) {
    console.error('Database initialization failed:', err.message);
    console.warn('Backend will continue running, but DB operations will fail. Running in DB-unreachable fallback mode.');
  }
}

// REST Endpoint to upload a photo and article
app.post('/api/photos', upload.single('photo'), async (req, res) => {
  const { sku, marketplace } = req.body;
  const file = req.file;

  console.log(`Received request: SKU=${sku}, Marketplace=${marketplace}, File=${file ? file.originalname : 'None'}`);

  if (!sku) {
    return res.status(400).json({ success: false, error: 'SKU (артикул) is required' });
  }
  if (!marketplace) {
    return res.status(400).json({ success: false, error: 'Marketplace prefix is required' });
  }
  if (!file) {
    return res.status(400).json({ success: false, error: 'Photo file is required' });
  }

  // Generate unique file name
  const timestamp = Date.now();
  const fileExt = path.extname(file.originalname) || '.jpg';
  const cleanSku = sku.replace(/[^a-zA-Z0-9-_]/g, '_');
  const cleanMarketplace = marketplace.replace(/[^a-zA-Z0-9-_\s]/g, '_').trim().replace(/\s+/g, '_');
  const filename = `${cleanMarketplace}_${cleanSku}_${timestamp}${fileExt}`;

  // 1. Save backup to disk
  try {
    const backupPath = path.join(uploadDir, filename);
    fs.writeFileSync(backupPath, file.buffer);
    console.log(`Backup saved to disk: ${backupPath}`);
  } catch (diskErr) {
    console.error('Failed to save photo backup to disk:', diskErr.message);
  }

  // 2. Insert into MSSQL Database
  if (dbPool) {
    try {
      const request = dbPool.request();
      request.input('sku', sql.NVarChar(100), sku);
      request.input('marketplace', sql.NVarChar(50), marketplace);
      request.input('photoName', sql.NVarChar(255), filename);
      request.input('photoData', sql.VarBinary(sql.MAX), file.buffer);

      const query = `
        INSERT INTO LduPhotos (Sku, Marketplace, PhotoName, PhotoData)
        VALUES (@sku, @marketplace, @photoName, @photoData);
      `;
      await request.query(query);
      console.log('Record inserted into MSSQL database successfully.');

      return res.status(200).json({
        success: true,
        message: 'Data uploaded and saved to DB and disk successfully',
        filename: filename
      });
    } catch (dbErr) {
      console.error('Failed to insert into MSSQL database:', dbErr.message);
      return res.status(500).json({
        success: false,
        error: `Database upload failed: ${dbErr.message}. File was saved on server disk.`,
        filename: filename
      });
    }
  } else {
    // Database connection was not established (fallback mode)
    return res.status(500).json({
      success: false,
      error: 'Database connection is unavailable. File saved on server disk only.',
      filename: filename
    });
  }
});

// GET Endpoint to check server status
app.get('/api/status', (req, res) => {
  res.status(200).json({
    success: true,
    status: 'online',
    databaseConnected: dbPool !== null,
    timestamp: new Date()
  });
});

// Start the server
app.listen(PORT, '0.0.0.0', async () => {
  console.log(`Backend server is running on http://0.0.0.0:${PORT}`);
  await initDatabase();
});
