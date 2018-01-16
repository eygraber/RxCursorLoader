package com.doctoror.rxcursorloader;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.test.mock.MockContentProvider;

final class TestContentProvider extends MockContentProvider {

    static final String AUTHORITY = "com.doctoror.rxcursorloader.demo.provider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(
            @NonNull Uri uri,
            @Nullable String[] projection,
            @Nullable String selection,
            @Nullable String[] selectionArgs,
            @Nullable String sortOrder) {
        final MatrixCursor demoResult = new MatrixCursor(
                RxCursorLoaderInstrumentedTest.QUERY_COLUMNS);

        demoResult.addRow(new String[]{
                "1",
                "3",
                "Darkspace"
        });
        demoResult.addRow(new String[]{
                "2",
                "2",
                "Paysage d'Hiver"
        });
        demoResult.addRow(new String[]{
                "3",
                "6",
                "KMFDM"
        });
        demoResult.addRow(new String[]{
                "4",
                "4",
                "Mechina"
        });
        return demoResult;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return "demo";
    }

    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}