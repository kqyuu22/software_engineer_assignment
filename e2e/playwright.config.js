const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './tests',
  fullyParallel: true,
  retries: 1,
  workers: 1,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:18080',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  // This automatically starts your Spring Boot server before running the tests!
  webServer: {
    command: 'cd .. && mvn spring-boot:run',
    url: 'http://localhost:18080',
    reuseExistingServer: true,
    timeout: 120000,
  },
});
