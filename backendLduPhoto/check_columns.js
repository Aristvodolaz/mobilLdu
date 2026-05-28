require('dotenv').config();
const sql = require('mssql');
const cfg = {
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  server: process.env.DB_SERVER,
  port: parseInt(process.env.DB_PORT || '59587'),
  database: process.env.DB_NAME,
  options: { encrypt: process.env.DB_ENCRYPT === 'true', trustServerCertificate: true }
};
sql.connect(cfg).then(pool => {
  return pool.request().query("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='LduPhotos' ORDER BY ORDINAL_POSITION");
}).then(r => {
  console.log('Columns in LduPhotos:', r.recordset.map(x => x.COLUMN_NAME).join(', '));
  process.exit(0);
}).catch(e => { console.error('Error:', e.message); process.exit(1); });
