GRADLE := ./gradlew

.PHONY: help format check test build clean install lint apk release bump export-therion export-svg

help:
	@echo "Common targets:"
	@echo "  make format    Apply Spotless formatting to Java/Kotlin/XML"
	@echo "  make check     Verify formatting without modifying files"
	@echo "  make test      Run unit tests"
	@echo "  make lint      Run Android lint"
	@echo "  make build     Full debug build (compile + test + lint)"
	@echo "  make apk       Build debug APK"
	@echo "  make release   Build release APK"
	@echo "  make install   Install debug APK on connected device"
	@echo "  make bump      Bump patch version, commit, and tag a release"
	@echo "  make export-therion  Dump a Therion bundle to app/build/exports/example/"
	@echo "  make export-svg      Dump SVG plan + EE to app/build/exports/example/"
	@echo "  make clean     Remove build artefacts"

format:
	$(GRADLE) spotlessApply

check:
	$(GRADLE) spotlessCheck

test: check
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

bump:
	python3 scripts/bump.py

export-therion:
	$(GRADLE) :app:exportTherionFixtures

export-svg:
	$(GRADLE) :app:exportSvgFixtures

clean:
	$(GRADLE) clean
