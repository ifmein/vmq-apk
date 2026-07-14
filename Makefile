.DEFAULT_GOAL := help

GRADLE := ./gradlew
RELEASE_SCRIPT := ./scripts/release.sh
VERSION ?=
REMOTE ?= origin
BUMP ?=
DRY_RUN ?= 0
SKIP_TESTS ?= 0

DEBUG_APK := app/build/outputs/apk/debug/app-debug.apk
RELEASE_APK := app/build/outputs/apk/release/app-release.apk
RELEASE_ARGS := $(if $(filter 1,$(DRY_RUN)),--dry-run,) $(if $(filter 1,$(SKIP_TESTS)),--skip-tests,) $(if $(REMOTE),--remote $(REMOTE),) $(if $(BUMP),--bump $(BUMP),)

.PHONY: help build build-debug build-release clean apk-path version bump release tag adb-refresh

REQUIRE_VERSION = @test -n "$(VERSION)" || (printf '%s\n' "VERSION is required, for example: make $@ VERSION=1.2.3" >&2; exit 1)

help: ## Show available targets
	@awk 'BEGIN {FS = ":.*## "; printf "Targets:\n"} /^[a-zA-Z0-9_.-]+:.*## / {printf "  %-14s %s\n", $$1, $$2} END {printf "\nVariables:\n  VERSION=<x.y.z>\n  REMOTE=origin\n  BUMP=patch|minor|major\n  DRY_RUN=1\n  SKIP_TESTS=1\n"}' $(MAKEFILE_LIST)

build: build-debug ## Build debug APK

build-debug: ## Build debug APK
	$(GRADLE) assembleDebug

build-release: ## Build release APK
	$(GRADLE) assembleRelease

version: ## Print current versionName and versionCode
	$(RELEASE_SCRIPT) current

bump: ## Update versionName and versionCode
	$(REQUIRE_VERSION)
	$(RELEASE_SCRIPT) $@ $(VERSION) $(RELEASE_ARGS)

release: ## Publish release; VERSION optional, auto-bumps when omitted
	bash $(RELEASE_SCRIPT) $(VERSION) $(RELEASE_ARGS)

tag: ## Create annotated git tag v<version>
	$(REQUIRE_VERSION)
	$(RELEASE_SCRIPT) $@ $(VERSION) $(RELEASE_ARGS)

clean: ## Clean Gradle outputs
	$(GRADLE) clean

apk-path: ## Print APK output paths
	@printf '%s\n' $(DEBUG_APK) $(RELEASE_APK)

adb-refresh: ## Reconnect ADB server (kill + restart)
	adb kill-server
	adb start-server
	adb disconnect
