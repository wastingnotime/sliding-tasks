SHELL := /usr/bin/env bash

# WNT mobile adapter shortcuts
.PHONY: mobile-test
mobile-test:
	cd apps/mobile && ./gradlew test

.PHONY: mobile-build
mobile-build:
	cd apps/mobile && ./gradlew assembleDebug

.PHONY: mobile-apk
mobile-apk:
	cd apps/mobile && ./gradlew --no-daemon testDebugUnitTest assembleDebug
