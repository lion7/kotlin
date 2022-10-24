import java.io.ByteArrayInputStream
import java.util.Properties

object Config {
    const val REPO_KEY = "bootstrap.kotlin.repo"
    const val LOCAL_BOOTSTRAP = "bootstrap.local"
}

abstract class PropertiesValueSource : ValueSource<Properties, PropertiesValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val fileName: Property<String>
        val rootDir: Property<File>
    }

    override fun obtain(): Properties {
        return parameters.rootDir.get().resolve(
            parameters.fileName.get()
        ).bufferedReader().use {
            Properties().apply { load(it) }
        }
    }
}

private val localProperties = providers.of(PropertiesValueSource::class.java) {
    parameters {
        fileName.set("local.properties")
        rootDir.set(settings.rootDir)
    }
}.forUseAtConfigurationTime()

val customBootstrapRepo = localProperties
    .map { it.getProperty(Config.REPO_KEY) }
    .orElse(providers.gradleProperty(Config.REPO_KEY).forUseAtConfigurationTime())

fun RepositoryHandler.addCustomBootstrapRepository(dest: String) {
    val bootstrapRepoUrl = customBootstrapRepo.orNull
    if (bootstrapRepoUrl != null) {
        logger.info("Adding custom bootstrap repository to $dest: $bootstrapRepoUrl")
        maven(url = bootstrapRepoUrl)
    }
}

fun String?.propValueToBoolean(default: Boolean = false): Boolean {
    return when {
        this == null -> default
        isEmpty() -> true // has property without value means 'true'
        else -> trim().toBoolean()
    }
}

val isLocalBootstrapEnabled = localProperties.map { it.getProperty(Config.LOCAL_BOOTSTRAP) }
    .orElse(providers.gradleProperty(Config.LOCAL_BOOTSTRAP))
    .map { it?.propValueToBoolean() }
    .forUseAtConfigurationTime()

pluginManagement.repositories.addCustomBootstrapRepository("settings")
gradle.beforeProject {
    repositories.addCustomBootstrapRepository(name)
}

// Get bootstrap kotlin version and repository url
// and set it using pluginManagement and dependencyManangement
//when {
//    isLocalBootstrapEnabled.get() -> TODO()
//}
