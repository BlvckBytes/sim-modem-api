plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":modem"))
    implementation(project(":rest"))

    implementation("org.springframework.boot:spring-boot-starter")
}

springBoot {
    mainClass.set("me.blvckbytes.simmodemapi.SimModemApiApplicationKt")
}
