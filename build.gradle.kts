plugins {
	java
	id("org.springframework.boot") version "3.4.0"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "com.adolfoeloy"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(23)
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
	implementation("software.amazon.awssdk:swf")
	implementation("software.amazon.awssdk:sns")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testCompileOnly("org.assertj:assertj-core:3.11.1")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
