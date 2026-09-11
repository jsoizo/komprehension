plugins {
    kotlin("jvm") version "2.4.20"
    id("com.jsoizo.komprehension")
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.jsoizo.komprehension.sample.MainKt")
}
