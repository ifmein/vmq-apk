APP_ID := com.vone.qrcode
MAIN_ACTIVITY := vmq.ui.main.MainActivity
GRADLE := ./gradlew
RELEASE_SCRIPT := ./scripts/release.sh
ADB ?= adb
DEVICE ?=
HOST ?=
PAIR_PORT ?=
CONNECT_PORT ?=
PAIR_CODE ?=
VERSION ?=
DRY_RUN ?= 0
SKIP_TESTS ?= 0
ADB_ARGS := $(if $(DEVICE),-s $(DEVICE),)
ADB_CMD := $(ADB) $(ADB_ARGS)
DEBUG_APK := app/build/outputs/apk/debug/app-debug.apk
RELEASE_APK := app/build/outputs/apk/release/app-release.apk
RELEASE_ARGS := $(if $(filter 1,$(DRY_RUN)),--dry-run,) $(if $(filter 1,$(SKIP_TESTS)),--skip-tests,)

.PHONY: help build build-debug build-release install install-debug reinstall run launch logs devices pair connect disconnect uninstall clean apk-path version bump release tag

help:
	@printf '%s\n' \
		'Make targets:' \
		'  make build          Build debug APK' \
		'  make build-debug    Build debug APK' \
		'  make build-release  Build release APK' \
		'  make install        Install debug APK to connected device' \
		'  make install-debug  Install debug APK to connected device' \
		'  make reinstall      Uninstall and reinstall debug APK' \
		'  make run            Build, install, and launch the app' \
		'  make launch         Launch the main activity' \
		'  make logs           Show app logs (MainActivity / NeNotificationService)' \
		'  make version        Print current versionName and versionCode' \
		'  make bump VERSION=1.2.3               Update versionName/versionCode' \
		'  make release VERSION=1.2.3            Bump version, test, and build release APK' \
		'  make tag VERSION=1.2.3                Create annotated git tag v1.2.3 on current HEAD' \
		'  make devices        List adb devices' \
		'  make pair           Pair a wireless debugging device' \
		'  make connect        Connect to a wireless debugging device' \
		'  make disconnect     Disconnect adb from all devices' \
		'  make uninstall      Uninstall the app from device' \
		'  make clean          Clean Gradle outputs' \
		'  make apk-path       Print APK output paths' \
		'' \
		'Optional variables:' \
		'  DEVICE=<adb-serial> Use a specific device, for example:' \
		'  make install DEVICE=192.168.20.182:41555' \
		'  VERSION=<x.y.z>     Semantic version for bump/release' \
		'  DRY_RUN=1           Print release actions without changing files' \
		'  SKIP_TESTS=1        Skip unit tests during release' \
		'  HOST=<ip> PAIR_PORT=<port> PAIR_CODE=<code> make pair' \
		'  HOST=<ip> CONNECT_PORT=<port> make connect'

build: build-debug

build-debug:
	$(GRADLE) assembleDebug

build-release:
	$(GRADLE) assembleRelease

install: install-debug

install-debug: build-debug
	$(ADB_CMD) install -r $(DEBUG_APK)

reinstall: build-debug
	$(ADB_CMD) uninstall $(APP_ID) || true
	$(ADB_CMD) install -r $(DEBUG_APK)

run: install-debug launch

launch:
	$(ADB_CMD) shell am start -n $(APP_ID)/$(MAIN_ACTIVITY)

logs:
	$(ADB_CMD) logcat -s MainActivity NeNotificationService

version:
	$(RELEASE_SCRIPT) current

bump:
	@test -n "$(VERSION)" || (printf '%s\n' 'VERSION is required, for example: make bump VERSION=1.2.3' >&2; exit 1)
	$(RELEASE_SCRIPT) bump $(VERSION) $(RELEASE_ARGS)

release:
	@test -n "$(VERSION)" || (printf '%s\n' 'VERSION is required, for example: make release VERSION=1.2.3' >&2; exit 1)
	$(RELEASE_SCRIPT) release $(VERSION) $(RELEASE_ARGS)

tag:
	@test -n "$(VERSION)" || (printf '%s\n' 'VERSION is required, for example: make tag VERSION=1.2.3' >&2; exit 1)
	$(RELEASE_SCRIPT) tag $(VERSION) $(RELEASE_ARGS)

devices:
	$(ADB) devices -l

pair:
	printf '%s\n' '$(PAIR_CODE)' | $(ADB) pair $(HOST):$(PAIR_PORT)

connect:
	$(ADB) connect $(HOST):$(CONNECT_PORT)

disconnect:
	$(ADB) disconnect

uninstall:
	$(ADB_CMD) uninstall $(APP_ID)

clean:
	$(GRADLE) clean

apk-path:
	@printf '%s\n' $(DEBUG_APK) $(RELEASE_APK)
