plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    id("io.ktor.plugin") version "2.3.3"

}

group = "org.example"
version = "1.0-SNAPSHOT"
val appMainClass = "com.adamratzman.SiteBackendKt"

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.3"

dependencies {
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0-RC")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set(appMainClass)
}

jib {
    container {
        mainClass = appMainClass
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

ktor {
    docker {
        portMappings.set(listOf(
            io.ktor.plugin.features.DockerPortMapping(
                80,
                80,
                io.ktor.plugin.features.DockerPortMappingProtocol.TCP
            )
        ))

        localImageName.set("adamratzman.com-backend")
        imageTag = "1.5"

        externalRegistry.set(
            io.ktor.plugin.features.DockerImageRegistry.dockerHub(
                appName = provider { "adamratzman.com-backend" },
                username = providers.environmentVariable("DockerHubUsername"),
                password = providers.environmentVariable("DockerHubPassword")
            )
        )
    }
}