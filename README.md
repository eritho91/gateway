# JWT Gateway – Webshop

A REST API Gateway developed with Java and Spring Boot as part of a microservices-based webshop application.

The gateway provides a single entry point for frontend requests and connects the React application to the backend services.

## Overview

The purpose of this service is to simplify communication between the frontend and backend.

Instead of communicating directly with multiple microservices, the frontend sends its requests to the gateway, which forwards them to the correct destination.

The gateway also manages authentication cookies and forwards JWT tokens to services that require authorization.

## Technologies

- Java 21
- Spring Boot 4
- Spring Web MVC
- Java HttpClient
- Maven
- Docker
- GitHub Actions
- Render

## Architecture

The application connects three main backend services:

- **Auth Service** – User registration, login and authentication.
- **Product Service** – Product retrieval and creation.
- **Order Service** – Order creation.

Communication follows this structure:

```text
                  React Frontend
                        |
                        v
                   JWT Gateway
                        |
             +----------+----------+
             |          |          |
             v          v          v
         Auth       Product      Order
        Service     Service     Service
```

## JWT and Cookie Handling

Authentication is based on JWT tokens stored in HttpOnly cookies.

When a user logs in, the gateway forwards the credentials to the authentication service. After successful authentication, the returned JWT is stored in a cookie named `access_token`.

For subsequent protected requests, the gateway reads the token from the cookie and forwards it to the appropriate microservice using the HTTP Authorization header.

```http
Authorization: Bearer <JWT>
```

The gateway does not verify the JWT itself. Token validation and authorization are handled by the receiving microservice.

Logging out removes the authentication cookie from the browser.

## Available Routes

All requests use the `/gateway` prefix.

| HTTP Method | Route | Purpose |
|---|---|---|
| POST | `/auth/register` | Register a user |
| POST | `/auth/login` | Log in and create authentication cookie |
| POST | `/auth/logout` | Remove authentication cookie |
| GET | `/auth/me` | Retrieve authenticated user information |
| GET | `/products` | Retrieve available products |
| GET | `/products/{id}` | Retrieve a product by ID |
| POST | `/products` | Add a new product |
| POST | `/orders` | Submit a new order |

Requests to protected routes return `401 Unauthorized` if the authentication cookie is missing.

## Configuration

Service URLs and cookie behavior are configured through environment variables.

| Variable | Default | Description |
|---|---|---|
| `AUTH_SERVICE_URL` | `http://localhost:9000` | Authentication service address |
| `PRODUCT_SERVICE_URL` | `http://localhost:8080` | Product service address |
| `ORDER_SERVICE_URL` | `http://localhost:8082` | Order service address |
| `COOKIE_SECURE` | `false` | Controls HTTPS-only cookies |
| `COOKIE_SAME_SITE` | `lax` | Controls cross-site cookie behavior |

The gateway listens on port `9003`.

CORS is configured to allow requests from the React frontend, including local development environments and the deployed frontend on Render.

## Getting Started

**Requirements**

- Java 21
- Git
- Running backend microservices

Clone the project:

```bash
git clone https://github.com/eritho91/gateway.git
cd gateway
```

Start the application:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The application will be accessible at:

`http://localhost:9003/gateway`

## Docker and Deployment

The project includes a multi-stage Dockerfile.

Maven is used to compile and package the application, while Eclipse Temurin Java 21 provides the runtime environment.

Build the image:

```bash
docker build -t jwt-gateway .
```

Run the container:

```bash
docker run -p 9003:9003 jwt-gateway
```

The gateway is also deployed on Render.

**Production URL:** https://gateway-wix3.onrender.com

## Continuous Integration

The repository uses GitHub Actions to automatically run tests when a pull request is opened against the `main` branch.

The workflow uses Java 21 and executes:

```bash
mvn test
```

This provides a basic automated check before code changes are merged.

## Code Organization

The application is divided into separate components with specific responsibilities.

| Component | Responsibility |
|---|---|
| `GatewayController` | Receives incoming API requests |
| `GatewayProxyService` | Communicates with backend microservices |
| `CorsConfig` | Defines permitted frontend origins |
| `HttpClientConfig` | Configures the HTTP client |
| `AuthServiceResponse` | Maps authentication service responses |
| `LoginResponse` | Defines the login response sent to the frontend |

## Related Projects

This gateway is one component of a larger webshop application developed using microservices.

- [Authentication Service](https://github.com/AMT94-star/auth-server-group7)
- [Product Service](https://github.com/johan969/product-service)
- [Order Service](https://github.com/eritho91/order-service)
- [Email Service](https://github.com/elquinox/email-microservice)
- [React Frontend](https://github.com/johan969/webshop-frontend)

## About the Project

Developed as part of a collaborative webshop project during the Java Developer program at IT-Högskolan in Stockholm.

My work on the gateway focused on API routing, communication between microservices, JWT forwarding and cookie-based authentication.
