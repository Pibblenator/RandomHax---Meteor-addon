import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    id("fabric-loom") version "1.11-SNAPSHOT"
}

@Suppress("UNCHECKED_CAST")
val mcVersion      = project.property("minecraft_version") as String
val yarnMappings   = project.property("yarn_mappings") as String
val loaderVersion  = project.property("loader_version") as String
val modVersion     = project.property("mod_version") as String
val mavenGroup     = project.property("maven_group") as String
val archivesBase   = project.property("archives_base_name") as String
val archivesPretty = (project.findProperty("archives_base_name_readable") as String?)
    ?: archivesBase.replace('-', ' ')

base {
    archivesName.set(archivesBase)
    version = modVersion
    group = mavenGroup
}

repositories {
    mavenCentral()
    maven("https://maven.meteordev.org/releases") { name = "meteor-maven" }
    maven("https://maven.meteordev.org/snapshots") { name = "meteor-maven-snapshots" }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    // Meteor snapshot for this MC version
    modImplementation("meteordevelopment:meteor-client:$mcVersion-SNAPSHOT")
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version.toString(),
            "mc_version" to mcVersion
        )
        inputs.properties(propertyMap)
        filteringCharset = "UTF-8"
        filesMatching("fabric.mod.json") { expand(propertyMap) }
    }

    jar {
        inputs.property("archivesName", base.archivesName.get())
        from("LICENSE") { rename { "${it}_${inputs.properties["archivesName"]}" } }
    }

    // Rename the *remapped* jar itself (config-cache safe)
    named<AbstractArchiveTask>("remapJar") {
        archiveFileName.set(provider { "$archivesPretty ${project.version}.jar" })
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
