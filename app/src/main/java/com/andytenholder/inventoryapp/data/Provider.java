package com.andytenholder.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * Created by Andy Tenholder on 7/5/2017.
 */

public class Provider extends ContentProvider {
    /** Tag for the log messages */
    public static final String LOG_TAG = Provider.class.getSimpleName();

    /** URI matcher code for the content URI for the inventory table */
    private static final int INV = 100;

    /** URI matcher code for the content URI for a single item in the inventory table */
    private static final int INV_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form "content://com.andytenholder.inventoryapp/inventory" will map to the
        // integer code {@link #INV}. This URI is used to provide access to MULTIPLE rows
        // of the inventory table.
        sUriMatcher.addURI(Contract.CONTENT_AUTHORITY, Contract.PATH_INVENTORY, INV);

        // The content URI of the form "content://com.andytenholder.inventoryapp/inventory/#" will map to the
        // integer code {@link #INV_ID}. This URI is used to provide access to ONE single row
        // of the invetory table.
        sUriMatcher.addURI(Contract.CONTENT_AUTHORITY, Contract.PATH_INVENTORY + "/#", INV_ID);
    }

    /** Database helper object */
    private DBHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new DBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case INV:
                // For the INV code, query the table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the table.
                cursor = database.query(Contract.InventoryEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case INV_ID:
                // For the INV_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.andytenholder.inventoryapp/inventory/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = Contract.InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // This will perform a query on the table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(Contract.InventoryEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INV:
                return insertItem(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert an item into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertItem(Uri uri, ContentValues values) {
        // Check that the name is not null
        String name = values.getAsString(Contract.InventoryEntry.COLUMN_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Item requires a name");
        }

        // Check that the supplier is not null
        String supplier = values.getAsString(Contract.InventoryEntry.COLUMN_SUPPLIER);
        if (supplier == null) {
            throw new IllegalArgumentException("Item requires a supplier");
        }

        // Check that the price is not less than 0
        int price = values.getAsInteger(Contract.InventoryEntry.COLUMN_PRICE);
        if (price < 0) {
            throw new IllegalArgumentException("Item requires valid price");
        }

        // Check that the quantity is not less than 0
        int quantity = values.getAsInteger(Contract.InventoryEntry.COLUMN_QUANTITY);
        if (quantity < 0) {
            throw new IllegalArgumentException("Item requires valid quantity");
        }


        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new item with the given values
        long id = database.insert(Contract.InventoryEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the item content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INV:
                return updateINV(uri, contentValues, selection, selectionArgs);
            case INV_ID:
                // For the INV_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = Contract.InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateINV(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update items in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more items).
     * Return the number of rows that were successfully updated.
     */
    private int updateINV(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // If the Contract.InventoryEntry.COLUMN_NAME key is present,
        // check that the name value is not null.
        if (values.containsKey(Contract.InventoryEntry.COLUMN_NAME)) {
            String name = values.getAsString(Contract.InventoryEntry.COLUMN_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Item requires a name");
            }
        }

        // If the Contract.InventoryEntry.COLUMN_SUPPLIER key is present,
        // Check that the supplier is not null
        if (values.containsKey(Contract.InventoryEntry.COLUMN_SUPPLIER)) {
            String supplier = values.getAsString(Contract.InventoryEntry.COLUMN_SUPPLIER);
            if (supplier == null) {
                throw new IllegalArgumentException("Item requires a supplier");
            }
        }

        // If the Contract.InventoryEntry.COLUMN_PRICE key is present,
        // Check that the price is not null and is not less than or equal to 0
        if (values.containsKey(Contract.InventoryEntry.COLUMN_PRICE)) {
            // Check that the price is not less than 0
            int price = values.getAsInteger(Contract.InventoryEntry.COLUMN_PRICE);
            if (price < 0) {
                throw new IllegalArgumentException("Item requires valid price");
            }
        }


        if (values.containsKey(Contract.InventoryEntry.COLUMN_QUANTITY)) {
            // Check that the quantity is not less than 0
            int quantity = values.getAsInteger(Contract.InventoryEntry.COLUMN_QUANTITY);
            if (quantity < 0) {
                throw new IllegalArgumentException("Item requires valid quantity");
            }
        }

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(Contract.InventoryEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INV:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(Contract.InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case INV_ID:
                // Delete a single row given by the ID in the URI
                selection = Contract.InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(Contract.InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INV:
                return Contract.InventoryEntry.CONTENT_LIST_TYPE;
            case INV_ID:
                return Contract.InventoryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}
