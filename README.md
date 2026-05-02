# Password Manager

A secure password manager built with Java 17, Spring Boot 3.2, JavaFX 17, and H2 database.

## Features

- 🔐 AES-256 encryption with PBKDF2 key derivation
- 🔑 BCrypt-hashed master password
- 🖥️ JavaFX desktop UI
- 🌐 REST API (Spring Boot)
- 💾 Embedded H2 database (file mode)
- 📋 Password generator
- 🔍 Search and filter by service/username/category
- 📤 JSON export/import

## Requirements

- Java 17+
- Maven 3.8+

## Build & Run

### Compile only
```bash
mvn compile
```

### Run tests
```bash
mvn test
```

### Run the application (Spring Boot, headless/server mode)
```bash
mvn spring-boot:run -Djava.awt.headless=true
```

## REST API

### Master Password
| Method | Endpoint              | Description               |
|--------|-----------------------|---------------------------|
| GET    | /api/master/status    | Check vault status        |
| POST   | /api/master/setup     | Create master password    |
| POST   | /api/master/unlock    | Unlock vault              |
| POST   | /api/master/lock      | Lock vault                |
| PUT    | /api/master/change    | Change master password    |
| POST   | /api/master/strength  | Check password strength   |

### Passwords
| Method | Endpoint                       | Description          |
|--------|--------------------------------|----------------------|
| GET    | /api/passwords                 | List all             |
| POST   | /api/passwords                 | Create new           |
| GET    | /api/passwords/{id}            | Get (with decrypt)   |
| PUT    | /api/passwords/{id}            | Update               |
| DELETE | /api/passwords/{id}            | Delete               |
| GET    | /api/passwords/search?q=       | Search               |
| GET    | /api/passwords/category/{cat}  | Filter by category   |
| GET    | /api/passwords/recent          | Recently used        |
| GET    | /api/passwords/export          | Export JSON          |
| POST   | /api/passwords/import          | Import JSON          |

## H2 Console
Access at http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/passwords`
- Username: `sa`
- Password: (empty)