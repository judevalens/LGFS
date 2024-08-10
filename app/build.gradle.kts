/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin application project to get you started.
 * For more details on building Java & JVM projects, please refer to https://docs.gradle.org/8.4/userguide/building_java_projects.html in the Gradle documentation.
 * This project uses @Incubating APIs which are subject to change.
 */

object Versions {
    const val SCALA_BINARY = "2.13"
    const val GRPC_VERSION = "1.57.2"
    const val GRPC_KOTLIN_VERSION = "1.4.0"
    const val PROTOBUF_VERSION = "3.24.1"
}

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    id("com.google.protobuf") version "0.9.4"
    idea
    `java-library`
    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    maven {
        url = uri("https://repo.akka.io/maven")
    }
    google()
}
idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

dependencies {
    // This dependency is used by the application.
    implementation("com.google.guava:guava:32.1.1-jre")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation(platform("com.typesafe.akka:akka-bom_${Versions.SCALA_BINARY}:2.8.5"))
    implementation("com.typesafe.akka:akka-actor-typed_${Versions.SCALA_BINARY}")
    testImplementation("com.typesafe.akka:akka-actor-testkit-typed_${Versions.SCALA_BINARY}")
    implementation("com.typesafe.akka:akka-cluster-typed_${Versions.SCALA_BINARY}")
    implementation("com.typesafe.akka:akka-cluster-tools_${Versions.SCALA_BINARY}")
    implementation("com.typesafe.akka:akka-remote_${Versions.SCALA_BINARY}")
    implementation("com.typesafe.akka:akka-serialization-jackson_${Versions.SCALA_BINARY}")

    implementation("org.json:json:20090211")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
    //protobuf(project(":protos"))

    api("io.grpc:grpc-stub:${Versions.GRPC_VERSION}")
    api("io.grpc:grpc-protobuf:${Versions.GRPC_VERSION}")
    api("com.google.protobuf:protobuf-java-util:${Versions.PROTOBUF_VERSION}")
    api("com.google.protobuf:protobuf-kotlin:${Versions.PROTOBUF_VERSION}")
    api("io.grpc:grpc-kotlin-stub:${Versions.GRPC_KOTLIN_VERSION}")
    implementation("io.grpc:grpc-netty-shaded:1.58.0")
    implementation("commons-cli:commons-cli:1.6.0")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}
sourceSets {
    main {
        proto {
            // In addition to the default 'src/main/proto'
            srcDir("src/main/kotlin/lgfs/api/grpc/protos")
            // In addition to the default '**/*.proto' (use with caution).
            // Using an extension other than 'proto' is NOT recommended,
            // because when proto files are published along with class files, we can
            // only tell the type of a file from its extension.
            include("**/*.protodevel'")
        }
        java {
            setSrcDirs(listOf("src/main/kotlin", "build/generated"))
            resources {
                srcDirs("src/main/resources")
            }

        }
    }
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "lgfs.AppKt"
        )
    }
}

tasks.withType(Copy::class.java) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${Versions.PROTOBUF_VERSION}"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${Versions.GRPC_VERSION}"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${Versions.GRPC_KOTLIN_VERSION}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
    }

}

application {
    // Define the main class for the application.
    mainClass.set("lgfs.AppKt")
    applicationDefaultJvmArgs = listOf("-Djava.util.logging.config.file=/home/jude/Documents/LGFS/app/logging.properties")
}