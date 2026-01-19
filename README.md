# E-Commerce Backend API

## Overview

A Spring Boot-based REST API for minimal e-commerce functionality with MongoDB persistence and Razorpay payment integration. The system handles product management, shopping carts, order processing, and payment workflows through webhook callbacks.

---

## Implementation Summary

This project implements a backend API with the following components:

### Core Functionality

1. **Product Management**
   - CRUD operations for products
   - Product catalog with name, description, price, stock quantity
   - Database: MongoDB collection `products`

2. **Shopping Cart**
   - User-scoped cart storage
   - Add items with quantity tracking
   - Display cart with embedded product details
   - Clear cart operations
   - Database: MongoDB collection `cart_items`

3. **Order Processing**
   - Create orders from cart contents
   - Automatic total amount calculation
   - Inventory management - stock deduction on order creation
   - Order item persistence with frozen prices
   - Cart auto-clearing after order creation
   - Database: MongoDB collections `orders`, `order_items`

4. **Payment Integration**
   - Razorpay SDK integration (version 1.4.5)
   - Payment order creation and tracking
   - Webhook endpoint for payment callbacks
   - Order status state transitions: CREATED → PAID/FAILED/CANCELLED
   - Database: MongoDB collection `payments`

5. **Extended Features**
   - Order history retrieval by user
   - Order cancellation with stock restoration
   - Product search by name (case-insensitive)

---

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Runtime | Java | 21+ |
| Framework | Spring Boot | 3.2+ |
| Web | Spring MVC | 3.2+ |
| Database | MongoDB | 5.0+ |
| ORM | Spring Data MongoDB | 3.2+ |
| Payment SDK | Razorpay Java | 1.4.5 |
| Build | Maven | 3.8+ |
| Utilities | Lombok | 1.18+ |

---

## Architecture

### System Components

```
┌────────────────────────────────────────────────────────────┐
│                  HTTP REST Client                          │
│                   (Postman/API)                            │
└────────────────┬─────────────────────────────────────────┘
                 │
                 ↓
┌────────────────────────────────────────────────────────────┐
│           Spring Boot Application (Port 8080)              │
├────────────────────────────────────────────────────────────┤
│ Controllers                                                │
│  - ProductController                                       │
│  - CartController                                          │
│  - OrderController                                         │
│  - PaymentController                                       │
│  - PaymentWebhookController                                │
├────────────────────────────────────────────────────────────┤
│ Service Layer (Business Logic)                             │
│  - ProductService                                          │
│  - CartService                                             │
│  - OrderService                                            │
│  - PaymentService                                          │
├────────────────────────────────────────────────────────────┤
│ Repository Layer (Data Access)                             │
│  - UserRepository                                          │
│  - ProductRepository                                       │
│  - CartRepository                                          │
│  - OrderRepository                                         │
│  - OrderItemRepository                                     │
│  - PaymentRepository                                       │
├────────────────────────────────────────────────────────────┤
│ Data Models (MongoDB Documents)                            │
│  - User, Product, CartItem, Order, OrderItem, Payment     │
└────────────────┬─────────────────────────────────────────┘
                 │
        ┌────────┴────────┬──────────────┐
        ↓                 ↓              ↓
    MongoDB          Razorpay SDK    Configuration
    (Database)       (Payments)      (RazorpayConfig)
```

### Data Model

**Entity Relationships:**

```
USER (1) ─── (N) CART_ITEM
USER (1) ─── (N) ORDER ─── (1) PAYMENT
                   │
                   └─── (N) ORDER_ITEM ─── (1) PRODUCT

PRODUCT (1) ─── (N) CART_ITEM
PRODUCT (1) ─── (N) ORDER_ITEM
```

**Database Collections:**

- **users** - User accounts and metadata
- **products** - Product catalog with pricing and inventory
- **cart_items** - Shopping cart items per user
- **orders** - Order records with status and timestamps
- **order_items** - Line items in orders (denormalized prices)
- **payments** - Payment records with Razorpay references

---

## API Endpoints

### Product Endpoints

| Method | Path | Function |
|--------|------|----------|
| POST | `/api/products` | Create product |
| GET | `/api/products` | List all products |
| GET | `/api/products/{id}` | Get product by ID |
| PUT | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Delete product |
| GET | `/api/products/search?q={name}` | Search products |

**Request Example:**
```bash
POST /api/products
Content-Type: application/json

{
  "name": "Gaming Laptop",
  "description": "High-performance laptop",
  "price": 50000.0,
  "stock": 10
}
```

### Cart Endpoints

| Method | Path | Function |
|--------|------|----------|
| POST | `/api/cart/add` | Add item to cart |
| GET | `/api/cart/{userId}` | Get user's cart |
| DELETE | `/api/cart/{userId}/clear` | Clear cart |
| PUT | `/api/cart/{cartItemId}` | Update quantity |
| DELETE | `/api/cart/item/{cartItemId}` | Remove item |
| GET | `/api/cart/{userId}/total` | Get cart total |

**Request Example:**
```bash
POST /api/cart/add
Content-Type: application/json

{
  "userId": "user123",
  "productId": "prod_id",
  "quantity": 2
}
```

### Order Endpoints

| Method | Path | Function |
|--------|------|----------|
| POST | `/api/orders` | Create order from cart |
| GET | `/api/orders/{orderId}` | Get order details |
| GET | `/api/orders/user/{userId}` | Get user's orders |
| POST | `/api/orders/{orderId}/cancel` | Cancel order |

**Request Example:**
```bash
POST /api/orders
Content-Type: application/json

{
  "userId": "user123"
}
```

**Response:**
```json
{
  "id": "order_id",
  "userId": "user123",
  "totalAmount": 100000.0,
  "status": "CREATED",
  "items": [
    {
      "productId": "prod_id",
      "quantity": 2,
      "price": 50000.0
    }
  ]
}
```

### Payment Endpoints

| Method | Path | Function |
|--------|------|----------|
| POST | `/api/payments/create` | Initiate payment |
| GET | `/api/payments/{paymentId}` | Get payment details |
| POST | `/api/webhooks/payment` | Razorpay webhook |

**Create Payment Request:**
```bash
POST /api/payments/create
Content-Type: application/json

{
  "orderId": "order_id",
  "amount": 100000
}
```

**Webhook Callback:**
```bash
POST /api/webhooks/payment
Content-Type: application/json

{
  "event": "payment.captured",
  "payload": {
    "payment": {
      "id": "pay_id",
      "order_id": "razorpay_order_id",
      "status": "captured"
    }
  }
}
```

---

## Order Flow

### Transaction Sequence

1. **Create Product**
   ```
   POST /api/products → Product saved to MongoDB
   ```

2. **Add to Cart**
   ```
   POST /api/cart/add
   → Validate product exists
   → Check stock availability
   → Create/update CartItem
   → Save to cart_items collection
   ```

3. **View Cart**
   ```
   GET /api/cart/{userId}
   → Retrieve CartItems for user
   → Populate product details
   → Return cart with items
   ```

4. **Create Order**
   ```
   POST /api/orders
   → Fetch CartItems for user
   → Validate all items have stock
   → Calculate total amount
   → Create Order document (status: CREATED)
   → Create OrderItem entries
   → Decrement product stock
   → Clear CartItems for user
   → Return Order
   ```

5. **Initiate Payment**
   ```
   POST /api/payments/create
   → Validate Order exists and status is CREATED
   → Call Razorpay SDK to create payment order
   → Create Payment document (status: PENDING)
   → Return Payment with razorpayOrderId
   ```

6. **Process Payment (Razorpay)**
   ```
   [External: User completes payment on Razorpay]
   → Razorpay processes payment
   → Razorpay sends webhook callback
   ```

7. **Handle Webhook**
   ```
   POST /api/webhooks/payment
   → Parse Razorpay payload
   → Locate Payment and Order
   → If payment.captured:
     → Update Payment status: SUCCESS
     → Update Order status: PAID
   → If payment.failed:
     → Update Payment status: FAILED
     → Update Order status: FAILED
   ```

8. **Verify Order**
   ```
   GET /api/orders/{orderId}
   → Retrieve Order with embedded payment details
   → Status: PAID (if payment successful)
   ```

---

## Key Business Logic

### Stock Management
- Stock checked during cart operations (validation only)
- Stock decremented atomically during order creation
- Stock restored during order cancellation

### Order Status Transitions
```
CREATED ─────────→ PAID
    ├────────────→ FAILED
    └────────────→ CANCELLED
```

### Cart Behavior
- Per-user cart with single instance
- Same product: quantity updated instead of duplicated
- Auto-cleared when order created
- Manual clear available

### Payment Validation
- Payment creation only for CREATED orders
- Duplicate payment attempts prevented
- Webhook handles asynchronous status updates

---

## Setup Instructions

### Prerequisites
- Java 21+
- Maven 3.8+
- MongoDB 5.0+ (local or cloud)
- Razorpay account (test mode)

### Configuration

**Step 1: MongoDB Setup**
```bash
# Local
mongod

# Docker
docker run -d -p 27017:27017 --name mongo mongo

# Atlas (cloud)
# Get connection string from MongoDB Atlas dashboard
```

**Step 2: Razorpay Credentials**
1. Login to Razorpay Dashboard
2. Navigate to Settings → API Keys
3. Get Test Mode Key ID and Secret

**Step 3: Application Configuration**

Update `src/main/resources/application.yml`:
```yaml
server:
  port: 8080

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/ecommerce_db

razorpay:
  key-id: rzp_test_XXXXX
  key-secret: XXXXX
```

### Build and Run

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Verify
curl http://localhost:8080/api/products
```

---

## Testing Workflow

### Test Sequence

1. **Create Product**
   ```bash
   POST http://localhost:8080/api/products
   {"name": "Laptop", "price": 50000.0, "stock": 10}
   ```

2. **Add to Cart**
   ```bash
   POST http://localhost:8080/api/cart/add
   {"userId": "user1", "productId": "<prod_id>", "quantity": 2}
   ```

3. **View Cart**
   ```bash
   GET http://localhost:8080/api/cart/user1
   ```

4. **Create Order**
   ```bash
   POST http://localhost:8080/api/orders
   {"userId": "user1"}
   ```

5. **Create Payment**
   ```bash
   POST http://localhost:8080/api/payments/create
   {"orderId": "<order_id>", "amount": 100000}
   ```

6. **Simulate Webhook**
   ```bash
   POST http://localhost:8080/api/webhooks/payment
   {
     "event": "payment.captured",
     "payload": {
       "payment": {"id": "pay_id", "order_id": "<razorpay_id>", "status": "captured"}
     }
   }
   ```

7. **Verify Order Status**
   ```bash
   GET http://localhost:8080/api/orders/<order_id>
   ```
   Expected: `status: PAID`, `payment.status: SUCCESS`

---

## Project Structure

```
src/main/java/com/example/in_class_project/
├── controller/
│   ├── ProductController.java
│   ├── CartController.java
│   ├── OrderController.java
│   ├── PaymentController.java
│   └── PaymentWebhookController.java
├── service/
│   ├── ProductService.java
│   ├── CartService.java
│   ├── OrderService.java
│   └── PaymentService.java
├── repository/
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── CartRepository.java
│   ├── OrderRepository.java
│   ├── OrderItemRepository.java
│   └── PaymentRepository.java
├── model/
│   ├── User.java
│   ├── Product.java
│   ├── CartItem.java
│   ├── Order.java
│   ├── OrderItem.java
│   └── Payment.java
├── dto/
│   ├── AddToCartRequest.java
│   ├── CreateOrderRequest.java
│   └── PaymentRequest.java
├── config/
│   ├── RazorpayConfig.java
│   └── RestTemplateConfig.java
└── InClassProjectApplication.java

src/main/resources/
└── application.yml

pom.xml
```

---

## Entity Models

### User
```java
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String username;
    private String email;
    private String role;
}
```

### Product
```java
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
}
```

### CartItem
```java
@Document(collection = "cart_items")
public class CartItem {
    @Id
    private String id;
    private String userId;
    private String productId;
    private Integer quantity;
    @Transient
    private Product product;
}
```

### Order
```java
@Document(collection = "orders")
public class Order {
    @Id
    private String id;
    private String userId;
    private Double totalAmount;
    private String status;  // CREATED, PAID, FAILED, CANCELLED
    private Instant createdAt;
    @Transient
    private List<OrderItem> items;
    @Transient
    private Payment payment;
}
```

### OrderItem
```java
@Document(collection = "order_items")
public class OrderItem {
    @Id
    private String id;
    private String orderId;
    private String productId;
    private Integer quantity;
    private Double price;  // Frozen at order time
}
```

### Payment
```java
@Document(collection = "payments")
public class Payment {
    @Id
    private String id;
    private String orderId;
    private Double amount;
    private String status;  // PENDING, SUCCESS, FAILED
    private String paymentId;  // Razorpay payment ID
    private String razorpayOrderId;  // Razorpay order ID
    private Instant createdAt;
}
```

---

## Implementation Details

### Product Service
- Creates products with validation
- Lists all products from `products` collection
- Provides search with MongoDB regex (case-insensitive)
- Updates and deletes products

### Cart Service
- Per-user cart management
- Stock validation during add
- Quantity update logic
- Cart retrieval with product embedding
- Cart clearing

### Order Service
- Validates cart before order creation
- Calculates total from product prices
- Creates Order + OrderItem documents
- Updates product stock atomically
- Handles order retrieval
- Implements order cancellation with stock restoration
- Supports order history queries

### Payment Service
- Creates Razorpay orders via SDK
- Stores payment records
- Handles webhook processing
- Updates order status on payment confirmation
- Manages order state transitions

### Webhook Handler
- Receives Razorpay callbacks
- Parses payment event payloads
- Locates corresponding Payment and Order
- Updates statuses based on payment result
- Handles payment.captured and payment.failed events

---

## Error Handling

**HTTP Status Codes:**
- 200: Successful operation
- 400: Bad request (invalid input, empty cart, insufficient stock)
- 404: Resource not found
- 409: Conflict (stock unavailable)
- 500: Server error

**Error Response Format:**
```json
{
  "error": "Descriptive error message",
  "timestamp": "ISO-8601 timestamp",
  "status": 400
}
```

---

## Known Constraints

1. No authentication/authorization implemented
2. Webhook signature verification not implemented
3. No API rate limiting
4. Minimal input validation
5. No database transactions across operations
6. No caching layer

---

## Verification Checklist

- [x] Product CRUD operations
- [x] Shopping cart add/view/clear
- [x] Order creation with stock deduction
- [x] Payment order creation via Razorpay SDK
- [x] Webhook endpoint receives callbacks
- [x] Order status updates on payment
- [x] Order history retrieval
- [x] Order cancellation with stock restore
- [x] Product search implementation
- [x] MongoDB persistence
- [x] API endpoints testable via Postman

---

## References

- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Spring Data MongoDB: https://spring.io/projects/spring-data-mongodb
- Razorpay Java SDK: https://github.com/razorpay/razorpay-java
- MongoDB Documentation: https://docs.mongodb.com

---

**Date:** January 19, 2026
**Language:** Java 21
**Build Tool:** Maven 3.8+
