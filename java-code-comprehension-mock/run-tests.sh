#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
mkdir -p build/classes
javac --release 17 -d build/classes \
  src/main/java/mock/delivery/*.java \
  src/test/java/mock/delivery/DeliveryServiceTest.java
java -cp build/classes mock.delivery.DeliveryServiceTest
