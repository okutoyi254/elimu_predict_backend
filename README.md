# Elimu-Predict 

> AI-Driven Student Performance Monitoring & Resource Allocation System

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)](https://spring.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

## Overview
Elimu-Predict is a role-based academic monitoring platform that uses
AI to identify at-risk students individually rather than relying on
class averages. Built with Spring Boot, integrated with a Python ML
microservice and Google Gemini API for intelligent recommendations.

## Tech Stack
| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3 (Java 21) |
| Database | PostgreSQL 16 |
| Security | Spring Security + JWT |
| ML Service | Python Flask + Scikit-Learn |
| AI Suggestions | Google Gemini API |
| Docs | Swagger / OpenAPI |

## Getting Started

### Prerequisites
- Java 21+
- PostgreSQL 16+
- Maven 3.8+
- Python 3.10+ (for ML service)

### Local Setup
```bash
# Clone repo
git clone https://github.com/YOUR_USERNAME/elimu-predict.git
cd elimu-predict

# Configure database
psql -U postgres -c "CREATE DATABASE elimu_predict;"

# Update credentials in src/main/resources/application.properties

# Run
./mvnw spring-boot:run
```

### API Documentation
Once running, visit: http://localhost:8080/swagger-ui.html

## Project Structure
```
src/main/java/com/elimupredict/
├── auth/          # JWT authentication
├── security/      # Spring Security config
├── user/          # User management
├── student/       # Student records
├── marks/         # Marks upload & management
├── subject/       # Subject management
├── ai/            # AI orchestration
├── reports/       # Dashboards & reports
└── audit/         # Audit logging
```

## Modules & Progress
- [x] Sprint 1 — Project setup & configuration
- [x] Sprint 2 — Auth & Security (JWT + Roles)
- [x] Sprint 3 — Core APIs (Students, Subjects, Marks)
- [ ] Sprint 4 — AI Integration (ML + Gemini)
- [ ] Sprint 5 — Reports, Dashboards & Testing

## Team
| Name | Role                                    |
|---|-----------------------------------------|
| Lovingstone Ochieng | Group Leader & Dev Supervision          |
| James Okutoyi | Backend and AI Integration              |
| Herine Adhiambo | Frontend                                |
| Stanley Makhanu | Frontend                                |
| Aaron Mutua | AI model training  & Dashboard Creation |
| Nick Naftali | Testing & Documentation                 |
| Dennis Kipleting | Frontend                                |

## License
MIT License — Masinde Muliro University of Science and Technology
