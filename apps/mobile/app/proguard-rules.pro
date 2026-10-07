# WorkManager persists this class name in its database. Keep it stable across
# updates so backup jobs scheduled by earlier releases can still instantiate it.
-keep class org.wastingnotime.slidingtasks.data.BackupWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
