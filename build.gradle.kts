plugins {
	java
	id("org.springframework.boot") version "3.4.0"
	id("io.spring.dependency-management") version "1.1.6"
	id("com.diffplug.spotless") version "7.0.2"
}

group = "com.adolfoeloy"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(23)
	}
}

spotless {
	// limit format enforcement to just the files changed by this feature branch
//	ratchetFrom("origin/main")

	format("misc") {
		// define the files to apply `misc` to
		target("*.gradle", ".gitattributes", ".gitignore")

		// define the steps to apply to those files
		trimTrailingWhitespace()
		leadingTabsToSpaces()
		endWithNewline()
	}

	java {
		// apply a specific flavor of google-java-format
		palantirJavaFormat()

		// fix formatting of type annotations
		formatAnnotations()
	}
}


repositories {
	mavenCentral()
}

dependencies {
	implementation(platform("software.amazon.awssdk:bom:2.29.30"))
	implementation("org.apache.commons:commons-lang3:3.0")
	implementation("com.google.guava:guava:33.4.0-jre")

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("software.amazon.awssdk:swf")
	implementation("software.amazon.awssdk:sns")
	implementation("com.diffplug.spotless:spotless-plugin-gradle:6.8.0")

	runtimeOnly("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testCompileOnly("org.assertj:assertj-core:3.11.1")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
