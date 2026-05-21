const path = require('path');

module.exports = {
  apps: [
    {
      name: 'ldu-photo-backend',
      script: path.join(__dirname, 'server.js'),
      cwd: __dirname,
      instances: 1,
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
