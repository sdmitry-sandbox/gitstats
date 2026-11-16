description = "domain"

plugins {
    `java-library`
}

dependencies {
    api(libs.spring.context)
    api(libs.spring.tx)
    implementation(libs.slf4j.api)
    implementation(libs.jackson.databind)
}
