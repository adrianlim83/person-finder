# 👥 Person Finder API

A production-ready Spring Boot REST API for managing person profiles with AI-generated bios and geospatial search capabilities.

## Features

- ✅ Create person profiles with AI-generated quirky bios
- ✅ Update person locations
- ✅ Find nearby persons using Haversine distance calculation
- ✅ MongoDB for persistent storage
- ✅ Input sanitization and prompt injection protection
- ✅ Comprehensive validation and error handling
- ✅ Clean architecture (Controller → Service → Repository)

## Tech Stack

- **Java 21**
- **Spring Boot 3.2.0**
- **MongoDB 7.0**
- **Maven**
- **Lombok**

## Prerequisites

- Java 21 or higher
- Docker and Docker Compose
- Maven 3.8+

## Getting Started

### 1. Build the application

```bash
mvn clean install
```

### 2. Set up the following environment variables:
-- `mongodb_host`: Your MongoDB host address
-- `mongodb_username`: Your MongoDB username
-- `mongodb_password`: Your MongoDB password
-- `mongodb_database`: Your MongoDB database name

Note: If you modify the environment variables in docker-compose.yml, 
please do not commit the file. You must also remove the credentials 
after testing before using any internal AI chat tools.

### 3. Run the application

```bash
mvn spring-boot:run {{Including the above environment variables, e.g., -Dmongodb_host=... -Dmongodb_username=... -Dmongodb_password=...}}
```

The API will be available at `http://localhost:8080`.

## API Endpoints

### Create Person

```bash
curl -X POST http://localhost:8080/api/v1/persons \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Smith",
    "email": "alice@example.com",
    "jobTitle": "Software Engineer",
    "hobbies": ["coding", "hiking", "photography"]
  }'
```

**Response:**
```json
{
  "id": 1,
  "name": "Alice Smith",
  "email": "alice@example.com",
  "jobTitle": "Software Engineer",
  "hobbies": ["coding", "hiking", "photography"],
  "bio": "Say hello to a Software Engineer secretly devoted to Coding side projects and Open source contribution like there's no tomorrow!"
}
```

### Update Location

```bash
curl -X PUT http://localhost:8080/api/v1/persons/1/location \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 3.083751,
    "longitude": 101.574040
  }'
```

### Find Nearby Persons

```bash
curl "http://localhost:8080/api/v1/persons/nearby?lat=3.083751&lon=101.574040&radiusInKm=10"
```

**Response:**
```json
[
  {
    "referenceId": 1,
    "latitude": 101.570534,
    "longitude": 3.09296,
    "distanceInKm": 0.0,
    "bio": "Say hello to a Software Engineer with a passion for Coding side projects and Open source contribution in their spare time!"
  },
  {
    "referenceId": 2,
    "latitude": 101.57404,
    "longitude": 3.083751,
    "distanceInKm": 1.0967203802763552,
    "bio": "Say hello to a Software Engineer secretly devoted to Coding side projects and Open source contribution like there's no tomorrow!"
  }
]
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
ai.provider=mock  # Options: mock, openai
ai.openai.api-key=YOUR_API_KEY_HERE
ai.openai.model=gpt-3.5-turbo
ai.openai.max-tokens=100
ai.openai.temperature=0.7
ai.openai.timeout-in-seconds=60
security.input.max-length=500
```

## Running Tests

```bash
mvn test
```

## Project Structure

```
src/
├── main/
│   └── java/com/persons/finder/
│       ├── ai/              # AI service interface and implementations
│       ├── config/          # Spring configuration classes
│       ├── data/            # data presentational objects
│       ├── domain/          # Domain entities, repositories, and services
│       ├── exception/       # Exception handling
│       ├── security/        # Security utilities (input sanitization)
└── test/                    # Unit tests
```

## Security Features

- ✅ Input sanitization for all user inputs
- ✅ Prompt injection detection and mitigation
- ✅ Maximum input length enforcement (500 chars)
- ✅ Control character removal
- ✅ PII protection (no name/location sent to AI)

See [SECURITY.md](SECURITY.md) for details.

## AI Service

The application supports two AI service implementations:

1. **MockAiBioService** (default): Generates deterministic quirky bios for testing
2. **OpenAiBioService**: Integration with OpenAI API (requires API key)

Switch between implementations in `application.yml`:
```yaml
ai:
  provider: mock  # or 'openai'
```

## Docker Commands

```bash
# Start MongoDB
docker-compose up -d

# Stop MongoDB
docker-compose down

# View logs
docker-compose logs -f

# Remove volumes
docker-compose down -v
```

## Development

### Build and Run
```bash
mvn clean install
mvn spring-boot:run {{Including the above environment variables, e.g., -Dmongodb_host=... -Dmongodb_username=... -Dmongodb_password=...}}
```

### Run Tests Only
```bash
mvn test
```

### Package as JAR
```bash
mvn clean package
java -jar target/PersonsFinder-0.0.1-SNAPSHOT.jar {{Including the above environment variables, e.g., -Dmongodb_host=... -Dmongodb_username=... -Dmongodb_password=...}}
```

## Documentation

- [AI_LOG.md](AI_LOG.md) - AI usage documentation
- [SECURITY.md](SECURITY.md) - Security considerations and prompt injection mitigation
- [HELP.md](HELP.md) - Spring Boot reference documentation

## License

This project is provided as-is for technical challenge purposes.
