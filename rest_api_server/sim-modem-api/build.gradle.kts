import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.plugin.SpringBootPlugin

buildscript {
  repositories {
    mavenCentral()
  }
}

plugins {
  id("org.springframework.boot") version "3.1.4" apply false
  id("io.spring.dependency-management") version "1.1.3"

  kotlin("jvm") version "1.8.22"
  kotlin("plugin.spring") version "1.8.22"
}

allprojects {
  group = "me.blvckbytes"
  version = "0.0.1-SNAPSHOT"

  tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict")
      jvmTarget = "17"
    }
  }
}

subprojects {
  repositories {
    mavenCentral()
  }

  apply {
    plugin("org.jetbrains.kotlin.plugin.spring")
    plugin("io.spring.dependency-management")
    plugin("org.jetbrains.kotlin.jvm")
  }

  the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
    imports {
      mavenBom(SpringBootPlugin.BOM_COORDINATES)
    }
  }

  tasks.test {
    useJUnitPlatform()
  }
}