#!/bin/bash

# Script to run PostgreSQL-specific tests
# Make sure to give execute permission: chmod +x run-postgres-tests.sh

echo "Running PostgreSQL Integration Tests..."

# Run all PostgreSQL tests with postgres-test profile
mvn test -Dspring.profiles.active=postgres-test \
         -Dtest=Postgres*Test,*PostgresqlTest \
         -DfailIfNoTests=false

# Check if tests passed
if [ $? -eq 0 ]; then
  echo "✅ PostgreSQL Integration Tests PASSED!"
else
  echo "❌ PostgreSQL Integration Tests FAILED!"
  exit 1
fi

# Optional: Generate coverage report for these tests
echo "Generating test coverage report..."
mvn jacoco:report

echo "Done! Coverage report available at: target/site/jacoco/index.html"