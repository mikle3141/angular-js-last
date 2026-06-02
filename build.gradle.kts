import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.intellij.platform") version "2.6.0"
    java
    antlr
    id("maven-publish")
}

fun properties(key: String) = project.findProperty(key).toString()

val pluginVersion: String by project
val ideaVersion: String by project
val antlr4Version: String by project

group = "antlr"
version = pluginVersion

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
    }
    antlr("org.antlr:antlr4:$antlr4Version") {
        exclude(group = "com.ibm.icu", module = "icu4j")
    }
    implementation("org.antlr:antlr4-intellij-adaptor:0.1")
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
}

tasks.named("verifyPluginProjectConfiguration") {
    enabled = false
}

tasks.named<org.gradle.api.plugins.antlr.AntlrTask>("generateGrammarSource") {
    arguments = arguments + listOf("-package", "org.antlr.jetbrains.sample.parser", "-Xexact-output-dir")
}

//publishing {
//    publications {
//        create<MavenPublication>("maven") {
//            artifactId = properties("pluginId")
//            groupId = project.group as String
//            version = project.version as String
//            artifact("build/distributions/${project.name}-${project.version}.zip") {
//                extension = "zip"
//            }
//        }
//    }
//    repositories {
//        maven {
//            val releaseUrl = "https://nexus-ci.delta.sbrf.ru/repository/maven-lib-release"
//            val snapshotUrl = "https://nexus-ci.delta.sbrf.ru/repository/maven-lib-dev"
//            url = uri(if (isRelease) releaseUrl else snapshotUrl)
//            credentials {
//                username = System.getProperty("gradle.wrapperUser")
//                password = System.getProperty("gradle.wrapperPassword")
//            }
//        }
//    }
//}
//
//tasks.named("publishAllPublicationsToMavenRepository") {
//    dependsOn("buildPlugin")
//}
//tasks.named("publishMavenPublicationToMavenLocal") {
//    dependsOn("buildPlugin")
//}
//tasks.named("publishMavenPublicationToMavenRepository") {
//    dependsOn("buildPlugin")
//}
//tasks.named("publishToMavenLocal") {
//    dependsOn("buildPlugin")
//}
