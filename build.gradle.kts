plugins {
    id("fabric-loom") version "1.13.+"
}

@Suppress("UNCHECKED_CAST")
val mcVersion      = (project.property("minecraft_version") as String)
val yarnMappings   = (project.property("yarn_mappings") as String)
val loaderVersion  = (project.property("loader_version") as String)
val modVersion     = (project.property("mod_version") as String)
val mavenGroup     = (project.property("maven_group") as String)
val archivesBase   = (project.property("archives_base_name") as String)
val meteorMc       = (project.property("meteor_mc_version") as String)

val xaeroMinimap   = (project.findProperty("xaeros_minimap_version") as String)
val xaeroWorldMap  = (project.findProperty("xaeros_worldmap_version") as String)
val xaeroPlus      = (project.findProperty("xaeroplus_version") as String)

base {
    archivesName.set(archivesBase)
    version = modVersion
    group = mavenGroup
}

repositories {
    mavenCentral()
    maven("https://maven.meteordev.org/releases")
    maven("https://maven.meteordev.org/snapshots")
    maven("https://api.modrinth.com/maven") { name = "Modrinth" }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    modImplementation("meteordevelopment:meteor-client:$meteorMc-SNAPSHOT")
    modCompileOnly("meteordevelopment:baritone:$meteorMc-SNAPSHOT")

    modImplementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    modImplementation("net.lenni0451:LambdaEvents:2.4.2")

    // Xaeros 67
    modImplementation("maven.modrinth:xaeroplus:$xaeroPlus")
    modImplementation("maven.modrinth:xaeros-minimap:$xaeroMinimap")
    modImplementation("maven.modrinth:xaeros-world-map:$xaeroWorldMap")
}

tasks {
    processResources {
        val props = mapOf(
            "version" to project.version.toString(),
            "mc_version" to mcVersion
        )
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("fabric.mod.json") { expand(props) }
    }

    jar {
        val nameProv = providers.provider { base.archivesName.get() }
        from("LICENSE") { rename { "${it}_${nameProv.get()}" } }
    }

    named<AbstractArchiveTask>("remapJar") {
        archiveFileName.set(providers.provider { "${base.archivesName.get()} ${project.version}.jar" })
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
