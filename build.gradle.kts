plugins {
    idea
    java
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.openapi.generator) apply false
    alias(libs.plugins.vaadin) apply false
}

group = "io.cx.gitstats"
version = "0.0.1-SNAPSHOT"

val baseJavaPackage by extra("io.cx.gitstats")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

allprojects {
    repositories {
        maven {
            url = uri("https://nxs-1.int.ops.cldx.ru/repository/maven/")
            isAllowInsecureProtocol = true
        }
        mavenLocal()
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")


    group = rootProject.group
    version = rootProject.version

    val generatedSourcesPath by extra { file("${project.projectDir}/build/generated-sources") }


    dependencies {
        implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
        implementation(platform("com.vaadin:vaadin-bom:${rootProject.libs.versions.vaadin.get()}"))
        implementation(platform("org.testcontainers:testcontainers-bom:${rootProject.libs.versions.testcontainers.get()}"))

        compileOnly(rootProject.libs.lombok)
        annotationProcessor(rootProject.libs.lombok)
        annotationProcessor(rootProject.libs.mapstruct.processor)
        annotationProcessor(rootProject.libs.lombok.mapstruct.binding)

        testCompileOnly(rootProject.libs.lombok)
        testAnnotationProcessor(rootProject.libs.lombok)
        testAnnotationProcessor(rootProject.libs.mapstruct.processor)

        testImplementation(rootProject.libs.assertj.core)
        testImplementation(rootProject.libs.spring.boot.starter.test)
    }

    java {
        sourceCompatibility = rootProject.java.sourceCompatibility
        targetCompatibility = rootProject.java.targetCompatibility
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(
            listOf(
                "-parameters",
                "-Amapstruct.defaultComponentModel=spring",
                "-Amapstruct.unmappedTargetPolicy=IGNORE"
            )
        )
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
