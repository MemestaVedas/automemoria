# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao

# Hilt
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ViewComponentBuilder { *; }

# Timber
-keep class timber.log.Timber { *; }

# Supabase / Ktor / Serialization
-keep class kotlinx.serialization.json.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
