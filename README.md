# Resume Analyzer: Backend

A REST API that parses a resume (PDF or Word), scores its content out of 100 using transparent rules, and returns specific feedback. Built with Spring Boot, Spring Security and MySQL. The web client is in [resume-analyzer-web](https://github.com/Shaurya-Singh6352/resume-analyzer-web).

## What it does

- User registration and login, with passwords hashed using BCrypt
- JWT-based authentication: every resume route requires a valid token
- Accepts a PDF or DOCX upload, tied to the logged-in user
- Detects the real file type from its content, not just the extension
- Extracts the text with Apache Tika
- Scores the resume with rule-based checks and returns strengths, suggestions and missing skills as JSON
- Stores each user's resumes and their extracted text in MySQL, scoped so users can only see their own

## Tech stack

| Area | Tools |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1 (Spring MVC, Spring Data JPA, Spring Security, Validation) |
| Auth | JWT (HS256), BCrypt password hashing |
| Database | MySQL 8, Hibernate |
| Text extraction | Apache Tika |
| Build | Maven |

## How the score works

The score is calculated by plain Java rules in `ScoringService`, so every point can be explained.

| Category | What is checked | Max |
|---|---|---|
| Contact info | Email, phone number, LinkedIn, GitHub | 10 |
| Sections | Education, Experience, Skills and Projects headings | 20 |
| Skills | Recognised technical skills (about 15 gives full marks) | 25 |
| Impact | Bullets that start with action verbs, lines with measurable results, no weak phrases | 30 |
| Length and style | 300 to 800 words, no first-person wording | 15 |

Each failed check adds an item to the `suggestions` list.

## API

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/health` | Public | Health check |
| POST | `/api/auth/register` | Public | Create an account; returns a JWT |
| POST | `/api/auth/login` | Public | Log in; returns a JWT |
| POST | `/api/resumes` | Bearer token | Upload a resume (multipart field `file`); returns the id, word count and a text preview |
| GET | `/api/resumes/{id}/analysis` | Bearer token | Returns the score, category breakdown, strengths, suggestions and missing skills |

Every route except `/api/health` and `/api/auth/**` requires an `Authorization: Bearer <token>` header. A resume can only be read by the user who uploaded it.

Example response of `GET /api/resumes/1/analysis`:

```json
{
  "resumeId": 1,
  "fileName": "my_resume.pdf",
  "overallScore": 78,
  "categories": [
    { "name": "Contact info", "score": 10, "max": 10 },
    { "name": "Impact", "score": 18, "max": 30 }
  ],
  "strengths": ["Complete contact details."],
  "suggestions": ["Only 22% of your lines start with an action verb."],
  "missingKeywords": ["Docker", "Kubernetes"]
}
```

## Run it locally

**Requirements:** JDK 21 or newer (developed on 25) and MySQL 8.

1. Create the database:
```sql
   CREATE DATABASE resume_analyzer;
```
2. Set your MySQL password and a JWT signing secret as environment variables (neither is stored in the repo):
   - Windows (Command Prompt): `set DB_PASSWORD=your_password` and `set JWT_SECRET=a-random-string-at-least-32-characters-long`
   - macOS/Linux: `export DB_PASSWORD=your_password` and `export JWT_SECRET=a-random-string-at-least-32-characters-long`
   - In IntelliJ: Run, Edit Configurations, Environment variables, add both as `KEY=value`
3. Start the app:
```
   mvnw.cmd spring-boot:run      (Windows)
   ./mvnw spring-boot:run        (macOS/Linux)
```
4. Open http://localhost:8080/api/health. You should see `{"status":"UP"}`.

Hibernate creates the tables on first start (`ddl-auto=update`).

## Project structure

```
src/main/java/com/resumeanalyzer/backend/
  controller/   HTTP endpoints (AuthController, HealthController, ResumeController, ApiExceptionHandler)
  service/      Business logic (AuthService, JwtService, ResumeService, ScoringService)
  repository/   Database access (Spring Data JPA)
  model/        JPA entities (User, Resume)
  dto/          JSON request/response shapes
  config/       Security and CORS configuration
```

## Limitations

- The scoring is rule-based: it checks structure and keywords, and cannot judge how well the writing reads.
- Scanned or image-only PDFs are rejected, because there is no OCR.
- Two-column layouts can extract in a jumbled order.

## Roadmap

- [x] Login and registration with Spring Security and JWT
- [ ] Resume history per user
- [ ] Match a resume against a pasted job description
- [ ] AI-generated feedback on wording and clarity