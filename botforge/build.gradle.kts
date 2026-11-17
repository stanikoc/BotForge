import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    `java-library`
    signing
    id("com.vanniktech.maven.publish") version "0.37.0"
}

dependencies {
    api("org.jspecify:jspecify:1.0.1")
}

mavenPublishing {
    configure(
        JavaLibrary(
            javadocJar = JavadocJar.Javadoc(),
            sourcesJar = SourcesJar.Sources(),
        )
    )

    coordinates("io.github.stanikoc", "botforge", version.toString())

    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    pom {
        name = "BotForge"
        description = "A small Telegram bot framework for Java."
        inceptionYear = "2026"
        url = "https://github.com/stanikoc/BotForge"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }

        developers {
            developer {
                id = "stanikoc"
                name = "Stanikoc"
                url = "https://github.com/stanikoc"
            }
        }

        scm {
            url = "https://github.com/stanikoc/BotForge"
            connection = "scm:git:git://github.com/stanikoc/BotForge.git"
            developerConnection = "scm:git:ssh://git@github.com/stanikoc/BotForge.git"
        }
    }
}

signing {
    setRequired {
        gradle.taskGraph.allTasks.any {
            it.name.contains("MavenCentral") || it.name == "publish"
        }
    }
}
