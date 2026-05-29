# ============================================================
# ProGuard / R8 rules for MindOS
# ============================================================

# ---------- Kotlin ----------
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# ---------- Coroutines ----------
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ---------- Compose ----------
# R8 full mode strips Compose stability metadata; keep @Stable/@Immutable markers.
-dontwarn androidx.compose.**
-keep,allowshrinking class androidx.compose.** { *; }

# ---------- Firebase ----------
# Firebase uses reflection for serialization — keep model classes and public API.
-dontwarn com.google.firebase.**
-keep class com.google.firebase.FirebaseApp { *; }
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.storage.** { *; }
-keep class com.google.firebase.functions.** { *; }

# ---------- Firebase App Check ----------
-keep class com.google.firebase.appcheck.** { *; }
-dontwarn com.google.firebase.appcheck.**
-keep class com.google.firebase.appcheck.playintegrity.** { *; }
-dontwarn com.google.firebase.appcheck.playintegrity.**
-dontwarn com.google.firebase.appcheck.debug.**

# Google Play Integrity API (transitive dep)
-keep class com.google.android.play.core.integrity.** { *; }
-dontwarn com.google.android.play.core.integrity.**

# ---------- dev.gitlive Firebase KMP SDK ----------
-keep class dev.gitlive.firebase.** { *; }
-dontwarn dev.gitlive.firebase.**

# ---------- Google Play Services ----------
-dontwarn com.google.android.gms.**
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# ---------- Kotlinx Serialization ----------
-keepattributes RuntimeVisibleAnnotations
-keep class kotlinx.serialization.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**
