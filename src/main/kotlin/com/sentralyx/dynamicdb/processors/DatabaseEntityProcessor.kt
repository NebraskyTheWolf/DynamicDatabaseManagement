package com.sentralyx.dynamicdb.processors

import com.sentralyx.dynamicdb.annotations.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.jvm.throws
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import javax.annotation.processing.*
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import kotlin.math.max

@SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.sentralyx.dynamicdb.annotations.DatabaseEntity", "com.sentralyx.dynamicdb.annotations.Query")
class DatabaseEntityProcessor : AbstractProcessor() {

    private val version: String = "1.1.34"

    /**
     * Processes the annotations by generating database models for each annotated class.
     *
     * @param annotations A set of annotations present in the current round.
     * @param roundEnv The environment for the current processing round.
     * @return True if the annotations were processed successfully.
     */
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(DatabaseEntity::class.java)
        for (element in elementsAnnotatedWith) {
            if (element.kind.isClass) {
                generateDatabaseModel(element)
            }
        }

        val methodsAnnotatedWithQuery = roundEnv.getElementsAnnotatedWith(Query::class.java)
        for (method in methodsAnnotatedWithQuery) {
            if (method.kind == ElementKind.METHOD) {
                generateQueryMethod(method)
            }
        }

        return true
    }

    /**
     * Generates an extension function for the annotated class, which provides access
     * to the corresponding database model class.
     *
     * This extension function allows the annotated class to directly access the
     * generated database model without explicitly creating an instance. It adds a
     * `model()` function to the class, which returns an instance of the generated
     * `${className}DatabaseModel`.
     *
     * For example, if `User` is annotated with `@DatabaseEntity`, this function will
     * generate the following extension function:
     *
     * ```
     * fun User.model(): UserDatabaseModel {
     *     return UserDatabaseModel()
     * }
     * ```
     *
     * This method can then be used to interact with the database model for the class.
     *
     * @param className The name of the annotated class.
     * @param packageName The package name where the annotated class is located.
     * @return A [FunSpec] representing the extension function for the database model.
     */
    private fun generateModelExtensionFunction(className: String, packageName: String): FunSpec {
        return FunSpec.builder("${className.lowercase()}Model")
            .receiver(ClassName(packageName, className))
            .returns(ClassName(packageName, "${className}DatabaseModel"))
            .addStatement("return ${className}DatabaseModel(this)")
            .build()
    }

    /**
     * Generates a database model for the specified entity class.
     *
     * This method creates a Kotlin data class representing the database model,
     * including insert, select, update, and delete functions.
     *
     * @param element The element representing the database entity class.
     */
    @OptIn(DelicateKotlinPoetApi::class)
    private fun generateDatabaseModel(element: Element) {
        val className = element.simpleName.toString()
        val packageName = processingEnv.elementUtils.getPackageOf(element).toString()

        val entityAnnotation = element.getAnnotation(DatabaseEntity::class.java)
        val tableName = entityAnnotation.tableName.ifEmpty { className.lowercase() }
        val fields = element.enclosedElements.filter { it.kind == ElementKind.FIELD }

        val fieldSpecs = fields.map { field ->
            val fieldName = field.simpleName.toString()
            val fieldType = field.asType().asTypeName()

            val columnType = field.getAnnotation(ColumnType::class.java)
                ?: throw IllegalArgumentException("Field $fieldName must have a ColumnType annotation")

            val sqlType = columnType.type.name
            val size = columnType.size

            val returnType = if (columnType.type.isSizeable) {
                if (size > columnType.type.maxSize)
                    throw IllegalArgumentException("Field $fieldName value size must be ${columnType.type.maxSize} or lower. Current size ($size)")
                "$sqlType(${size})"
            } else {
                sqlType
            }

            PropertySpec.builder(fieldName, fieldType)
                .initializer(fieldName)
                .addModifiers(KModifier.PRIVATE)
                .build() to returnType
        }

        val primaryKeyFieldSpec = fieldSpecs.firstOrNull { (propertySpec, _) ->
            val fieldElement = fields.find { it.simpleName.toString() == propertySpec.name }
            fieldElement?.getAnnotation(PrimaryKey::class.java) != null
        }?.first ?: throw IllegalArgumentException("No field annotated with @PrimaryKey found in $className")

        val classBuilder = TypeSpec.classBuilder("${className}DatabaseModel")
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("currentUser", ClassName(packageName, className))
                    .build()
            )
            .addProperty(
                PropertySpec.builder("currentUser", ClassName(packageName, className))
                    .initializer("currentUser")
                    .mutable(true)
                    .build()
            )
            .addFunction(generateInsertFunction(packageName, className, tableName, fieldSpecs))
            .addFunction(generateSelectFunction(packageName, className, tableName, primaryKeyFieldSpec, fieldSpecs))
            .addFunction(generateSelectSelfFunction(packageName, className, tableName, primaryKeyFieldSpec, fieldSpecs))
            .addFunction(generateUpdateFunction(packageName, className, tableName, fieldSpecs, primaryKeyFieldSpec))
            .addFunction(generateDeleteFunction(packageName, className, tableName, primaryKeyFieldSpec))
            .addFunction(generateDeleteUserFunction(packageName, className, tableName, primaryKeyFieldSpec))
            .addFunction(generateCreateTableFunction(tableName = tableName, element = element))

        val kotlinFile = FileSpec.builder(packageName, "${className}DatabaseModel")
            .addImport("com.sentralyx.dynamicdb.connector.MySQLConnector", "getConnection")
            .addImport(ClassName("java.sql", "ResultSet"))
            .addType(classBuilder.build())
            .addFunction(generateModelExtensionFunction(className, packageName))
            .addBodyComment("""
                /**
                 * This class was auto-generated using DynamicDatabaseMenegement Framework
                 * Do not edit this code manually.
                 * Version: %P
                 *
                 * Generates a database model for the specified entity class.
                 *
                 * This method creates a Kotlin data class representing the database model,
                 * including insert, select, update, and delete functions.
                 *
                 * @param element The element representing the database entity class.
                 */
            """.trimIndent(), version)

        kotlinFile.build().writeTo(processingEnv.filer)
    }

    /**
     * Generates an insert function for the database model.
     *
     * @param className The name of the class to insert into the database.
     * @param tableName The name of the database table.
     * @param fields The fields of the database model.
     * @return A function specification for inserting data into the database.
     */
    private fun generateInsertFunction(
        packageName: String,
        className: String,
        tableName: String,
        fields: List<Pair<PropertySpec, String>>
    ): FunSpec {
        val insertQuery =
            "INSERT INTO $tableName (${fields.joinToString { it.first.name }}) VALUES (${fields.joinToString { "?" }})"

        val code = buildCodeBlock {
            addStatement("getConnection().use { connection ->")
            addStatement("    val statement = connection.prepareStatement(%P)", insertQuery)

            fields.forEachIndexed { index, (property, _) ->
                addStatement("    statement.setObject(${index + 1}, obj.${property.name})")
            }

            addStatement("    statement.executeUpdate()")
            addStatement("}") // This will automatically close the connection
        }

        return FunSpec.builder("insert${className}")
            .addParameter("obj", ClassName(packageName, className))
            .addCode(code)
            .throws(SQLException::class)
            .build()
    }

    /**
     * Generates a select function for the database model.
     *
     * @param className The name of the class to select from the database.
     * @param tableName The name of the database table.
     * @param primaryKeyField The primary key field of the database model.
     * @return A function specification for selecting data from the database.
     */
    private fun generateSelectFunction(
        packageName: String,
        className: String,
        tableName: String,
        primaryKeyField: PropertySpec,
        fields: List<Pair<PropertySpec, String>>
    ): FunSpec {
        val selectQuery =
            "SELECT ${fields.joinToString { it.first.name }} FROM $tableName WHERE ${primaryKeyField.name} = ?"

        val code = buildCodeBlock {
            addStatement("getConnection().use { connection ->")
            addStatement("    val preparedStatement = connection.prepareStatement(%P)", selectQuery)
            addStatement("    preparedStatement.setObject(1, id)")
            addStatement("    val resultSet = preparedStatement.executeQuery()")
            beginControlFlow("    if (resultSet.next())")
            val constructorArgs = fields.joinToString(", ") {
                val getter = when (it.first.type) {
                    INT -> "getInt"
                    STRING -> "getString"
                    BOOLEAN -> "getBoolean"
                    FLOAT -> "getFloat"
                    LONG -> "getLong"
                    DOUBLE -> "getDouble"
                    SHORT -> "getShort"
                    else -> when (MySQLType.valueOf(it.second.split("(")[0])) {
                        MySQLType.INT -> "getInt"
                        MySQLType.VARCHAR -> "getString"
                        MySQLType.BOOL, MySQLType.TINYINT -> "getBoolean"
                        MySQLType.FLOAT -> "getFloat"
                        MySQLType.BIGINT -> "getLong"
                        MySQLType.TIMESTAMP -> "getTimestamp"
                        MySQLType.DECIMAL -> "getDouble"
                        MySQLType.JSON -> "getString"

                        // TODO: Add more available types.

                        else -> "getObject"
                    }
                }

                "resultSet.${getter}(\"${it.first.name}\")"
            }
            addStatement("return %T($constructorArgs)", ClassName(packageName, className))
            endControlFlow()

            addStatement("    return null")
            addStatement("}") // This will automatically close the connection
        }

        return FunSpec.builder("select${className}ById")
            .addParameter("id", Any::class) // Specify the type based on your primary key type
            .returns(ClassName(packageName, className).copy(nullable = true))
            .addCode(code)
            .throws(SQLException::class)
            .build()
    }

    private fun generateSelectSelfFunction(
        packageName: String,
        className: String,
        tableName: String,
        primaryKeyField: PropertySpec,
        fields: List<Pair<PropertySpec, String>>
    ): FunSpec {
        val selectQuery =
            "SELECT ${fields.joinToString { it.first.name }} FROM $tableName WHERE ${primaryKeyField.name} = ?"

        val code = buildCodeBlock {
            addStatement("getConnection().use { connection ->")
            addStatement("    val preparedStatement = connection.prepareStatement(%P)", selectQuery)
            addStatement("    preparedStatement.setInt(1, this.currentUser.id)")
            addStatement("    val resultSet = preparedStatement.executeQuery()")
            beginControlFlow("    if (resultSet.next())")
            val constructorArgs = fields.joinToString(", ") {
                val getter = when (it.first.type) {
                    INT -> "getInt"
                    STRING -> "getString"
                    BOOLEAN -> "getBoolean"
                    FLOAT -> "getFloat"
                    LONG -> "getLong"
                    DOUBLE -> "getDouble"
                    SHORT -> "getShort"
                    else -> when (MySQLType.valueOf(it.second.split("(")[0])) {
                        MySQLType.INT -> "getInt"
                        MySQLType.VARCHAR -> "getString"
                        MySQLType.BOOL, MySQLType.TINYINT -> "getBoolean"
                        MySQLType.FLOAT -> "getFloat"
                        MySQLType.BIGINT -> "getLong"
                        MySQLType.TIMESTAMP -> "getTimestamp"
                        MySQLType.DECIMAL -> "getDouble"
                        MySQLType.JSON -> "getString"

                        // TODO: Add more available types.

                        else -> "getObject"
                    }
                }

                "resultSet.${getter}(\"${it.first.name}\")"
            }
            addStatement("return %T($constructorArgs)", ClassName(packageName, className))
            endControlFlow()

            addStatement("    return null")
            addStatement("}") // This will automatically close the connection
        }

        return FunSpec.builder("select${className}")
            .returns(ClassName(packageName, className).copy(nullable = true))
            .addCode(code)
            .throws(SQLException::class)
            .build()
    }

    /**
     * Generates an update function for the database model.
     *
     * @param className The name of the class to update in the database.
     * @param tableName The name of the database table.
     * @param fields The fields of the database model.
     * @param primaryKeyField The primary key field of the database model.
     * @return A function specification for updating data in the database.
     */
    private fun generateUpdateFunction(
        packageName: String,
        className: String,
        tableName: String,
        fields: List<Pair<PropertySpec, String>>,
        primaryKeyField: PropertySpec
    ): FunSpec {
        val setClause = fields.joinToString { "${it.first.name} = ?" }
        val updateQuery = "UPDATE $tableName SET $setClause WHERE ${primaryKeyField.name} = ?"

        val code = buildCodeBlock {
            addStatement("getConnection().use { connection ->")
            addStatement("    val preparedStatement = connection.prepareStatement(%P)", updateQuery)

            fields.forEachIndexed { index, (property, _) ->
                addStatement("    preparedStatement.setObject(${index + 1}, obj.${property.name})")
            }

            addStatement("    preparedStatement.setObject(${fields.size + 1}, obj.${primaryKeyField.name})")
            addStatement("    preparedStatement.executeUpdate()")
            addStatement("}") // This will automatically close the connection
        }

        return FunSpec.builder("update${className}")
            .addParameter("obj", ClassName(packageName, className))
            .addCode(code)
            .throws(SQLException::class)
            .build()
    }

    /**
     * Generates a delete function for the database model.
     *
     * @param className The name of the class to delete from the database.
     * @param tableName The name of the database table.
     * @param primaryKeyField The primary key field of the database model.
     * @return A function specification for deleting data from the database.
     */
    private fun generateDeleteFunction(
        packageName: String,
        className: String,
        tableName: String,
        primaryKeyField: PropertySpec
    ): FunSpec {
        val deleteQuery = "DELETE FROM $tableName WHERE ${primaryKeyField.name} = ?"

        val code = buildCodeBlock {
            addStatement("getConnection().use { connection ->")
            addStatement("    connection.prepareStatement(%P).use { preparedStatement ->", deleteQuery)
            addStatement("        preparedStatement.setObject(1, id)")
            addStatement("        preparedStatement.executeUpdate()")
            addStatement("    }")
            addStatement("}") // This will automatically close the connection
        }

        return FunSpec.builder("delete${className}ById")
            .addParameter("id", Any::class) // Specify the type based on your primary key type
            .addCode(code)
            .throws(SQLException::class)
            .build()
    }

    private fun generateDeleteUserFunction(
        packageName: String,
        className: String,
        tableName: String,
        primaryKeyField: PropertySpec
    ): FunSpec {
        val deleteQuery = "DELETE FROM $tableName WHERE ${primaryKeyField.name} = ?"

        val code = buildCodeBlock {
            addStatement("getConnection().use { connection ->")
            addStatement("    connection.prepareStatement(%P).use { preparedStatement ->", deleteQuery)
            addStatement("        preparedStatement.setInt(1, this.currentUser.id)")
            addStatement("        preparedStatement.executeUpdate()")
            addStatement("    }")
            addStatement("}") // This will automatically close the connection
        }

        return FunSpec.builder("delete${className}")
            .addCode(code)
            .throws(SQLException::class)
            .build()
    }

    /**
     * Generates boilerplate code for executing the custom query specified by @Query.
     * This allows the user to implement logic to handle the ResultSet.
     */
    private fun generateQueryMethod(method: Element) {
        val methodName = method.simpleName.toString()
        val queryAnnotation = method.getAnnotation(Query::class.java)
        val query = queryAnnotation.value

        val className = (method.enclosingElement as TypeElement).simpleName.toString()
        val packageName = processingEnv.elementUtils.getPackageOf(method).toString()

        val fields = method.enclosedElements.filter { it.kind == ElementKind.FIELD }

        val parameterSpec = fields.map { field ->
            val fieldName = field.simpleName.toString()
            val fieldType = field.asType().asTypeName()

            ParameterSpec.builder(fieldName, fieldType)
                .addModifiers(KModifier.PRIVATE)
                .build()
        }

        val funSpec = generateQueryExecutionFunction(packageName, methodName, query, method, parameterSpec)

        val fileSpec = FileSpec.builder(packageName, "${className}QueryExecutor")
            .addImport("com.sentralyx.dynamicdb.connector.MySQLConnector", "getConnection")
            .addFunction(funSpec)
            .build()

        fileSpec.writeTo(processingEnv.filer)
    }

    private fun generateQueryExecutionFunction(
        packageName: String,
        methodName: String,
        query: String,
        method: Element,
        parameterSpec: List<ParameterSpec>
    ): FunSpec {
        val parameters = method as? ExecutableElement ?: throw IllegalArgumentException("Element is not a method")

        val codeBlock = buildCodeBlock {
            addStatement("val connection = getConnection()")
            addStatement("val preparedStatement = connection.prepareStatement(%P)", query)

            parameters.parameters.forEachIndexed { index, param ->
                val paramName = param.simpleName.toString()
                addStatement("preparedStatement.setObject(${index + 1}, $paramName)") // Adjust according to type
            }

            addStatement("val resultSet = preparedStatement.executeQuery()")

            addStatement("// Process resultSet and implement your logic here")
            addStatement("// Example: return a list of mapped objects")
        }

        return FunSpec.builder(methodName)
            .addParameters(parameterSpec)
            .addModifiers(KModifier.PUBLIC)
            .returns(
                List::class.asTypeName().parameterizedBy(
                    ClassName(
                        packageName,
                        (method.enclosingElement as TypeElement).simpleName.toString()
                    )
                )
            )
            .addCode(codeBlock)
            .build()
    }

    // Add the following method in the DatabaseEntityProcessor class

    /**
     * Generates a create table function for the database model.
     *
     * @param className The name of the class to create the table for.
     * @param tableName The name of the database table.
     * @param fields The fields of the database model.
     * @return A function specification for creating the database table.
     */
    private fun generateCreateTableFunction(
        tableName: String,
        element: Element
    ): FunSpec {
        val fields = element.enclosedElements.filter { it.kind == ElementKind.FIELD }
        val fieldsSql = fields.joinToString(", ") { field ->
            val fieldName = field.simpleName.toString()
            val sqlType = field.getAnnotation(ColumnType::class.java)
            val isPrimaryKey = field.getAnnotation(PrimaryKey::class.java) != null
            val isUnique = field.getAnnotation(Unique::class.java) != null

            val baseSqlType = sqlType?.type?.name ?: throw IllegalArgumentException("Field $fieldName must have a SQLType annotation")
            val size = sqlType.size

            val constraints = mutableListOf<String>()
            if (isPrimaryKey) constraints.add("PRIMARY KEY")
            if (isUnique) constraints.add("UNIQUE")

            "$fieldName $baseSqlType($size) ${constraints.joinToString(" ")}".trim()
        }

        val sql = "CREATE TABLE $tableName ($fieldsSql)"

        val code = buildCodeBlock {
            addStatement("getConnection().use { connection ->")

            addStatement("  val meta = connection.metaData")
            addStatement("  val resultSet: ResultSet = meta.getTables(null, null, \"$tableName\", null)")
            beginControlFlow("if (!resultSet.next())")
                addStatement("  connection.createStatement().use { statement ->")
                    addStatement("  statement.executeUpdate(\"$sql\")")
                addStatement("  }")
            endControlFlow()

            addStatement("}")
        }

        return FunSpec.builder("createTable")
            .addCode(code)
            .addComment("""
                /**
                 * This code was auto-generated, DO NOT EDIT THIS METHOD MANUALLY.
                 *
                 * Generates a create table function for the database model.
                 *
                 * @param className The name of the class to create the table for.
                 * @param tableName The name of the database table.
                 * @param fields The fields of the database model.
                 * @return A function specification for creating the database table.
                 */
            """.trimIndent())
            .throws(SQLException::class)
            .build()
    }
}