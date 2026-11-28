// Shared multiplatform code for Mochy (fork of Mochy Music by realidkroo,
// github.com/realidkroo/mochy-music). One library for every platform, in Metrolist's shape:
// the Android app (:app), the desktop launcher (:desktopApp) and the iOS app all depend on it
// and add nothing to it. Code moves here from :app one area at a time.
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

plugins {
    kotlin("multiplatform")
    // AGP ships multiplatform support only as a library plugin, so the installable APK stays in :app
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
}

// FFmpeg's natives for the machine doing the build: each desktop installer is built on its own OS.
val ffmpegPlatform: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arm = System.getProperty("os.arch").lowercase().let { it == "aarch64" || it == "arm64" }
    when {
        os.contains("mac") -> if (arm) "macosx-arm64" else "macosx-x86_64"
        os.contains("win") -> "windows-x86_64"
        else -> if (arm) "linux-arm64" else "linux-x86_64"
    }
}
val ffmpegDesktop: String = libs.versions.ffmpegDesktop.get()

kotlin {
    jvmToolchain(21)

    extensions.configure<KotlinMultiplatformAndroidLibraryExtension>("androidLibrary") {
        namespace = "com.mochyy.mochymusic.shared"
        compileSdk = 37
        minSdk = 24
        // compose resources (strings, later fonts and icons) ship inside the APK through this
        androidResources.enable = true
    }

    jvm("desktop") {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }

    // Declared now so commonMain stays iOS-clean; they compile only on a macOS host.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        // desktop and iOS: what Room can run there (the music database without Android's blocking DAO)
        val nonAndroidMain = create("nonAndroidMain") {
            dependsOn(commonMain.get())
        }
        getByName("desktopMain").dependsOn(nonAndroidMain)
        iosMain.get().dependsOn(nonAndroidMain)
        commonMain.dependencies {
            api(project(":shared:preferences"))
            api(project(":shared:theme"))
            api(libs.cmp.runtime)
            api(libs.cmp.foundation)
            api(libs.cmp.ui)
            api(libs.cmp.material3)
            implementation(libs.cmp.material.icons.core)
            implementation(libs.room.runtime)
            // api: the app reads shared StringResource values (SeekBarStyle.labelRes) with stringResource
            api(libs.cmp.components.resources)
            // the YouTube Music models (songs, albums, artists) every platform's screens show
            api(project(":providers:innertube"))
            implementation(libs.coil)
            // cover palette extraction on desktop and iOS (the same quantizer Metrolist uses); Android keeps androidx.palette
            implementation(libs.materialKolor)
            implementation(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            // Providers take an injected HttpClient; each platform picks the engine, timeouts and proxy.
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
            // shared ViewModels and what they're given, the way Metrolist does it; api so the app and the
            // launchers start Koin and call koinViewModel without naming Koin again
            api(libs.cmp.lifecycle.viewmodel.savedstate)
            // screens take androidx.navigation.NavController, as they do in the app
            api(libs.cmp.navigation.compose)
            api(libs.koin.core)
            api(libs.koin.compose)
            api(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            // BackPressHandler: the activity's own back handling, as the app has always used
            implementation(libs.activity)
            // the moved cover components read colours with the same androidx.palette the app uses
            implementation(libs.palette)
        }
        getByName("desktopMain").dependencies {
            implementation(compose.desktop.common)
            implementation(libs.sqlite.bundled)
            implementation(libs.ktor.client.okhttp)
            // desktop device discovery, as Metrolist's desktop build does it
            implementation(libs.jmdns)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.coil.network.okhttp)
            // stream resolution, the same library the Android app asks first
            implementation(libs.innertubex.desktop)
            // Linux media session (MPRIS over the session D-Bus); loaded only on Linux, failures are ignored
            implementation(libs.dbus.java.core)
            implementation(libs.dbus.java.transport.native.unixsocket)
            // decoding: JavaCV's FFmpeg grabber without the rest of JavaCV's camera and vision stack
            implementation("org.bytedeco:javacv:${libs.versions.javacpp.get()}") { isTransitive = false }
            implementation(libs.javacpp)
            implementation("org.bytedeco:ffmpeg:$ffmpegDesktop")
            implementation("org.bytedeco:ffmpeg:$ffmpegDesktop:$ffmpegPlatform")
            implementation("org.bytedeco:javacpp:${libs.versions.javacpp.get()}:$ffmpegPlatform")
        }
        iosMain.dependencies {
            implementation(libs.sqlite.bundled)
            implementation(libs.innertubex)
            implementation(libs.ktor.client.darwin)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.coil.network.ktor)
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

// Compose compiler tuning, the same rules as the app's block (app/build.gradle.kts): a composable
// that moved here is compiled by this module, so without this it loses the app's stability list and
// keeps source information in release builds, recomposing more than it did while it lived in :app.
//
// includeSourceInformation turns the source-position metadata and the live-literal hooks into
// per-composition work. It is needed for the layout inspector, so debug keeps it; release and
// profileable must strip it because profileable exists to measure release performance.
composeCompiler {
    // A KMP library has no build-type object to ask "is this a release build", so the decision comes
    // from the requested task names, exactly as the app does it. Reading gradle.startParameter at
    // configuration time keeps the configuration cache valid; its entry is keyed by task names, so it
    // re-evaluates when the requested tasks change.
    val buildingReleaseLike = gradle.startParameter.taskNames.any { name ->
        name.contains("Release", ignoreCase = true) ||
            name.contains("Profileable", ignoreCase = true) ||
            name.contains("Fast", ignoreCase = true)
    }
    includeSourceInformation.set(!buildingReleaseLike)

    if (project.hasProperty("composeMetrics")) {
        metricsDestination.set(layout.buildDirectory.dir("compose-metrics"))
        reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
    }

    // Treat these as @Immutable/@Stable without annotating them at every declaration site. The file
    // sits at the repository root so this module and :app read the same list; it must be resolved
    // from the root, not from this project's directory.
    stabilityConfigurationFiles.add(
        rootProject.layout.projectDirectory.file("compose_compiler_config.conf")
    )
}

// The desktop/iOS InternalDatabase exports its schema here, never to app/schemas: Fast checks diffs it
// against app/schemas so the two entity lists can't drift, and app/schemas stays Android's own export.
val roomSchemas = layout.buildDirectory.dir("schemas")
ksp {
    arg("room.schemaLocation", roomSchemas.get().asFile.path)
}
// declared, so a build-cache hit restores the export along with the generated code
tasks.matching { it.name == "kspKotlinDesktop" }.configureEach { outputs.dir(roomSchemas) }

val prepareIosFonts = tasks.register<Sync>("prepareIosFonts") {
    from(rootProject.layout.projectDirectory.dir("app/src/main/res/font")) {
        include("inter_regular.ttf", "inter_semibold.ttf")
        into("font")
    }
    into(layout.buildDirectory.dir("generated/iosComposeFonts"))
}

/**
 * The app's strings and plain vector icons, rewritten as Compose resources, so shared screens read the
 * same text the Android app does: `app/src/main/res` stays the one place a string is written or
 * translated. Android and Compose unescape differently (Compose knows only \n, \t, \uXXXX and
 * \\), so each value is decoded the way aapt2 reads it and written back in Compose's form.
 */
object ResourceBridge {
    private val SharedDrawableRef = Regex("""Res\.drawable\.(\w+)""")
    private val localeDir = Regex("values(-([a-z]{2,3})(-r([A-Z]{2}))?)?")
    // android's BCP 47 form, e.g. values-b+sr+Latn: compose has no script qualifier, so it becomes
    // values-sr (plus -rREGION when given), unless a plain folder already holds that name
    private val bcp47Dir = Regex("values-b\\+([a-z]{2,3})(\\+[A-Z][a-z]{3})?(\\+([A-Z]{2}))?")

    // compose matches the platform locale's language, and ios and desktop say "he" and "id"
    private val legacyLanguages = mapOf("iw" to "he", "in" to "id")

    fun run(res: File, sharedResources: File, out: File, sharedSources: Collection<File>) {
        out.deleteRecursively()
        sharedResources.copyRecursively(out)

        val dirs = res.listFiles().orEmpty().filter { it.isDirectory }
        val locales = dirs.filter { localeDir.matches(it.name) }
        val taken = locales.map { it.name }.toSet()
        // every folder under the name compose will read it by
        val named = locales.associateBy { it.name } + dirs.mapNotNull { dir ->
            val match = bcp47Dir.matchEntire(dir.name) ?: return@mapNotNull null
            val region = match.groupValues[4].takeIf { it.isNotEmpty() }?.let { "-r$it" }.orEmpty()
            val name = "values-${match.groupValues[1]}$region"
            if (name in taken) null else name to dir
        }
        val parsed = named.mapValues { (_, dir) -> readValues(dir) }
        val defaults = parsed["values"].orEmpty()
        for ((dirName, values) in parsed) {
            if (values.isEmpty()) continue
            val match = localeDir.matchEntire(dirName)!!
            val language = match.groupValues[2]
            val target = legacyLanguages[language]
                ?.let { dirName.replaceFirst("values-$language", "values-$it") }
                ?.takeIf { it !in parsed } ?: dirName
            write(File(out, "$target/app_strings.xml"), values) { ref -> values[ref] ?: defaults[ref] }
        }

        val drawables = File(out, "drawable").apply { mkdirs() }
        // plain vectors from drawable/ and the density-independent rasters from drawable-nodpi/ (brand
        // marks, onboarding art); density and version folders are left out, and drawable/ is read first
        // so a name in both keeps the copy it already had. A nodpi raster is copied only when shared code
        // names it: those are large pictures, and every copy ships a second time inside the APK
        val usedInShared = sharedSources.flatMap { file -> SharedDrawableRef.findAll(file.readText()).map { it.groupValues[1] } }.toSet()
        listOf(res.resolve("drawable"), res.resolve("drawable-nodpi")).forEach { source ->
            source.listFiles().orEmpty().forEach { file ->
                if (source.name == "drawable-nodpi" && file.nameWithoutExtension !in usedInShared) return@forEach
                // Android also reads "24.0dip"; Compose resources only read dp or px and throw at runtime on
                // anything else (a crash the first time a shared screen draws that icon), so sizes are rewritten
                // here, and a vector still sized some other way is left out: asking for it fails the build instead
                // the two framework colours icons use become the values Android gives them, so Compose can read them
                val vectorText = if (file.extension == "xml") withFrameworkColors(file.readText()).let { text ->
                    val plain = "<vector" in text && "<animated-vector" !in text && "?attr" !in text &&
                        "@color/" !in text && "@android:" !in text && "@drawable/" !in text
                    if (plain) text.replace(DipSize) { "${it.groupValues[1]}${it.groupValues[2]}dp\"" } else null
                }?.takeIf { text -> VectorSize.findAll(text).all { it.groupValues[2] == "dp" || it.groupValues[2] == "px" } } else null
                val raster = file.extension in setOf("png", "webp", "jpg")
                if ((vectorText != null || raster) && !File(drawables, file.name).exists() &&
                    drawables.listFiles().orEmpty().none { it.nameWithoutExtension == file.nameWithoutExtension }
                ) {
                    if (vectorText != null) File(drawables, file.name).writeText(vectorText) else file.copyTo(File(drawables, file.name))
                }
            }
        }
    }

    // "@android:color/white" → "#FFFFFFFF": the framework's own values for the only two it's used for
    private val FrameworkColorValues = mapOf("white" to "#FFFFFFFF", "transparent" to "#00000000")
    private val FrameworkColor = Regex(""""@android:color/(white|transparent)"""")
    // ?attr/colorControlNormal is only a default tint: Compose's Icon tints with its own colour over it, so
    // the bridged copy drops it (an Image() of such an icon draws its fill, white, instead)
    private val ControlTint = Regex("""\s*android:tint="\?attr/colorControlNormal"""")
    private fun withFrameworkColors(text: String): String =
        FrameworkColor.replace(text.replace(ControlTint, "")) { "\"${FrameworkColorValues.getValue(it.groupValues[1])}\"" }

    // android:width="24.0dip" → android:width="24.0dp"
    private val DipSize = Regex("""(android:(?:width|height)=")([0-9.]+)dip"""")

    // every vector's size and its unit, which Compose resources needs to be dp or px
    private val VectorSize = Regex("""android:(?:width|height)="([0-9.]+)([a-z]*)"""")

    private sealed interface Value
    private data class Text(val text: String) : Value
    private data class Plural(val items: List<Pair<String, String>>) : Value
    private data class StringList(val items: List<String>) : Value

    private fun readValues(dir: File): Map<String, Value> {
        val values = linkedMapOf<String, Value>()
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        dir.listFiles { f -> f.extension == "xml" }.orEmpty().sortedBy { it.name }.forEach { file ->
            val root = runCatching { factory.newDocumentBuilder().parse(file).documentElement }.getOrNull() ?: return@forEach
            if (root.tagName != "resources") return@forEach
            val children = root.childNodes
            for (i in 0 until children.length) {
                val node = children.item(i) as? Element ?: continue
                val name = node.getAttribute("name").takeIf { it.isNotEmpty() } ?: continue
                values[name] = when (node.tagName) {
                    "string" -> Text(decode(node.textContent))
                    "plurals" -> Plural(node.items().map { it.getAttribute("quantity") to decode(it.textContent) })
                    "string-array" -> StringList(node.items().map { decode(it.textContent) })
                    else -> continue
                }
            }
        }
        return values
    }

    private fun Element.items(): List<Element> =
        (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }.filter { it.tagName == "item" }

    // aapt2's reading: backslash escapes, double quotes keep whitespace and are dropped, other whitespace runs become one space
    private fun decode(raw: String): String {
        val out = StringBuilder()
        var quoted = false
        var space = false
        var i = 0
        fun emit(c: Char) {
            if (space) out.append(' ')
            space = false
            out.append(c)
        }
        while (i < raw.length) {
            val c = raw[i]
            when {
                c == '\\' && i + 1 < raw.length -> {
                    val next = raw[i + 1]
                    if (next == 'u' && i + 6 <= raw.length && raw.substring(i + 2, i + 6).all { it.isLetterOrDigit() }) {
                        raw.substring(i + 2, i + 6).toIntOrNull(16)?.let { emit(it.toChar()) }
                        i += 6
                        continue
                    }
                    emit(when (next) { 'n' -> '\n'; 't' -> '\t'; else -> next })
                    i += 2
                    continue
                }
                c == '"' -> quoted = !quoted
                !quoted && c.isWhitespace() -> if (out.isNotEmpty()) space = true
                else -> emit(c)
            }
            i++
        }
        return out.toString()
    }

    // the inverse of compose's handleSpecialCharacters
    private fun encode(text: String): String =
        text.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")

    private fun write(file: File, values: Map<String, Value>, lookup: (String) -> Value?) {
        fun resolve(text: String, depth: Int = 0): String =
            if (depth < 4 && text.startsWith("@string/")) {
                (lookup(text.removePrefix("@string/")) as? Text)?.let { resolve(it.text, depth + 1) } ?: text
            } else {
                text
            }
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val root = doc.createElement("resources").also(doc::appendChild)
        for ((name, value) in values) {
            val element = when (value) {
                is Text -> doc.createElement("string").apply { textContent = encode(resolve(value.text)) }
                is Plural -> doc.createElement("plurals").apply {
                    value.items.forEach { (quantity, text) ->
                        appendChild(doc.createElement("item").apply { setAttribute("quantity", quantity); textContent = encode(resolve(text)) })
                    }
                }
                is StringList -> doc.createElement("string-array").apply {
                    value.items.forEach { text -> appendChild(doc.createElement("item").apply { textContent = encode(resolve(text)) }) }
                }
            }
            element.setAttribute("name", name)
            root.appendChild(element)
        }
        file.parentFile.mkdirs()
        TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        }.transform(DOMSource(doc), StreamResult(file))
    }
}

val bridgedResources = layout.buildDirectory.dir("generated/mochyComposeResources/commonMain")
val bridgeAppResources = tasks.register("bridgeAppResources") {
    val appRes = rootProject.layout.projectDirectory.dir("app/src/main/res")
    val sharedRes = layout.projectDirectory.dir("src/commonMain/composeResources")
    val out = bridgedResources
    inputs.dir(appRes).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(sharedRes).withPathSensitivity(PathSensitivity.RELATIVE)
    // the shared Kotlin sources decide which nodpi pictures are copied (see ResourceBridge.run)
    val sharedKotlin = fileTree(layout.projectDirectory.dir("src")) { include("**/*.kt") }
    inputs.files(sharedKotlin).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(out)
    outputs.cacheIf { true }
    doLast { ResourceBridge.run(appRes.asFile, sharedRes.asFile, out.get().asFile, sharedKotlin.files) }
}

compose.resources {
    customDirectory("iosMain", layout.dir(prepareIosFonts.map { it.destinationDir }))
    customDirectory("commonMain", layout.dir(bridgeAppResources.map { bridgedResources.get().asFile }))
    // a library does not generate Res by default; every platform's UI reads strings through it
    generateResClass = always
    publicResClass = false
    packageOfResClass = "com.mochyy.mochymusic.shared.resources"
}
