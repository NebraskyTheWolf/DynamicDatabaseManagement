plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("kapt") version "1.9.21"
    id("maven-publish")
    id("signing")
}

group = "com.sentralyx.dynamicdb"
version = "1.0.8"

kapt {
    generateStubs = true
    annotationProcessor( "${group}.processors.DatabaseEntityProcessor")
}

java {
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    implementation(kotlin("stdlib"))

    kapt("com.squareup:kotlinpoet:1.11.0")
    implementation("com.squareup:kotlinpoet:1.11.0")

    implementation("mysql:mysql-connector-java:8.0.30")
}

kotlin {
    jvmToolchain(8)
}

tasks.withType<JavaCompile>().configureEach {
    options.annotationProcessorPath = configurations.kapt.get()
}

tasks.named<Jar>("jar") {
    val processorClassName = "${group}.processors.DatabaseEntityProcessor"

    val resourceDir = "${buildDir}/generated/resources/META-INF/services/"
    val file = File(resourceDir, "javax.annotation.processing.Processor")
    file.parentFile.mkdirs()
    file.writeText(processorClassName)

    from("${buildDir}/generated/resources") {
        include("META-INF/services/**")
    }
}

sourceSets.main {
    resources.srcDir("${buildDir}/generated/resources") // Include generated resources
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])

            groupId = "com.sentralyx"
            artifactId = "dynamicdb"
            version = version

            artifact(tasks["javadocJar"])
            artifact(tasks["sourcesJar"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/NebraskyTheWolf/DynamicDatabaseManagement")
            credentials {
                username = project.findProperty("github.actor") as String? ?: ""
                password = project.findProperty("github.token") as String? ?: ""
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
}
