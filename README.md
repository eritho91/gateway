# JWT Gateway – Webshop

A REST API Gateway developed with Java and Spring Boot as part of our webshop project at IT-Högskolan.

The gateway handles requests from the React frontend and sends them to the different backend services. It also handles JWT authentication using HttpOnly cookies.

## How it works

When the frontend makes a request, it goes through the JWT Gateway instead of contacting the backend services directly.

The gateway receives the request and forwards it to the correct service. We have three main services connected to the gateway. The Auth Service handles login and registration, the Product Service handles products and the Order Service handles orders.

The communication between the services looks like this:

```text
                 React Frontend
                       |
                       v
                   JWT Gateway
                       |
             +---------+----------+
             |         |          |
             v         v          v
           Auth      Product    Order
          Service   Service    Service
```

The gateway also handles JWT tokens with HttpOnly cookies.

When a user logs in, the gateway sends the login information to the Auth Service. If the login is successful, a JWT token is returned and saved in an HttpOnly cookie called `access_token`.

When the user makes another request that needs authentication, the gateway reads the JWT token from the cookie and forwards it to the correct service using the Authorization header.

`Authorization: Bearer <token>`

The gateway doesn't validate the JWT token itself. The other services handle that part.

When the user logs out, the cookie is removed.

The gateway currently has these endpoints:

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/auth/register` | Register a user |
| POST | `/auth/login` | Login |
| POST | `/auth/logout` | Logout |
| GET | `/auth/me` | Get logged in user |
| GET | `/products` | Get all products |
| GET | `/products/{id}` | Get product by ID |
| POST | `/products` | Create a product |
| POST | `/orders` | Create an order |

All endpoints start with `/gateway`.

If a user tries to access a protected endpoint without an authentication cookie, the gateway returns `401 Unauthorized`.

## Technologies

- Java 21
- Spring Boot 4
- Spring Web MVC
- Java HttpClient
- Maven
- Docker
- GitHub Actions
- Render

I used Java HttpClient to send requests from the gateway to the other services.

The gateway uses environment variables for the URLs to the other services. This way the same application can be used locally and on Render without having to change the URLs in the code.

| Variable | Default value |
| --- | --- |
| `AUTH_SERVICE_URL` | `http://localhost:9000` |
| `PRODUCT_SERVICE_URL` | `http://localhost:8080` |
| `ORDER_SERVICE_URL` | `http://localhost:8082` |
| `COOKIE_SECURE` | `false` |
| `COOKIE_SAME_SITE` | `lax` |

I also configured CORS so the React frontend can communicate with the gateway, both locally and on Render.

The gateway runs on port `9003`.

The project has a Dockerfile and is deployed on Render. I also added GitHub Actions to run tests when a pull request is made against the main branch.

**Production URL:** https://gateway-wix3.onrender.com

## About the project

This gateway is part of our webshop project where we built different microservices during the Java Developer program at IT-Högskolan in Stockholm.

My work was mainly on the gateway, connecting the frontend to the backend services and handling the JWT tokens with HttpOnly cookies. I also worked with forwarding requests between the services and getting everything to work together.

The other services in the project are:

- [Auth Service](https://github.com/AMT94-star/auth-server-group7)
- [Product Service](https://github.com/johan969/product-service)
- [Order Service](https://github.com/eritho91/order-service)
- [Email Service](https://github.com/elquinox/email-microservice)
- [React Frontend](https://github.com/johan969/webshop-frontend)
