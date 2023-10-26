dependencies {
    implementation(project(":domain"))

    // Otherwise, jackson cannot instantiate kotlin classes without an empty constructor
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
