const sql = require('mssql');

const config = {
  user: 'sa',
  password: 'icY2eGuyfU',
  server: 'PRM-SRV-MSSQL-01.komus.net',
  port: 59587,
  database: 'SPOe_rc',
  options: {
    encrypt: true,
    trustServerCertificate: true // useful for local/dev connection issues
  }
};

async function test() {
  try {
    console.log('Connecting to MSSQL...');
    let pool = await sql.connect(config);
    console.log('Connected successfully!');
    
    console.log('Querying existing tables...');
    let result = await pool.request().query(`
      SELECT TABLE_NAME 
      FROM INFORMATION_SCHEMA.TABLES 
      WHERE TABLE_TYPE = 'BASE TABLE'
      ORDER BY TABLE_NAME
    `);
    console.log('Tables in database:');
    result.recordset.forEach(row => {
      console.log(` - ${row.TABLE_NAME}`);
    });
    
    await sql.close();
  } catch (err) {
    console.error('Database connection failed:', err);
  }
}

test();
