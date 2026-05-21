const path = require('path');

module.exports = {
  apps: [
    {
      name: 'ldu-photo-backend',
      script: path.join(__dirname, 'server.js'),
      cwd: __dirname,
      exec_mode: 'fork',
      autorestart: true,
      watch: false,
      max_memory_restart: '1G',
      time: true,
      merge_logs: true,
      env: {
        NODE_ENV: 'production',
      },
    },
  ],
};
