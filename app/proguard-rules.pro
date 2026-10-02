# ── Firestore ──────────────────────────────────────────────────────────────
# Documents are (de)serialized into these classes by reflection, using property names,
# @PropertyName annotations and the no-arg constructor - so none of that may be renamed/removed.
-keepattributes Signature,*Annotation*
-keepclassmembers class com.yunok.walzi.data.model.** { *; }
-keep class com.yunok.walzi.data.model.** { <init>(); }

# Firebase / Play services ship their own consumer rules. (A blanket
# "-keep class com.google.firebase.** { *; }" used to be here; it defeated R8 shrinking of the
# whole Firebase SDK and isn't needed.)

# ── Crashlytics ────────────────────────────────────────────────────────────
# Keep file names + line numbers so release stack traces can be de-obfuscated.
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
