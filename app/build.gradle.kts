description = "app"

plugins {
    `java`
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":persistence"))
    implementation(project(":connector"))
    implementation(project(":web"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.postgresql)
    implementation(libs.liquibase.core)

    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
}

sourceSets {
    create("it") {
        java {
            srcDir("src/it/java")
        }
        resources {
            srcDir("src/it/resources")
        }
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
}

val itImplementation by configurations.getting {
    extendsFrom(configurations["testImplementation"])
}
val itRuntimeOnly by configurations.getting {
    extendsFrom(configurations["testRuntimeOnly"])
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = sourceSets["it"].output.classesDirs
    classpath = sourceSets["it"].runtimeClasspath
    useJUnitPlatform()
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    dependsOn(tasks.withType<com.vaadin.gradle.VaadinBuildFrontendTask>())
}