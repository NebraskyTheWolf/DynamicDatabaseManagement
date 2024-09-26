plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("kapt") version "1.9.21"
    id("maven-publish")
    id("signing")
}

group = "com.sentralyx.dynamicdb"
version = "1.0-SNAPSHOT"

kapt {
    generateStubs = true
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

tasks.register<Copy>("generateProcessorFile") {
    val processorClassName = "${group}.processors.DatabaseEntityProcessor"

    doLast {
        val resourceDir = "${buildDir}/generated/resources/META-INF/services/"
        val file = File(resourceDir, "javax.annotation.processing.Processor")
        file.parentFile.mkdirs()
        file.writeText(processorClassName)
    }
}

tasks.named("processResources") {
    dependsOn("generateProcessorFile")
}

tasks.named<Jar>("jar") {
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
            version = "1.0.0"

            artifact(tasks["javadocJar"])
            artifact(tasks["sourcesJar"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/NebraskyTheWolf/dynamicdb")
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
