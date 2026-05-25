module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',

  // Only collect tests from src/. The lib/ directory holds compiled output
  // and (when present) contains duplicate compiled .test.js files. Letting
  // Jest run both source AND compiled tests doubles the suite size and can
  // execute stale code after refactors. We test the TypeScript source.
  roots: ['<rootDir>/src'],
  testMatch: ['<rootDir>/src/**/*.test.ts'],
};
