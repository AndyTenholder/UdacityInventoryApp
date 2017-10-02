package com.andytenholder.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.andytenholder.inventoryapp.data.Contract;

/**
 * Created by Andy Tenholder on 7/5/2017.
 */

public class InvCursorAdapter extends CursorAdapter {


    public InvCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }


    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        final Button saleButton = (Button) view.findViewById(R.id.sale_button);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView summaryTextView = (TextView) view.findViewById(R.id.summary);
        ImageView PictureImageView = (ImageView) view.findViewById(R.id.item_image);

        // Find the columns of attributes that we're interested in
        int priceComlumnIndex = cursor.getColumnIndex(Contract.InventoryEntry.COLUMN_PRICE);
        int pictureComlumnIndex = cursor.getColumnIndex(Contract.InventoryEntry.COLUMN_PICTURE);
        int nameColumnIndex = cursor.getColumnIndex(Contract.InventoryEntry.COLUMN_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(Contract.InventoryEntry.COLUMN_QUANTITY);

        // Read the attributes from the Cursor for the current item
        int itemPrice = cursor.getInt(priceComlumnIndex);
        String itemName = cursor.getString(nameColumnIndex);
        String itemPicture = cursor.getString(pictureComlumnIndex);
        int itemQuantity = cursor.getInt(quantityColumnIndex);

        // Update the TextViews with the attributes for the current item
        priceTextView.setText(String.valueOf(itemPrice));
        nameTextView.setText(itemName);
        summaryTextView.setText(String.valueOf(itemQuantity));
        if (itemPicture != null) {
            Uri imgUri = Uri.parse(itemPicture);
            PictureImageView.setImageURI(imgUri);
        }

        final int inventoryItemId = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.InventoryEntry._ID));
        final int inventoryItemQuantity = cursor.getInt(cursor.getColumnIndex(Contract.InventoryEntry.COLUMN_QUANTITY));




        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inventoryItemQuantity > 0) {
                    int quantity = inventoryItemQuantity - 1;
                    ContentValues values = new ContentValues();
                    values.put(Contract.InventoryEntry.COLUMN_QUANTITY, quantity);
                    Uri newUri = ContentUris.withAppendedId(Contract.InventoryEntry.CONTENT_URI, inventoryItemId);
                    context.getContentResolver().update(newUri, values, null, null);
                } else {
                    Toast.makeText(context, R.string.no_quantity, Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
