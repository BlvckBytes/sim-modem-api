dependencies {
    implementation(project(":domain"))

    implementation("org.springframework:spring-context")
    implementation("org.slf4j:slf4j-api")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}