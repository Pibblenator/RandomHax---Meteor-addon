import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    id("fabric-loom") version "1.13.4"
}

fun prop(name: String, def: String) = providers.gradleProperty(name).orElse(def).get()

val mcVersion               = prop("minecraft_version", "1.21.4")
val yarnMappings            = prop("yarn_mappings", "1.21.4+build.1")
val loaderVersion           = prop("loader_version", "0.16.7")
val modVersion              = prop("mod_version", "0.1.0")
val mavenGroup              = prop("maven_group", "com.randomhax")
val archivesBase            = prop("archives_base_name", "randomhax")
val archivesPretty          = prop("archives_base_name_readable", archivesBase.replace('-', ' '))

val meteorMcVersion         = prop("meteor_mc_version", "1.21.4")
val meteorVersionSuffix     = prop("meteor_version_suffix", "SNAPSHOT")

val xaerosMinimapVersion    = prop("xaeros_minimap_version", "25.2.10_Fabric_1.21.4")
val xaerosWorldMapVersion   = prop("xaeros_worldmap_version", "1.39.12_Fabric_1.21.4")
val xaeroplusVersion        = prop("xaeroplus_version", "2.29.0+fabric-1.21.4")

base {
    archivesName.set(archivesBase)
    version = modVersion
    group = mavenGroup
}

repositories {
    maven { url = uri("https://maven.fabricmc.net/") }
    mavenCentral()
    maven("https://maven.meteordev.org/releases")
    maven("https://maven.meteordev.org/snapshots")
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content { includeGroup("maven.modrinth") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    modImplementation("meteordevelopment:meteor-client:$meteorMcVersion-$meteorVersionSuffix")

    modImplementation("maven.modrinth:xaeros-minimap:$xaerosMinimapVersion")
    modImplementation("maven.modrinth:xaeros-world-map:$xaerosWorldMapVersion")
    modImplementation("maven.modrinth:xaeroplus:$xaeroplusVersion")

    modCompileOnly("meteordevelopment:baritone:$meteorMcVersion-$meteorVersionSuffix")

    modImplementation(include("com.github.ben-manes.caffeine:caffeine:3.1.8")!!)
    modImplementation(include("net.lenni0451:LambdaEvents:2.4.2")!!)
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.119.4+1.21.4")
}

tasks {
    processResources {
        val props = mapOf("version" to project.version.toString(), "mc_version" to mcVersion)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("fabric.mod.json") { expand(props) }
    }

    jar {
        inputs.property("archivesName", base.archivesName.get())
        from("LICENSE") { rename { "${it}_${inputs.properties["archivesName"]}" } }
    }

    named<AbstractArchiveTask>("remapJar") {
        archiveFileName.set(providers.provider { "$archivesPretty ${project.version}.jar" })
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
