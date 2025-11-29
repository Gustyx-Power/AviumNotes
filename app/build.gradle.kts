import org.apache.http.client.methods.HttpPost
import org.apache.http.client.config.RequestConfig
import org.apache.http.entity.StringEntity
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.util.Date
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
}

android {
    namespace = "id.avium.aviumnotes"
    compileSdk = 36

    defaultConfig {
        applicationId = "id.avium.aviumnotes"
        minSdk = 32
        targetSdk = 36
        versionCode = 1
        versionName = "1.0-beta2"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = project.findProperty("myKeystorePath")?.let { file(it) }
            storePassword = project.findProperty("myKeystorePassword") as String?
            keyAlias = project.findProperty("myKeyAlias") as String?
            keyPassword = project.findProperty("myKeyPassword") as String?
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        lint.disable.add("NullSafeMutableLiveData")
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.4.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13")
    implementation("androidx.compose.ui:ui-graphics")


    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.savedstate:savedstate:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
}

abstract class SendTelegramMessageTask : DefaultTask() {
    @get:Input abstract val telegramBotToken: Property<String>
    @get:Input abstract val telegramChatId: Property<String>
    @get:Input abstract val appVersionName: Property<String>
    @get:Input abstract val appPackageName: Property<String>
    @get:Input abstract val appProjectName: Property<String>
    @get:Input @get:Optional abstract val changelog: Property<String>

    init {
        telegramBotToken.convention(project.findProperty("telegramBotToken")?.toString() ?: "")
        telegramChatId.convention(project.findProperty("telegramChatId")?.toString() ?: "")
        appVersionName.convention("")
        appPackageName.convention("")
        appProjectName.convention(project.name)
        changelog.convention(project.findProperty("myChangelog")?.toString() ?: "")
    }

    @TaskAction
    fun sendMessage() {
        if (telegramBotToken.get().isEmpty() || telegramChatId.get().isEmpty()) {
            logger.warn("Telegram Bot Token or Chat ID not found. Skipping message.")
            return
        }

        val buildStatus = if (project.gradle.taskGraph.allTasks.any { it.state.failure != null }) "FAILED" else "SUCCESS"
        val currentAppVersion = appVersionName.getOrElse(project.android.defaultConfig.versionName ?: "N/A")
        val currentAppPackage = appPackageName.getOrElse(project.android.defaultConfig.applicationId ?: "N/A")
        val currentProjectName = appProjectName.get()
        val kotlinVersion = project.getKotlinPluginVersion() ?: "N/A"

        fun sendTelegramMessage(text: String, disableNotification: Boolean = false): Int? {
            val url = "https://botapi.arasea.dpdns.org/bot${telegramBotToken.get()}/sendMessage"
            val jsonPayload = """{"chat_id":"${telegramChatId.get()}","text":"${text.replace("\"", "\\\"")}","disable_notification":$disableNotification}"""
            HttpClients.createDefault().use { httpClient ->
                val post = HttpPost(url)
                post.entity = StringEntity(jsonPayload, "UTF-8")
                post.setHeader("Content-Type", "application/json")
                val response = httpClient.execute(post)
                val responseBody = EntityUtils.toString(response.entity, "UTF-8")
                EntityUtils.consumeQuietly(response.entity)
                return "\\\"message_id\\\":(\\d+)".toRegex().find(responseBody)?.groupValues?.get(1)?.toIntOrNull()
            }
        }

        fun editTelegramMessage(messageId: Int, text: String) {
            val url = "https://botapi.arasea.dpdns.org/bot${telegramBotToken.get()}/editMessageText"
            val jsonPayload = """{"chat_id":"${telegramChatId.get()}","message_id":$messageId,"text":"${text.replace("\"", "\\\"")}"}"""
            HttpClients.createDefault().use { httpClient ->
                val post = HttpPost(url)
                post.entity = StringEntity(jsonPayload, "UTF-8")
                post.setHeader("Content-Type", "application/json")
                httpClient.execute(post).entity?.let { EntityUtils.consumeQuietly(it) }
            }
        }

        fun pinTelegramMessage(messageId: Int) {
            val url = "https://botapi.arasea.dpdns.org/bot${telegramBotToken.get()}/pinChatMessage"
            val jsonPayload = """{"chat_id":"${telegramChatId.get()}","message_id":$messageId,"disable_notification":true}"""
            HttpClients.createDefault().use { httpClient ->
                val post = HttpPost(url)
                post.entity = StringEntity(jsonPayload, "UTF-8")
                post.setHeader("Content-Type", "application/json")
                httpClient.execute(post).entity?.let { EntityUtils.consumeQuietly(it) }
            }
        }

        val buildMsgId = sendTelegramMessage("Processing build...", disableNotification = true)
        if (buildMsgId != null) pinTelegramMessage(buildMsgId)

        val javaVersion = JavaVersion.current().toString()
        val gradleVersion = project.gradle.gradleVersion
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")

        val (processor, kernelInfo) = if (osName.contains("Windows", ignoreCase = true)) {
            val proc = try {
                val process = ProcessBuilder("cmd", "/c", "wmic cpu get name")
                    .redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                output.lines().drop(1).firstOrNull()?.trim() ?: osArch
            } catch (e: Exception) {
                osArch
            }
            val kernel = try {
                val process = ProcessBuilder("cmd", "/c", "ver")
                    .redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                output.trim()
            } catch (e: Exception) {
                "N/A"
            }
            Pair(proc, kernel)
        } else {
            val proc = try {
                val process = ProcessBuilder("cat", "/proc/cpuinfo").redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                output.lines().find { it.startsWith("model name") }?.substringAfter(":")?.trim() ?: osArch
            } catch (e: Exception) {
                osArch
            }
            val kernel = try {
                val process = ProcessBuilder("uname", "-r").redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().readText().trim()
                process.waitFor()
                output.ifEmpty { "N/A" }
            } catch (e: Exception) {
                "N/A"
            }
            Pair(proc, kernel)
        }

        val compileSdkVersion = project.android.compileSdk ?: "N/A"
        val minSdkVersion = project.android.defaultConfig.minSdk ?: "N/A"
        val targetSdkVersionInt = project.android.defaultConfig.targetSdk
        val targetSdkVersionName = when (targetSdkVersionInt) {
            32 -> "12L [Snowcone V2]"
            33 -> "13 [Tiramisu]"
            34 -> "14 [UpsideDownCake]"
            35 -> "15 [VanillaIceCream]"
            36 -> "16 [Baklava]"
            else -> "Unknown"
        }

        val buildChangelog = changelog.getOrElse("")

        var message = "[Build Status] ${project.name} - $buildStatus* 🚀\n\n" +
                "📦 App: $currentProjectName\n" +
                "🏷️ Version: $currentAppVersion\n" +
                "🆔 Package: $currentAppPackage\n" +
                "📅 Time: ${Date()}\n\n" +
                "[Build Environment]\n" +
                "  OS: $osName ($osArch)\n" +
                "  Kernel: $kernelInfo\n" +
                "  Processor: $processor\n" +
                "  Kotlin: $kotlinVersion\n" +
                "  Java: $javaVersion\n" +
                "  Gradle: $gradleVersion\n\n" +
                "[App SDK Information]\n" +
                "  Min SDK: $minSdkVersion\n" +
                "  Target SDK: $targetSdkVersionInt (Android $targetSdkVersionName)\n"

        if (buildChangelog.isNotBlank()) {
            message += "\nChangelog:\n$buildChangelog\n"
        }

        if (buildMsgId != null) {
            editTelegramMessage(buildMsgId, if (buildStatus == "SUCCESS") "✅ Build finished successfully!" else "❌ Build failed!")
        }

        val url = "https://botapi.arasea.dpdns.org/bot${telegramBotToken.get()}/sendMessage"
        HttpClients.createDefault().use { httpClient ->
            val post = HttpPost(url)
            val jsonPayload = """{"chat_id":"${telegramChatId.get()}","text":"${message.replace("\"", "\\\"")}"}"""
            post.entity = StringEntity(jsonPayload, "UTF-8")
            post.setHeader("Content-Type", "application/json")
            try {
                val response = httpClient.execute(post)
                val responseBody = EntityUtils.toString(response.entity, "UTF-8")
                if (response.statusLine.statusCode in 200..299) {
                    logger.lifecycle("Successfully sent message to Telegram.")
                } else {
                    logger.error("Failed to send message. Status: ${response.statusLine}")
                }
                EntityUtils.consumeQuietly(response.entity)
            } catch (e: Exception) {
                logger.error("Failed to send message: ${e.message}", e)
            }
        }
    }
}

abstract class UploadApkToTelegramTask : DefaultTask() {
    @get:Input abstract val telegramBotToken: Property<String>
    @get:Input abstract val telegramChatId: Property<String>
    @get:InputFile abstract val apkFile: RegularFileProperty
    @get:Input abstract val appVersionName: Property<String>
    @get:Input abstract val appName: Property<String>

    @TaskAction
    fun uploadApk() {
        if (telegramBotToken.get().isEmpty() || telegramChatId.get().isEmpty()) {
            logger.warn("Telegram credentials not found. Skipping APK upload.")
            return
        }

        val currentApkFile = apkFile.get().asFile
        if (!currentApkFile.exists()) {
            logger.error("APK not found at ${currentApkFile.absolutePath}")
            return
        }

        val fileSizeMb = currentApkFile.length() / (1024.0 * 1024.0)
        logger.lifecycle("Uploading APK: ${currentApkFile.name} (${"%.2f".format(fileSizeMb)} MB)")

        if (fileSizeMb > 199) {
            logger.error("APK size exceeds 200MB limit. Skipping upload.")
            return
        }

        val caption = "📦 New Test Release: ${appName.get()} v${appVersionName.get()}\n" +
                "Build time: ${Date()}\n" +
                "File: ${currentApkFile.name} (${"%.2f".format(fileSizeMb)} MB)"

        val url = "https://botapi.arasea.dpdns.org/bot${telegramBotToken.get()}/sendDocument"
        val requestConfig = RequestConfig.custom()
            .setConnectTimeout(30 * 1000)
            .setSocketTimeout(5 * 60 * 1000)
            .build()

        HttpClients.custom().setDefaultRequestConfig(requestConfig).build().use { httpClient ->
            val post = HttpPost(url)
            val entityBuilder = MultipartEntityBuilder.create()
            entityBuilder.addTextBody("chat_id", telegramChatId.get())
            entityBuilder.addTextBody("caption", caption, org.apache.http.entity.ContentType.TEXT_PLAIN.withCharset("UTF-8"))
            entityBuilder.addPart("document", FileBody(currentApkFile))
            post.entity = entityBuilder.build()

            try {
                val response = httpClient.execute(post)
                val responseBody = EntityUtils.toString(response.entity, "UTF-8")
                if (response.statusLine.statusCode in 200..299) {
                    logger.lifecycle("Successfully uploaded APK to Telegram.")
                } else {
                    logger.error("Failed to upload APK. Status: ${response.statusLine}")
                }
                EntityUtils.consumeQuietly(response.entity)
            } catch (e: Exception) {
                logger.error("Failed to upload APK: ${e.message}", e)
            }
        }
    }
}

val renameReleaseApk by tasks.registering(Copy::class) {
    group = "custom"
    description = "Renames app-release.apk"
    val versionName = android.defaultConfig.versionName ?: "unknown"
    from(layout.buildDirectory.dir("outputs/apk/release")) {
        include("app-release.apk")
    }
    into(layout.projectDirectory.dir("dist"))
    rename { "AviumNotes-$versionName.apk" }
}

val uploadReleaseApkToTelegram by tasks.registering(UploadApkToTelegramTask::class) {
    group = "custom"
    description = "Uploads renamed APK to Telegram"
    val versionName = android.defaultConfig.versionName ?: "unknown"
    apkFile.set(layout.projectDirectory.file("dist/AviumNotes-$versionName.apk"))
    telegramBotToken.convention(project.findProperty("telegramBotToken")?.toString() ?: "")
    telegramChatId.convention(project.findProperty("telegramChatId")?.toString() ?: "")
    appVersionName.convention(project.provider { android.defaultConfig.versionName ?: "N/A" })
    appName.convention(project.name)
    mustRunAfter(renameReleaseApk)
}

val notifyBuildStatusToTelegram by tasks.registering(SendTelegramMessageTask::class) {
    group = "custom"
    description = "Sends build status to Telegram"
    appVersionName.convention(project.provider { android.defaultConfig.versionName ?: "N/A" })
    appPackageName.convention(project.provider { android.defaultConfig.applicationId ?: "N/A" })
    appProjectName.convention(project.provider { android.namespace?.substringAfterLast('.') ?: project.name })
}

tasks.register("buildAndPublish") {
    group = "custom"
    description = "Build, rename, upload APK, and notify"
    dependsOn(tasks.named("assembleRelease"))
    renameReleaseApk.get().mustRunAfter(tasks.named("assembleRelease"))
    uploadReleaseApkToTelegram.get().mustRunAfter(renameReleaseApk)
    notifyBuildStatusToTelegram.get().mustRunAfter(uploadReleaseApkToTelegram)
    finalizedBy(renameReleaseApk, uploadReleaseApkToTelegram, notifyBuildStatusToTelegram)
}
