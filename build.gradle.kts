import java.util.*

plugins {
	id("java")
	id("com.github.johnrengelman.shadow").version("7.1.0")
}

val props = Properties().apply {
	file("$projectDir/src/main/resources/build.properties").inputStream().use { fis ->
		load(fis)
	}
}

version = props.getProperty("version")

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

tasks.jar {
	manifest {
		attributes["Main-Class"] = "Main"
	}
}

tasks.register<JavaExec>("obfuscate") {
	dependsOn(tasks.shadowJar)
	description = "Obfuscates launcher jar with local tool"
	mainClass = "cataclysm.build.BuildTools"

	doFirst {
		classpath = files(project(":build-tools").layout.buildDirectory.file("libs/build-tools.jar")) +
			project(":build-tools").sourceSets.main.get().runtimeClasspath

		args(
			"-job", "transformObf",
			"-i", tasks.shadowJar.get().archiveFile.get().asFile,
			"-o", layout.buildDirectory.file("lib/launcher-final.jar").get(),
			"-cp", "${System.getProperty("java.home")}/lib/rt.jar",
			"-pm", layout.buildDirectory.file("lib/launcher-final.map").get(),
			"-mainClass", "Main",
			"-packageFilter", "cataclysm.",
			"-keepAnnotations", "proguard.annotation.Keep",
			"-m", "launcher"
		)
	}
}

tasks.register("generateVersion") {
	description = "Generating version.txt file with current launcher version"
	doLast {
		val libsDir = mkdir(layout.buildDirectory.dir("libs"))
		file("$libsDir/version.txt").writeText(project.version.toString())
	}
}

tasks.register<Copy>("deploy") {
	dependsOn(tasks.named("obfuscate"), tasks.named("generateVersion"))
	group = "deploying"
	description = "Copying launcher jar into deploy directory"

	from(layout.buildDirectory.file("libs")) {
//		include("launcher-final.jar").rename { "launcher.jar" }
		include("version.txt")
	}

	into("${properties["projectCataclysm.deployDirPath"]}/launcher/")
}

repositories {
    mavenCentral()
    maven("https://iudex.fi/maven/")
}

dependencies {
    implementation("org.jetbrains:annotations:15.0")
	implementation("com.guardsquare:proguard-annotations:7.0.1")
	implementation("org.tinylog:tinylog-api:2.7.0")
	implementation("org.tinylog:tinylog-impl:2.7.0")
	implementation("com.squareup.okhttp3:okhttp:4.10.0")
	implementation("com.google.guava:guava:33.4.0-jre")
	implementation("com.googlecode.json-simple:json-simple:1.1.1")

	testImplementation("net.sf.jopt-simple:jopt-simple:5.0.4")
}
