plugins {
	id("java")
	id("com.github.johnrengelman.shadow").version("7.1.0")
}

version = "1.8.3-4"

repositories {
    mavenCentral()
    maven("https://iudex.fi/maven/")
}

dependencies {
	implementation("org.tinylog:tinylog-api:2.7.0")
	implementation("org.tinylog:tinylog-impl:2.7.0")
	implementation("com.squareup.okhttp3:okhttp:4.12.0")
	implementation("com.google.code.gson:gson:2.12.1")

	runtimeOnly("org.jetbrains:annotations:15.0")

	testImplementation("net.sf.jopt-simple:jopt-simple:5.0.4")
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

tasks.jar {
	manifest {
		attributes["Main-Class"] = "Main"
		attributes["Implementation-Version"] = version
	}
}

tasks.shadowJar {
	// make it to just launcher.jar
	archiveClassifier = ""
	archiveVersion = ""
	doLast {
		archiveFile.get().asFile.parentFile.resolve("version.txt")
			.writeText(project.version.toString())
	}
}
