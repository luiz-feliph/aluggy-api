# aluggy-api

REST API for [Aluggy](https://aluggyapp.com.br) — a classifieds platform connecting students to rental properties near university campuses.

## Overview

Aluggy is a web platform focused on university students looking for housing close to their campus. This repository contains the backend, built as a RESTful API that serves the [aluggy-web](https://github.com/luiz-feliph/aluggy-web) client.

## Tech Stack

- **Language:** Java 25
- **Framework:** Spring Boot 4
- **Security:** Spring Security + JWT
- **Database:** PostgreSQL (production) / H2 (development)
- **ORM:** Spring Data JPA + Hibernate
- **Migrations:** Flyway
- **Build tool:** Maven

## Getting Started

### Prerequisites

- Java 25+
- PostgreSQL running locally
- Maven

### Running locally

git clone https://github.com/luiz-feliph/aluggy-api.git
cd aluggy-api
cp .env.example .env
./mvnw spring-boot:run

The API will be available at http://localhost:8080.

## Project Structure

```
src/main/java/com/aluggy/api/
├── entities/
├── controllers/
├── services/
├── repositories/
└── config/
```

The codebase is organized by layer. Each directory groups classes by their technical responsibility within the application.

## Related

- [aluggy-web](https://github.com/luiz-feliph/aluggy-web) — React frontend