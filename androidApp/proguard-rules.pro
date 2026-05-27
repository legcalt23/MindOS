# ============================================================
# ProGuard / R8 rules for MindOS
# ============================================================

# ---------- Kotlin & Coroutines ----------
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# ---------- Compose ----------
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ---------- Firebase (general) ----------
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }

# ---------- Firebase App Check ----------
# Keep App Check providers and token types so R8 doesn't strip them
-keep class com.google.firebase.appcheck.** { *; }
-dontwarn com.google.firebase.appcheck.**

# Play Integrity provider (release builds)
-keep class com.google.firebase.appcheck.playintegrity.** { *; }
-dontwarn com.google.firebase.appcheck.playintegrity.**

# Google Play Integrity API (transitive dep of Play Integrity provider)
-keep class com.google.android.play.core.integrity.** { *; }
-dontwarn com.google.android.play.core.integrity.**

# ---------- Firebase App Check Debug provider ----------
# NOTE: The debug provider is only included in debugImplementation, so these
# rules are harmless in release but prevent issues if ever misconfigured.
-dontwarn com.google.firebase.appcheck.debug.**

# ---------- dev.gitlive Firebase KMP SDK ----------
# Ensure KMP bridge classes are not removed
-keep class dev.gitlive.firebase.** { *; }
-dontwarn dev.gitlive.firebase.**

# ---------- Google Play Services ----------
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ---------- Serialization (Kotlinx) ----------
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

