package com.sentralyx.dynamicdb.processors

import com.mysql.cj.MysqlType
import com.sentralyx.dynamicdb.annotations.ColumnType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.jvm.throws
import com.sentralyx.dynamicdb.annotations.DatabaseEntity
import com.sentralyx.dynamicdb.annotations.PrimaryKey
import com.sentralyx.dynamicdb.connector.checkAndCreateTable
import com.sentralyx.dynamicdb.connector.MySQLConnector
import java.sql.SQLException
import javax.annotation.processing.*
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

@SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.sentralyx.dynamicdb.annotations.DatabaseEntity")
class DatabaseEntityProcessor : AbstractProcessor() {

    private val isDebug = System.getenv("dynamicdb.debug").toBoolean()

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

            if (isDebug) {
                println(element.simpleName)
            }

            if (element.kind.isClass) {
                if (isDebug) {
                    println("${element.simpleName} (Pass)")
                }

                generateDatabaseModel(element)
            }
        }

        return true
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

            PropertySpec.builder(fieldName, fieldType)
                .initializer(fieldName)
                .addModifiers(KModifier.PRIVATE)
                .build() to "$sqlType(${size})"
        }

        val primaryKeyFieldSpec = fieldSpecs.firstOrNull { (propertySpec, _) ->
            val fieldElement = fields.find { it.simpleName.toString() == propertySpec.name }
            fieldElement?.getAnnotation(PrimaryKey::class.java) != null
        }?.first ?: throw IllegalArgumentException("No field annotated with @PrimaryKey found in $className")

        if (isDebug) {
            println("Creating class '${className}DatabaseModel'")
        }

        val classBuilder = TypeSpec.classBuilder("${className}DatabaseModel")
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameters(fieldSpecs.map { (prop, _) ->
                        ParameterSpec.builder(prop.name, prop.type).build()
                    })
                    .build()
            )
            .addProperties(fieldSpecs.map { it.first })
            .addFunction(generateInsertFunction(className, tableName, fieldSpecs))
            .addFunction(generateSelectFunction(className, tableName, primaryKeyFieldSpec, fieldSpecs))
            .addFunction(generateUpdateFunction(className, tableName, fieldSpecs, primaryKeyFieldSpec))
            .addFunction(generateDeleteFunction(className, tableName, primaryKeyFieldSpec))

        if (isDebug) {
            println("Class Builder: \n")
            println(classBuilder.toString())
        }

        val kotlinFile = FileSpec.builder(packageName, "${className}DatabaseModel")
            .addType(classBuilder.build())
            .build()

        kotlinFile.writeTo(processingEnv.filer)
    }

    /**
     * Generates an insert function for the database model.
     *
     * @param className The name of the class to insert into the database.
     * @param tableName The name of the database table.
     * @param fields The fields of the database model.
     * @return A function specification for inserting data into the database.
     */
    private fun generateInsertFunction(className: String, tableName: String, fields: List<Pair<PropertySpec, String>>): FunSpec {
        val insertQuery = "INSERT INTO $tableName (${fields.joinToString { it.first.name }}) VALUES (${fields.joinToString { "?" }})"

        val code = buildCodeBlock {
            addStatement("val connection = MySQLConnector.getConnection()")
            addStatement("val statement = connection.prepareStatement(%P)", insertQuery)

            fields.forEachIndexed { index, (property, _) ->
                addStatement("statement.setObject(${index + 1}, obj.${property.name})")
            }

            addStatement("statement.executeUpdate()")
        }

        return FunSpec.builder("insert")
            .addParameter("obj", ClassName("", className))
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
    private fun generateSelectFunction(className: String, tableName: String, primaryKeyField: PropertySpec, fields: List<Pair<PropertySpec, String>>): FunSpec {
        val selectQuery = "SELECT ${fields.joinToString { it.first.name }} FROM $tableName WHERE ${primaryKeyField.name} = ?"

        val code = buildCodeBlock {
            addStatement("val connection = MySQLConnector.getConnection()")
            addStatement("val preparedStatement = connection.prepareStatement(%P)", selectQuery)
            addStatement("preparedStatement.setObject(1, id)")
            addStatement("val resultSet = preparedStatement.executeQuery()")
            beginControlFlow("if (resultSet.next())")
                val constructorArgs = fields.joinToString(", ") {
                    val schemaType = MySQLType.isValidType(it.second)
                    if (schemaType) {
                        val getter = when (MySQLType.valueOf(it.second)) {
                            MySQLType.INT -> "getInt"
                            MySQLType.VARCHAR -> "getString"
                            MySQLType.BOOL, MySQLType.TINYINT -> "getBoolean"
                            MySQLType.FLOAT -> "getFloat"
                            MySQLType.BIGINT -> "getLong"
                            MySQLType.TIMESTAMP -> "getTimestamp"
                            MySQLType.DECIMAL -> "getDouble"
                            MySQLType.JSON -> "getString"

                            // TODO: Add more types...

                            else -> "getObject"
                        }

                        "${it.first.name} = resultSet.${getter}(\"${it.first.name}\")"
                    } else {
                        "${it.first.name} = resultSet.getObject(\"${it.first.name}\")"
                    }
                }
                addStatement("return %T($constructorArgs)", ClassName("", className))
            endControlFlow()

            addStatement("return null")
        }

        return FunSpec.builder("selectById")
            .addParameter("id", Any::class) // Specify the type based on your primary key type
            .returns(ClassName("", className).copy(nullable = true))
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
    private fun generateUpdateFunction(className: String, tableName: String, fields: List<Pair<PropertySpec, String>>, primaryKeyField: PropertySpec): FunSpec {
        val setClause = fields.joinToString { "${it.first.name} = ?" }
        val updateQuery = "UPDATE $tableName SET $setClause WHERE ${primaryKeyField.name} = ?"

        val code = buildCodeBlock {
            addStatement("val connection = MySQLConnector.getConnection()")
            addStatement("val preparedStatement = connection.prepareStatement(%P)", updateQuery)

            fields.forEachIndexed { index, (property, _) ->
                addStatement("preparedStatement.setObject(${index + 1}, obj.${property.name})")
            }

            addStatement("preparedStatement.setObject(${fields.size + 1}, obj.${primaryKeyField.name})")
            addStatement("preparedStatement.executeUpdate()")
        }

        return FunSpec.builder("update")
            .addParameter("obj", ClassName("", className))
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
    private fun generateDeleteFunction(className: String, tableName: String, primaryKeyField: PropertySpec): FunSpec {
        val deleteQuery = "DELETE FROM $tableName WHERE ${primaryKeyField.name} = ?"

        val code = buildCodeBlock {
            addStatement("val connection = MySQLConnector.getConnection()")
            addStatement("connection.prepareStatement(%P).use { preparedStatement ->", deleteQuery)
                addStatement("preparedStatement.setObject(1, id)")
                addStatement("preparedStatement.executeUpdate()")
            addStatement("}")
        }

        return FunSpec.builder("deleteById")
            .addParameter("id", Any::class) // Specify the type based on your primary key type
            .addCode(code)
            .throws(SQLException::class)
            .build()
    }
}
