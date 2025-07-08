pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") // necesario para hannesa2 fork
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "MyApplication"
include(":app")