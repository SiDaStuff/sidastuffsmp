plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.gradleup.shadow") version "8.3.6"
}

fun getProperty(key: String): String {
    return project.property(key) as String
}

group = "org.atrimilan"
version = getProperty("projectVersion")

val localServerDir = "local-server"
val projectVersion = getProperty("projectVersion")

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }
    maven {
        name = "placeholderapi"
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}

dependencies {
    paperweight.paperDevBundle(getProperty("paperApiVersion"))
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly(files("libs/Vulcan-2.9.7.11.jar"))
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    implementation("org.slf4j:slf4j-nop:2.0.9")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.20.0")
    implementation("com.google.http-client:google-http-client-gson:1.43.3")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks {

    /********** Versioning **********/

    fun incrementVersion(type: String) {
        val (major, minor, patch) = projectVersion.split(".").map { it.toInt() }
        val newVersion = when (type) {
            "major" -> "${major + 1}.0.0"
            "minor" -> "$major.${minor + 1}.0"
            "patch" -> "$major.$minor.${patch + 1}"
            else -> throw GradleException("Invalid version type")
        }
        file("gradle.properties").apply {
            writeText(readText().replace(Regex("projectVersion=.*"), "projectVersion=$newVersion"))
        }
    }

    register("incrementMajorVersion") {
        group = "2- versioning"
        doLast {
            incrementVersion("major")
        }
    }

    register("incrementMinorVersion") {
        group = "2- versioning"
        doLast {
            incrementVersion("minor")
        }
    }

    register("incrementPatchVersion") {
        group = "2- versioning"
        doLast {
            incrementVersion("patch")
        }
    }

    /********** Build plugin and run a local server **********/

    register("buildPluginAndRunServer") {
        group = "1- local server"
        description = "Build the plugin's JAR file and run a Paper test server that includes it"

        dependsOn("shadowJar")

        doFirst {
            val jarFile = file("build/libs/${project.name}-${version}-all.jar")
            val pluginsDir = file("${localServerDir}/plugins").apply { mkdirs() }

            if (jarFile.exists())
                jarFile.copyTo(file("${pluginsDir}/${jarFile.name}"), overwrite = true)
            else
                throw GradleException("File ${jarFile.name} not found.")
        }
        finalizedBy("runDevBundleServer")
    }

    runDevBundleServer {
        group = "1- local server"
        runDirectory.set(file(localServerDir))
        jvmArgs(
            "-Dcom.mojang.eula.agree=true",
            "-Dserver.port=25565"
        )
        doFirst {
            val serverPropertiesFile = file("${localServerDir}/server.properties")
            val bukkitYmlFile = file("${localServerDir}/bukkit.yml")
            listOf(serverPropertiesFile, bukkitYmlFile).forEach { file ->
                file.parentFile.mkdirs()
            }
            serverPropertiesFile.writeText(""" allow-nether=false """.trimIndent())
            bukkitYmlFile.writeText(""" settings: allow-end: false """.trimIndent())
        }
    }

    named("runServer") {
        throw GradleException("Please use the 'runDevBundleServer' task instead.")
    }

    shadowJar {
        archiveClassifier.set("all")
        mergeServiceFiles()
        relocate("org.xerial", "org.atrimilan.sidastuffsmp.libs.org.xerial")
        relocate("org.slf4j", "org.atrimilan.sidastuffsmp.libs.org.slf4j")
    }

    build {
        dependsOn("shadowJar")
    }

    test {
        useJUnitPlatform()
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
