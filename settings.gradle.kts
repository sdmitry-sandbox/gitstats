rootProject.name = "gitstats"

include("domain")
include("web")
include("connector")
include("persistence")
include("app")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            // Versions
            version("java", "17")
            version("spring-boot", "3.4.3")
            version("spring-dependency-management", "1.1.7")
            version("openapi-generator-plugin", "7.11.0")
            version("lombok", "1.18.36")
            version("mapstruct", "1.6.3")
            version("lombok-mapstruct-binding", "0.2.0")
            version("vaadin", "24.6.5")
            version("postgresql", "42.7.5")
            version("liquibase", "4.31.0")
            version("testcontainers", "1.20.5")
            version("jackson-databind-nullable", "0.2.6")
            version("assertj", "3.27.3")
            version("ttddyy-datasource-proxy", "1.10.1")

            // Plugins
            plugin("spring-boot", "org.springframework.boot").versionRef("spring-boot")
            plugin("spring-dependency-management", "io.spring.dependency-management").versionRef("spring-dependency-management")
            plugin("openapi-generator", "org.openapi.generator").versionRef("openapi-generator-plugin")
            plugin("vaadin", "com.vaadin").versionRef("vaadin")

            // BOMs
            library("spring-boot-dependencies", "org.springframework.boot", "spring-boot-dependencies").versionRef("spring-boot")
            library("vaadin-bom", "com.vaadin", "vaadin-bom").versionRef("vaadin")
            library("testcontainers-bom", "org.testcontainers", "testcontainers-bom").versionRef("testcontainers")

            // Spring Boot Starters
            library("spring-boot-starter", "org.springframework.boot", "spring-boot-starter").withoutVersion()
            library("spring-boot-starter-web", "org.springframework.boot", "spring-boot-starter-web").withoutVersion()
            library("spring-boot-starter-data-jpa", "org.springframework.boot", "spring-boot-starter-data-jpa").withoutVersion()
            library("spring-boot-starter-validation", "org.springframework.boot", "spring-boot-starter-validation").withoutVersion()
            library("spring-boot-starter-test", "org.springframework.boot", "spring-boot-starter-test").withoutVersion()

            // Spring
            library("spring-context", "org.springframework", "spring-context").withoutVersion()
            library("spring-tx", "org.springframework", "spring-tx").withoutVersion()

            // Vaadin
            library("vaadin-spring-boot-starter", "com.vaadin", "vaadin-spring-boot-starter").withoutVersion()

            // Logging
            library("slf4j-api", "org.slf4j", "slf4j-api").withoutVersion()

            // Database & Liquibase
            library("postgresql", "org.postgresql", "postgresql").versionRef("postgresql")
            library("liquibase-core", "org.liquibase", "liquibase-core").withoutVersion()

            // Lombok & Mapstruct
            library("lombok", "org.projectlombok", "lombok").versionRef("lombok")
            library("mapstruct", "org.mapstruct", "mapstruct").versionRef("mapstruct")
            library("mapstruct-processor", "org.mapstruct", "mapstruct-processor").versionRef("mapstruct")
            library("lombok-mapstruct-binding", "org.projectlombok", "lombok-mapstruct-binding").versionRef("lombok-mapstruct-binding")

            // OpenAPI & Jackson
            library("jackson-databind", "com.fasterxml.jackson.core", "jackson-databind").withoutVersion()
            library("jackson-databind-nullable", "org.openapitools", "jackson-databind-nullable").versionRef("jackson-databind-nullable")

            // Testcontainers & Testing
            library("testcontainers-postgresql", "org.testcontainers", "postgresql").withoutVersion()
            library("testcontainers-junit-jupiter", "org.testcontainers", "junit-jupiter").withoutVersion()
            library("assertj-core", "org.assertj", "assertj-core").withoutVersion()
            library("ttddyy-datasource-proxy", "net.ttddyy", "datasource-proxy").versionRef("ttddyy-datasource-proxy")
        }
    }
}
