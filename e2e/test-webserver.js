const { defineConfig } = require('@playwright/test');
console.log(defineConfig({
  webServer: [{ command: 'echo 1', port: 1234 }, { command: 'echo 2', port: 5678 }]
}));
