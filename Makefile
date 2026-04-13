GRADLE := ./gradlew

.PHONY: help setup format check test build clean install lint apk release

help:
	@echo "Common targets:"
	@echo "  make setup     Enable the git pre-commit hook (run once per clone)"
	@echo "  make format    Apply Spotless formatting to Java/Kotlin/XML"
	@echo "  make check     Verify formatting without modifying files"
	@echo "  make test      Run unit tests"
	@echo "  make lint      Run Android lint"
	@echo "  make build     Full debug build (compile + test + lint)"
	@echo "  make apk       Build debug APK"
	@echo "  make release   Build release APK"
	@echo "  make install   Install debug APK on connected device"
	@echo "  make clean     Remove build artefacts"

setup:
	git config core.hooksPath scripts/git-hooks
	@echo "Pre-commit hook enabled. Spotless will auto-format staged files before each commit."

format:
	$(GRADLE) spotlessApply

check:
	$(GRADLE) spotlessCheck

test:
	$(GRADLE) test

lint:
	$(GRADLE) lint

build:
	$(GRADLE) build

apk:
	$(GRADLE) assembleDebug

release:
	$(GRADLE) assembleRelease

install:
	$(GRADLE) installDebug

clean:
	$(GRADLE) clean
