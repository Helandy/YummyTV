# Optional desktop/JVM classes referenced by transitive parser dependencies.
-dontwarn com.google.re2j.Matcher
-dontwarn com.google.re2j.Pattern
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn javax.script.ScriptEngineFactory
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# ML Kit (сканер QR для входа на ТВ) находит свои компоненты через ComponentDiscovery —
# рефлексией по именам из манифеста. R8 вырезал конструкторы регистраторов, MlKitContext
# оставался пустым, и GmsBarcodeScanning.getClient падал с NPE при открытии «Войти на ТВ».
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
