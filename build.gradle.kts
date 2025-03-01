plugins {
	id("java")
	id("com.github.johnrengelman.shadow").version("7.1.0")
}

version = "1.8.3-2"

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

tasks.jar {
	manifest {
		attributes["Main-Class"] = "Main"
		attributes["Implementation-Version"] = version
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
