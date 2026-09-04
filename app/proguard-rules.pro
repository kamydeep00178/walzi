# Firestore model classes are (de)serialized via reflection - keep their members.
-keepclassmembers class com.yunok.walzi.data.model.** {
  *;
}
-keepclassmembers class com.yunok.walzi.domain.model.** {
  *;
}
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
