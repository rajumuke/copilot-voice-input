plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.20.0"
}

group = "com.rajumuke"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    intellijPlatform {
        local("/Applications/IntelliJ IDEA.app")
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
