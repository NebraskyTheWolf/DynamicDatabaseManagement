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
        url = uri("https://maven.pkg.github.com/NebraskyTheWolf/DynamicDatabaseManagement")
        credentials {
            username = project.findProperty("github.actor") as String? ?: ""
            password = project.findProperty("github.token") as String? ?: ""
        }
    }
}

kapt { /*T ODO: Your configuration */ }

dependencies {
    implementation(kotlin("stdlib"))

    implementation("com.sentralyx:dynamicdb:1.0.23")
    kapt("com.sentralyx:dynamicdb:1.0.23")

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
@DatabaseEntity(tableName = "users")
data class User(
    @PrimaryKey
    val id: Int,
    
    @ColumnType(type = ColumnType.VARCHAR, size = 50)
    @Unique
    val name: String,
    
    @ColumnType(type = ColumnType.INT)
    val age: Int
)
```

2.  Automatic Database Model Generation
The library will generate a database model for your entity during compilation. Ensure that the appropriate annotation processing is configured in your project.

3. Execute Database Operations

With the generated model, you can easily perform CRUD operations as shown in the example below:

```kotlin
fun main() {
    val user = User(id = 1, name = "Alice", age = 30)

    // Insert user into the database
    user.userModel().insert(user)

    // Select user from the database
    val retrievedUser = user.userModel().selectById(1)
    println(retrievedUser)

    // Update user
    user.userModel().update(user.copy(age = 31))

    // Delete user
    user.userModel().delete(1)
}
```

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

## Issue Handling

If you encounter any issues while using the library, please follow these steps:

1. **Check Existing Issues**: Before creating a new issue, please check if your problem has already been reported.
2. **Create a New Issue**: If your issue is not listed, please open a new issue in the repository.
    - **Title**: Use a clear and concise title for your issue.
    - **Description**: Provide a detailed description of the problem, including:
        - Steps to reproduce the issue
        - Expected behavior
        - Actual behavior
        - Any relevant code snippets or error messages
3. **Label Your Issue**: If applicable, add labels to your issue to categorize it (e.g., bug, enhancement, question).
4. **Follow Up**: Be responsive to any questions or requests for clarification from maintainers.

## Error Handling

The library provides robust error handling for all database operations. SQL exceptions encountered during operations are caught and can be handled gracefully, allowing developers to maintain control over error management.

## Contributing

Contributions are highly encouraged! If you want to enhance this library, please follow these steps:

1. **Fork the Repository**: Create your own copy of the repository.
2. **Create a New Branch**: Use a descriptive name for your branch (`git checkout -b feature/YourFeature`).
3. **Make Your Changes**: Implement your feature or fix the bug.
4. **Commit Your Changes**: Keep your commit messages clear and concise (`git commit -m 'Add new feature'`).
5. **Push to Your Branch**: Share your changes with the community (`git push origin feature/YourFeature`).
6. **Open a Pull Request**: Submit your changes for review.

## License

This project is licensed under the MIT License. For more details, see the [LICENSE](LICENSE) file.

## Acknowledgments

- Thanks to the Kotlin community for their continuous support and contributions.
- Inspired by various open-source database management solutions.
