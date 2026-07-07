import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.intellij.platform") version "2.6.0"
    java
    id("maven-publish")
}

fun properties(key: String) = project.findProperty(key).toString()

tasks.wrapper {
    gradleVersion = "8.13"
}

tasks.compileJava {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
}

val isRelease = System.getProperty("release").toBoolean()

group = properties("pluginGroup")
version = properties("pluginVersion")
version = if (isRelease) version else "$version-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("ideaVersion"), useInstaller = false)
        testFramework(TestFrameworkType.Platform)
        bundledPlugins(
            "com.intellij.java",
            "org.intellij.plugins.markdown",
            "org.intellij.intelliLang",
            "com.intellij.properties",
            "com.intellij.modules.json",
            "HtmlTools"
        )
        localPlugin(providers.gradleProperty("jsdecorPluginZip"))
        localPlugin(providers.gradleProperty("gigaideProJar"))
    }
    compileOnly(files(providers.gradleProperty("jsdecorCompileJar")))
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        id = properties("pluginId")
        name = properties("pluginName")
        version = project.version as String
        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }
    }
    buildSearchableOptions = false
}

tasks.named("verifyPluginProjectConfiguration") {
    enabled = false
}
