
import org.gradle.jvm.tasks.Jar

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

val artifactBase = ((findProperty("customArchiveBaseName") as String?)?.trim()).orEmpty()
    .ifBlank { "SteamTurbine" }
val artifactVersion = ((findProperty("modVersion") as String?)?.trim()).orEmpty()
    .ifBlank { ((findProperty("version") as String?)?.trim()).orEmpty() }
    .ifBlank { "1.0.0" }

tasks.named<Jar>("jar") {
    archiveBaseName.set(artifactBase)
    archiveVersion.set("")
    archiveClassifier.set("dev")
    archiveFileName.set("${artifactBase}-${artifactVersion}-dev.jar")
}

tasks.named<Jar>("sourcesJar") {
    archiveBaseName.set(artifactBase)
    archiveVersion.set("")
    archiveClassifier.set("sources")
    archiveFileName.set("${artifactBase}-${artifactVersion}-sources.jar")
}

tasks.named<Jar>("apiJar") {
    archiveBaseName.set(artifactBase)
    archiveVersion.set("")
    archiveClassifier.set("api")
    archiveFileName.set("${artifactBase}-${artifactVersion}-api.jar")
}

tasks.named<Jar>("reobfJar") {
    archiveBaseName.set(artifactBase)
    archiveVersion.set("")
    archiveClassifier.set("")
    archiveFileName.set("${artifactBase}-${artifactVersion}.jar")
}
