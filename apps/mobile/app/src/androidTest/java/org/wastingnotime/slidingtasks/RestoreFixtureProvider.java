package org.wastingnotime.slidingtasks;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Read-only test APK fixture, absent from application builds. Java allows standalone provider loading. */
public class RestoreFixtureProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "application/json"; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException("Read only"); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        File file = new File(getContext().getCacheDir(), "restore-fixture.json");
        String data = "valid".equals(uri.getLastPathSegment()) ?
            "{\"formatVersion\":1,\"tasks\":[{\"id\":\"restored\",\"title\":\"Restored task\",\"type\":\"ONE_TIME\",\"active\":true," +
            "\"createdOn\":\"2026-10-11\",\"schedule\":{\"kind\":\"ONCE\",\"startsOn\":\"2026-10-11\",\"interval\":1,\"days\":[\"SUNDAY\"]}}]," +
            "\"cards\":[],\"events\":[]}" : "invalid backup";
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(data.getBytes(StandardCharsets.UTF_8));
        } catch (IOException error) {
            throw new FileNotFoundException(error.getMessage());
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
