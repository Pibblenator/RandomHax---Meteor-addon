plugins { id("fabric-loom") version "1.13.+" }

@Suppress("UNCHECKED_CAST")
val mcVersion       = project.property("minecraft_version") as String
val yarnMappings    = project.property("yarn_mappings") as String
val loaderVersion   = project.property("loader_version") as String
val modVersion      = project.property("mod_version") as String
val mavenGroup      = project.property("maven_group") as String
val archivesBase    = project.property("archives_base_name") as String
val meteorMcVersion = (findProperty("meteor_mc_version") ?: mcVersion) as String
val xaeroPlusVer    = project.property("xaeroplus_version") as String
val xaeroWMVer      = project.property("xaeros_worldmap_version") as String
val xaeroMMVer      = project.property("xaeros_minimap_version") as String

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

    modImplementation("meteordevelopment:meteor-client:$meteorMcVersion-SNAPSHOT")
    modCompileOnly("meteordevelopment:baritone:$meteorMcVersion-SNAPSHOT")

    modImplementation(include("com.github.ben-manes.caffeine:caffeine:3.1.8")!!)
    modImplementation(include("net.lenni0451:LambdaEvents:2.4.2")!!)

    modCompileOnly("maven.modrinth:xaeroplus:$xaeroPlusVer")
    modCompileOnly("maven.modrinth:xaeros-world-map:$xaeroWMVer")
    modCompileOnly("maven.modrinth:xaeros-minimap:$xaeroMMVer")

    modRuntimeOnly("maven.modrinth:xaeroplus:$xaeroPlusVer")
    modRuntimeOnly("maven.modrinth:xaeros-world-map:$xaeroWMVer")
    modRuntimeOnly("maven.modrinth:xaeros-minimap:$xaeroMMVer")
}

tasks {
    processResources {
        val props = mapOf("version" to project.version.toString(), "mc_version" to mcVersion)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("fabric.mod.json") { expand(props) }
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }
    named<org.gradle.api.tasks.bundling.AbstractArchiveTask>("remapJar") {
        archiveFileName.set(provider { "$archivesBase ${project.version}.jar" })
    }
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
