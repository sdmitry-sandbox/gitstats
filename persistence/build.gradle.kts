description = "persistence"

plugins {
    `java-library`
}

dependencies {
    api(project(":domain"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.liquibase.core)
    implementation(libs.postgresql)
    implementation(libs.mapstruct)

    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.ttddyy.datasource.proxy)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
}
