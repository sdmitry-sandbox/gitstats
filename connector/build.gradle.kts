description = "connector"

plugins {
    `java-library`
    alias(libs.plugins.openapi.generator)
}

val baseJavaPackage: String by rootProject.extra

dependencies {
    api(project(":domain"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.databind.nullable)
    implementation(libs.mapstruct)
    implementation(libs.slf4j.api)
}

val clients = listOf(
    mapOf(
        "name" to "gitlabApi",
        "spec" to "$projectDir/src/main/resources/openapi/gitlab-openapi.yml",
        "package" to "connector.gitlab"
    )
)

val generateTaskNames = mutableListOf<String>()

clients.forEach { client ->
    val taskName = "generate${client["name"]?.replaceFirstChar { it.uppercase() }}Client"
    generateTaskNames.add(taskName)
    tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>(taskName) {
        generatorName.set("java")
        inputSpec.set(client["spec"])
        outputDir.set("${project.layout.buildDirectory.get()}/generated-sources")
        apiPackage.set("$baseJavaPackage.${client["package"]}.client")
        modelPackage.set("$baseJavaPackage.${client["package"]}.model")
        invokerPackage.set("$baseJavaPackage.${client["package"]}.invoker")

        configOptions.set(
            mapOf(
                "useSpringBoot3" to "true",
                "useSwaggerUI" to "false",
                "documentationProvider" to "none",
                "serializationLibrary" to "jackson",
                "library" to "restclient",
                "useBeanValidation" to "false",
                "gradleBuildFile" to "false",
                "openApiNullable" to "false"
            )
        )
    }
}

sourceSets {
    main {
        java {
            srcDir("${project.layout.buildDirectory.get()}/generated-sources/src/main/java")
        }
    }
}

tasks.withType<JavaCompile> {
    dependsOn(generateTaskNames)
}
