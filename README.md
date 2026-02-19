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
- **MapStruct**

## Prerequisites

- Java 21 or higher
- Docker and Docker Compose
- Maven 3.8+

## Getting Started

### 1. Start MongoDB

```bash
docker-compose up -d
```

This will start MongoDB on `localhost:27017`.

### 2. Build the application

```bash
mvn clean install
```

### 3. Run the application

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

## API Endpoints

### Create Person

```bash
curl -X POST http://localhost:8080/persons \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Smith",
    "jobTitle": "Software Engineer",
    "hobbies": ["coding", "hiking", "photography"],
    "location": {
      "latitude": 40.7128,
      "longitude": -74.0060
    }
  }'
```

**Response:**
```json
{
  "id": "507f1f77bcf86cd799439011",
  "name": "Alice Smith",
  "jobTitle": "Software Engineer",
  "hobbies": ["coding", "hiking", "photography"],
  "location": {
    "latitude": 40.7128,
    "longitude": -74.006
  },
  "bio": "Meet a Software Engineer who moonlights as coding and hiking like there's no tomorrow!"
}
```

### Update Location

```bash
curl -X PUT http://localhost:8080/persons/507f1f77bcf86cd799439011/location \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 40.7589,
    "longitude": -73.9851
  }'
```

### Find Nearby Persons

```bash
curl "http://localhost:8080/persons/nearby?lat=40.7128&lon=-74.0060&radiusInKm=10"
```

**Response:**
```json
[
  {
    "id": "507f1f77bcf86cd799439011",
    "name": "Alice Smith",
    "jobTitle": "Software Engineer",
    "hobbies": ["coding", "hiking"],
    "location": {
      "latitude": 40.7589,
      "longitude": -73.9851
    },
    "bio": "Meet a Software Engineer...",
    "distanceInKm": 5.2
  }
]
```

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
ai:
  provider: mock  # Options: mock, openai
  openai:
    api-key: YOUR_API_KEY_HERE  # Replace with your OpenAI API key
    model: gpt-3.5-turbo
    max-tokens: 100

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/personfinder
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
│       ├── controller/      # REST controllers
│       ├── domain/          # Domain entities
│       ├── dto/             # Data Transfer Objects
│       ├── exception/       # Exception handling
│       ├── repository/      # MongoDB repositories
│       ├── security/        # Security utilities (input sanitization)
│       ├── service/         # Business logic
│       └── util/            # Utility classes (Haversine, mappers)
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
mvn spring-boot:run
```

### Run Tests Only
```bash
mvn test
```

### Package as JAR
```bash
mvn clean package
java -jar target/PersonsFinder-0.0.1-SNAPSHOT.jar
```

## Documentation

- [AI_LOG.md](AI_LOG.md) - AI usage documentation
- [SECURITY.md](SECURITY.md) - Security considerations and prompt injection mitigation
- [HELP.md](HELP.md) - Spring Boot reference documentation

## License

This project is provided as-is for technical challenge purposes.
