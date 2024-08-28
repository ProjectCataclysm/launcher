import java.util.Properties

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

sourceSets {
	val coreCommon = create("coreCommon") {
		compileClasspath += sourceSets.main.get().compileClasspath
	}

	main {
		compileClasspath += coreCommon.runtimeClasspath
		runtimeClasspath += coreCommon.runtimeClasspath
	}

	test {
		compileClasspath += coreCommon.runtimeClasspath
		runtimeClasspath += coreCommon.runtimeClasspath
	}
}

configurations {
	create("commonJars") {
		extendsFrom(configurations.implementation.get())
	}
}

tasks.jar {
	manifest {
		attributes["Main-Class"] = "Main"
	}
}

tasks.shadowJar {
	from(sourceSets.named("coreCommon").get().output)
}

////shadowJar {
////    classifier 'all'
////    manifest.attributes.put('Main-Class', 'Main')
////
////    exclude '**/*.txt'
////    exclude '**/*.java'
////    exclude '**/*.html'
////    exclude 'proguard/**'
////    from sourceSets.coreCommon.output
////}

tasks.register<Jar>("commonJar") {
	archiveClassifier = "common"
	from(sourceSets.named("coreCommon").get().output)
}

artifacts {
	add("commonJars", tasks.named("commonJar"))
//	archives("jar")
//	archives("shadowJar")
}

tasks.register<JavaExec>("obfuscate") {
	dependsOn(tasks.shadowJar)
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
	description = "Создает текстовый файл с версией лаунчера"
	doLast {
		val libsDir = mkdir(layout.buildDirectory.dir("libs"))
		file("$libsDir/version.txt").writeText(project.version.toString())
	}
}

tasks.register<Copy>("deploy") {
	dependsOn(tasks.named("obfuscate"), tasks.named("generateVersion"))
	group = "deploying"

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
//	implementation("org.openjfx:javafx:11:pom")
    implementation("org.jetbrains:annotations:15.0")
	implementation("com.guardsquare:proguard-annotations:7.0.1")

    implementation("com.google.guava:guava:31.0.1-jre")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")

	implementation("org.apache.logging.log4j:log4j-api:2.19.0")
	implementation("org.apache.logging.log4j:log4j-core:2.19.0")

    implementation("com.squareup.okhttp3:okhttp:4.10.0")
//    implementation 'org.libtorrent4j:libtorrent4j:2.0.6-26'
//    implementation 'org.libtorrent4j:libtorrent4j-windows:2.0.6-26'
//    implementation 'com.github.atomashpolskiy:bt-core:1.10'
//    implementation 'com.github.atomashpolskiy:bt-http-tracker-client:1.10'
//    implementation 'com.github.atomashpolskiy:bt-dht:1.10'
//    implementation 'com.github.atomashpolskiy:bt-upnp:1.10'

	testImplementation("com.frostwire:jlibtorrent:1.2.0.18")
	testImplementation("net.java.dev.jna:jna:4.4.0")
    testImplementation("org.yaml:snakeyaml:1.29")
    testImplementation("net.sf.jopt-simple:jopt-simple:5.0.3")
}
