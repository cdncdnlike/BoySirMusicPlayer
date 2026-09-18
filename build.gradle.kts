plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
}

group = "lezi.boysir"
version = "1.0.2"

repositories {
    mavenCentral()
}

val junitVersion = "5.12.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    inputs.property("projectVersion", project.version.toString())
    filesMatching("**/version.properties") {
        expand("version" to project.version.toString())
    }
}

application {
    mainModule.set("lezi.boysir.musicplayer")
    mainClass.set("lezi.boysir.musicplayer.HelloApplication")
}

javafx {
    version = "17.0.14"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.media")
}

dependencies {
    implementation("io.github.palexdev:materialfx:11.17.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages", "--add-modules", "jdk.crypto.ec,jdk.crypto.cryptoki,java.net.http,java.sql"))
    launcher {
        name = "BoySirMusicPlayer"
    }
    jpackage {
        imageName = "BoySirMusicPlayer"
        installerName = "BoySirMusicPlayer"
        appVersion = project.version.toString()
        icon = "src/main/resources/lezi/boysir/musicplayer/app-logo.ico"
        installerOptions = listOf(
            "--win-menu",
            "--win-shortcut",
            "--win-dir-chooser",
            "--win-menu-group", "BoySir Music Player",
            "--license-file", "src/main/resources/LICENSE.txt",
            "--vendor", "Boy_Sir",
            "--description", "BoySir Music Player",
            "--copyright", "Copyright © 2026 BoySir Network",
            "--file-associations", "src/main/resources/lezi/boysir/musicplayer/file-association-mp3.properties",
            "--file-associations", "src/main/resources/lezi/boysir/musicplayer/file-association-m4a.properties",
            "--file-associations", "src/main/resources/lezi/boysir/musicplayer/file-association-aac.properties",
            "--file-associations", "src/main/resources/lezi/boysir/musicplayer/file-association-wav.properties",
            "--file-associations", "src/main/resources/lezi/boysir/musicplayer/file-association-aiff.properties",
            "--file-associations", "src/main/resources/lezi/boysir/musicplayer/file-association-flac.properties",
        )
    }
}
