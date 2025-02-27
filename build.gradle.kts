import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.util.*

plugins {
	id("java")
	id("application")
	id("org.jetbrains.kotlin.jvm") version("2.1.0")
	id("org.jetbrains.kotlin.plugin.serialization") version("2.1.0")
}

val props = Properties().apply {
	file("$projectDir/src/main/resources/build.properties").inputStream().use { fis ->
		load(fis)
	}
}

group = "ru.cataclysm"
version = props.getProperty("version")

repositories {
    mavenCentral()
}

dependencies {
	implementation("com.squareup.okhttp3:okhttp:4.12.0")

	implementation("org.tinylog:tinylog-api-kotlin:2.7.0")
	implementation("org.tinylog:tinylog-impl:2.7.0")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.9.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.7.3")

	implementation(files("$projectDir/libs/jlibtorrent-1.2.13.0.jar"))
}

application {
	mainModule = "ru.cataclysm.main"
	mainClass = "ru.cataclysm.LauncherKt"
}

tasks.named<KotlinJvmCompile>("compileKotlin") {
//	sourceCompatibility = JavaVersion.VERSION_1_8
//	targetCompatibility = JavaVersion.VERSION_1_8
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_1_8)
	}
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

tasks.jar {
	manifest {
		attributes["Main-Class"] = "Main"
	}
}


dependencies {
	implementation("com.squareup.okhttp3:okhttp:4.12.0")

	implementation("org.tinylog:tinylog-api-kotlin:2.7.0")
	implementation("org.tinylog:tinylog-impl:2.7.0")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.9.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.7.3")

	implementation(files("$projectDir/libs/jlibtorrent-1.2.13.0.jar"))
}

// jar task modification to include dependencies
tasks.register<Jar>("jarWithDependency") {
//	dependsOn(tasks.jar)
	with(tasks.jar.get())

	manifest {
		attributes["Main-Class"] = "ru.cataclysm.LauncherKt"
		attributes["Implementation-Version"] = version
	}

	// Include the classpath from the dependencies
	from(configurations.runtimeClasspath.get().asFileTree.map { if (it.isDirectory) it else zipTree(it) }) {
		exclude("**/module-info.class")
		exclude("**/META-INF/LICENSE")
		exclude("**/META-INF/NOTICE")
		exclude("**/META-INF/DEPENDENCIES")
	}

	// include libtorrent natives
	from("$projectDir/libs") {
		exclude("*.jar")
	}

	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

