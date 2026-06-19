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

## Coding Principles (Efficiency & Minimalism)

Claude Code는 코드를 작성할 때 아래 원칙을 엄격히 준수해야 합니다.

1.  **Reuse First**: 새로운 코드를 작성하기 전에 반드시 기존 프로젝트의 코드를 스캔하세요. 유사한 패턴이나 라이브러리가 있다면 이를 재사용하는 것을 최우선으로 합니다.
2.  **Minimize Changes**: 요구사항을 해결하기 위해 필요한 최소한의 코드만 수정하세요. 불필요한 리팩토링이나 과도한 엔지니어링(Over-engineering)을 지양합니다.
3.  **No Redundant Code**: 목적이 불분명하거나 사용되지 않는 불필요한 코드는 절대 작성하지 마세요. 모든 코드는 명확한 이유(Why)와 기능적 근거를 바탕으로 작성되어야 합니다.
4.  **Context-Aware**: 수정 전 항상 관련 파일(`MASTER_PLAN.md`, 기존 도메인 엔티티 등)을 먼저 파악하여, 프로젝트의 일관성을 유지하는 범위 내에서 작업하세요.