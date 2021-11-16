@file:kotlin.Suppress("unsupported")
plugins {
    kotlin("jvm") version "1.4.0-rc"
    id("org.jetbrains.intellij") version "0.7.2"
    java
}
java.sourceCompatibility = JavaVersion.VERSION_1_8

group = "cn.olange"
version = "1.0.5.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "IC-2020.3.3"
    setPlugins("java")
}
tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    setSinceBuild("173.0")
    setUntilBuild("203.*")
}