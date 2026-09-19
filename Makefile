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

.PHONY: mobile-run
mobile-run:
	MOBILE_AVD="$(MOBILE_AVD)" apps/mobile/tools/run-emulator.sh

.PHONY: mobile-install
mobile-install:
	DEVICE_SERIAL="$(DEVICE_SERIAL)" apps/mobile/tools/install-device.sh
