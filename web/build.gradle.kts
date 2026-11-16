description = "web"

plugins {
    `java-library`
    alias(libs.plugins.vaadin)
}

dependencies {
    api(project(":domain"))

    implementation(libs.vaadin.spring.boot.starter)
    implementation(libs.spring.tx)
    implementation(libs.mapstruct)
}
