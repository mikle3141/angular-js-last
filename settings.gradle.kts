pluginManagement {
    repositories {
        val oscToken: String = System.getProperty("gradle.wrapperOscToken")
        val mavenUser: String = System.getProperty("gradle.wrapperUser")
        val mavenPassword: String = System.getProperty("gradle.wrapperPassword")

        maven {
            name = "public"
            url = uri("https://nexus-ci.delta.sbrf.ru/repository/public")
            credentials {
                username = mavenUser
                password = mavenPassword
            }
            content {
                includeGroup("com.google.code.gson")
            }
        }
        maven {
            name = "gradle-plugins"
            url = uri("https://sberosc.sigma.sbrf.ru/repo/maven/gradle_plugins")
            credentials {
                username = "token"
                password = oscToken
            }
        }
        maven {
            name = "epoch"
            url = uri("https://nexus-ci.delta.sbrf.ru/repository/maven-lib-int/ru/sbrf/epoch")
            credentials {
                username = mavenUser
                password = mavenPassword
            }
            metadataSources {
                mavenPom()
                gradleMetadata()
            }
        }
    }
}

rootProject.name = "angular-js"
