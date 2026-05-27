-dontwarn java.lang.invoke.StringConcatFactory
-overloadaggressively
-repackageclasses ''
-allowaccessmodification

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*
-renamesourcefileattribute SourceFile