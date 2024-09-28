# Dynamic Database Management Library

![Kotlin Version](https://img.shields.io/badge/Kotlin-1.5%2B-blue.svg) ![License](https://img.shields.io/badge/License-MIT-green.svg)

## Overview
**Dynamic Database Management Library** is a Kotlin library designed to streamline the interaction with SQL databases by automatically generating database models based on Kotlin data classes. This library leverages custom annotations to simplify CRUD (Create, Read, Update, Delete) operations while ensuring robust error handling and resource management.

## Key Features
- **Dynamic Query Generation**: Automatically generates SQL queries based on annotated Kotlin data classes.
- **Custom Annotations**: Use annotations like `@DatabaseEntity`, `@PrimaryKey`, and `@ColumnType` for easy configuration of database schemas.
- **Automatic Resource Management**: Uses Kotlin's resource management capabilities to ensure database connections and statements are properly closed.
- **Robust Error Handling**: Catches and manages SQL exceptions to facilitate smoother development.
- **Integration Friendly**: Works seamlessly with existing Kotlin applications.

---

## Installation

# Gradle x Kotlin DSL

```kotlin
plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("kapt") version "1.9.21"
}

group = "com.example.test"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/NebraskyTheWolf/kDDM")
        credentials {
            username = project.findProperty("github.actor") as String? ?: ""
            password = project.findProperty("github.token") as String? ?: ""
        }
    }
}

kapt { /*TODO: Your configurations */ }

dependencies {
    implementation(kotlin("stdlib"))

    implementation("com.sentralyx:dynamicdb:LATEST")
    kapt("com.sentralyx:dynamicdb:LATEST")

    implementation("mysql:mysql-connector-java:8.0.30")
}

kotlin {
    jvmToolchain(8)
}
```

---

## Getting Started
1. Define Your Data Class

To start using the library, create a Kotlin data class representing your database entity. Utilize the provided annotations for configuration.

```kotlin
@DatabaseEntity(tableName = "payments")
data class Payment(
    @ColumnType(type = MySQLType.INT, size = (11))
    @PrimaryKey var id: Int,

    @ColumnType(type = MySQLType.VARCHAR, size = (160))
    @Unique @NotNull var transactionId: String,

    @ColumnType(type = MySQLType.VARCHAR, size = (80))
    @ForeignKey(
        targetTable = "customers",
        targetName = "id",
        onDelete = ForeignKeyType.CASCADE
    )
    @NotNull var customerId: Int,

    @ColumnType(type = MySQLType.VARCHAR, size = (160))
    @ForeignKey(
        targetTable = "orders",
        targetName = "order_id",
        onDelete = ForeignKeyType.CASCADE
    )
    var orderId: String,

    @ColumnType(type = MySQLType.FLOAT, size = (11))
    @DefaultFloatValue(value = 0f)
    @NotNull var price: Float,

    @ColumnType(type = MySQLType.VARCHAR, size = (60))
    @DefaultStringValue(value = "PENDING")
    var status: String,

    @ColumnType(type = MySQLType.TIMESTAMP)
    var createdAt: Timestamp,

    @ColumnType(type = MySQLType.TIMESTAMP)
    var updatedAt: Timestamp
)
```

2.  Automatic Database Model Generation
The library will generate a database model for your entity during compilation. Ensure that the appropriate annotation processing is configured in your project.

3. Execute Database Operations

With the generated model, you can easily perform CRUD operations as shown in the example below:

```kotlin
fun main() {
    // MySQL and MariaDB is supported.
    MySQLConnector.connect("jdbc:mariadb://localhost:3306/test", "username", "password")

    val payment = Payment(
        id = 1,
        transactionId = UUID.randomUUID().toString(),
        customerId = UUID.randomUUID().toString(),
        orderId = UUID.randomUUID().toString(),
        status = "PENDING",
        price = 10.90F,
        createdAt = Timestamp(System.currentTimeMillis()),
        updatedAt = Timestamp(System.currentTimeMillis())
    )

    payment.paymentModel().createTable()
    payment.paymentModel().insertPayment(payment)

    val selectedPayment = payment.paymentModel().selectPayment()
    println(selectedPayment)
}
```

---

## Custom Annotations

### `@DatabaseEntity`
- **Purpose**: Marks a data class as a database entity.
- **Parameters**:
    - `tableName`: Specifies the name of the corresponding database table.

### `@ColumnType`
- **Purpose**: Configures the SQL type and constraints for a field.
- **Parameters**:
    - `type`: Specifies the SQL type (e.g., STRING, INT).
    - `size`: Defines the size of the column for string types.

### `@PrimaryKey`
- **Purpose**: Identifies a field as the primary key of the database table.

---

## Contributing

Contributions are highly encouraged! If you want to enhance this library, please follow these steps:

1. **Fork the Repository**: Create your own copy of the repository.
2. **Create a New Branch**: Use a descriptive name for your branch (`git checkout -b feature/YourFeature`).
3. **Make Your Changes**: Implement your feature or fix the bug.
4. **Commit Your Changes**: Keep your commit messages clear and concise (`git commit -m 'Add new feature'`).
5. **Push to Your Branch**: Share your changes with the community (`git push origin feature/YourFeature`).
6. **Open a Pull Request**: Submit your changes for review.

---

## License

This project is licensed under the MIT License. For more details, see the [LICENSE](LICENSE) file.

---

## Acknowledgments

- Thanks to the Kotlin community for their continuous support and contributions.
- Inspired by various open-source database management solutions.
