# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NoteDrop (노트드롭) is a Spring Boot backend system for specialty coffee enthusiasts. Core technical challenges:
- Real-time feedback processing and preference recommendations
- High-throughput flash sale / limited product traffic handling (선착순 트래픽 제어)

## Tech Stack

- **Java 21** / **Spring Boot 4.1.0**
- **Spring Data JPA** + **MySQL**
- **Redis** — planned for caching and queue
- **Docker / AWS** — planned for deployment
- **Lombok** — used for boilerplate reduction throughout

## Common Commands

```bash
# Build
./gradlew build

# Run application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.notedrop.notedrop.SomeTest"

# Clean build
./gradlew clean build
```

## Package Structure

Root package: `com.notedrop.notedrop`

The project is in its initial stage. New code should follow standard Spring Boot layering: `controller` → `service` → `repository` → `entity`, all under the root package.

## Configuration

`src/main/resources/application.properties` currently only sets `spring.application.name=notedrop`. MySQL datasource and Redis connection properties will need to be added as those integrations are built out.

## Git Commit Rules

- Always prefix the commit message with a standard Gitmoji that matches the nature of the changes.
- Write the commit summary clearly in Korean after the Gitmoji.