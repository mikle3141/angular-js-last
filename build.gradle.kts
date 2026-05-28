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
val oscToken: String = System.getProperty("gradle.wrapperOscToken")
val mavenUser: String = System.getProperty("gradle.wrapperUser")
val mavenPassword: String = System.getProperty("gradle.wrapperPassword")

group = properties("pluginGroup")
version = properties("pluginVersion")
version = if (isRelease) version else "$version-SNAPSHOT"

repositories {
    maven {
        name = "sberosc-intellij-repository-releases"
        url = uri("https://sberosc.sigma.sbrf.ru/repo/maven/jetbrains_intellij_repository/releases")
        credentials {
            username = "token"
            password = oscToken
        }
    }
    maven {
        name = "public"
        url = uri("https://nexus-ci.delta.sbrf.ru/repository/public/")
        credentials {
            username = mavenUser
            password = mavenPassword
        }
    }
    maven {
        name = "central"
        url = uri("https://nexus-ci.delta.sbrf.ru/repository/central/")
        credentials {
            username = mavenUser
            password = mavenPassword
        }
        content {
            includeGroup("com.google.code.gson")
        }
    }
    maven {
        name = "maven-lib-int"
        url = uri("https://nexus-ci.delta.sbrf.ru/repository/maven-lib-int")
        credentials {
            username = mavenUser
            password = mavenPassword
        }
        metadataSources {
            mavenPom()
            gradleMetadata()
        }
    }
    maven {
        name = "maven-sberosc-cache"
        url = uri("https://nexus-ci.delta.sbrf.ru/repository/maven-sberosc-cache")
        credentials {
            username = mavenUser
            password = mavenPassword
        }
        metadataSources {
            mavenPom()
            gradleMetadata()
        }
    }
    maven {
        name = "maven-sberosc-cache-intellij-dependencies"
        url = uri("https://nexus-ci.delta.sbrf.ru/repository/maven-sberosc-cache/intellij-dependencies")
        credentials {
            username = mavenUser
            password = mavenPassword
        }
        metadataSources {
            mavenPom()
            gradleMetadata()
        }
    }
    maven {
        name = "sberosc-intellij-dependencies"
        url = uri("https://sberosc.sigma.sbrf.ru/repo/maven/jetbrains_redirect/intellij-dependencies/")
        credentials {
            username = "token"
            password = oscToken
        }
    }
    maven {
        name = "sberosc-central"
        url = uri("https://sberosc.sigma.sbrf.ru/repo/maven/central/")
        credentials {
            username = "token"
            password = oscToken
        }
    }
    maven {
        name = "sberosc-intellij-repository"
        url = uri("https://sberosc.sigma.sbrf.ru/repo/maven/jetbrains_intellij_repository/")
        credentials {
            username = "token"
            password = oscToken
        }
    }

    intellijPlatform {
        localPlatformArtifacts()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("ideaVersion"), useInstaller = false)
    }
    antlr("org.antlr:antlr4:$antlr4Version") {
        exclude(group = "com.ibm.icu", module = "icu4j")
    }
//    implementation("org.antlr:antlr4-intellij-adaptor:0.1")
    implementation(files("libs/antlr4-intellij-adaptor-0.2.0.jar"))
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = properties("pluginId")
            groupId = project.group as String
            version = project.version as String
            artifact("build/distributions/${project.name}-${project.version}.zip") {
                extension = "zip"
            }
        }
    }
    repositories {
        maven {
            val releaseUrl = "https://nexus-ci.delta.sbrf.ru/repository/maven-lib-release"
            val snapshotUrl = "https://nexus-ci.delta.sbrf.ru/repository/maven-lib-dev"
            url = uri(if (isRelease) releaseUrl else snapshotUrl)
            credentials {
                username = System.getProperty("gradle.wrapperUser")
                password = System.getProperty("gradle.wrapperPassword")
            }
        }
    }
}

tasks.named("publishAllPublicationsToMavenRepository") {
    dependsOn("buildPlugin")
}
tasks.named("publishMavenPublicationToMavenLocal") {
    dependsOn("buildPlugin")
}
tasks.named("publishMavenPublicationToMavenRepository") {
    dependsOn("buildPlugin")
}
tasks.named("publishToMavenLocal") {
    dependsOn("buildPlugin")
}
