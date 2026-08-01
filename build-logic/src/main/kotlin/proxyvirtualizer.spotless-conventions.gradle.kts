plugins {
    id("com.diffplug.spotless")
}

spotless {
    java {
        target("src/*/java/**/*.java")
        googleJavaFormat("1.28.0")
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("gradleKotlin") {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", "**/.gradle/**", "**/.kotlin/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
