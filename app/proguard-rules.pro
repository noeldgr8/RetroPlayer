# WorkManager (used by Glance to update the home screen widgets) creates these classes by name
# through their constructors. R8 strips the constructors otherwise, and the widgets stay stuck
# on the loading spinner.
-keep class * extends androidx.work.InputMerger {
    public <init>();
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
