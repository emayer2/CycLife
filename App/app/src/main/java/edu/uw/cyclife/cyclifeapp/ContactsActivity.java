package edu.uw.cyclife.cyclifeapp;

import android.app.ListActivity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Keegan Griffee on 4/30/2017.
 */

public class ContactsActivity extends ListActivity{
    ArrayList<String> name1 = new ArrayList<String>();
    ArrayList<String> phno1 = new ArrayList<String>();
    protected static final String TAG = null;
    public String[] Contacts = {};
    public int[] to = {};
    public ListView myListView;


    //@SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final Button done_Button = (Button) findViewById(R.id.kill_button);
        final Button clear_Button =(Button) findViewById(R.id.kill_button);
        Cursor mCursor = getContacts();

        startManagingCursor(mCursor);
        ListAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_multiple_choice, mCursor,
                Contacts = new String[] {ContactsContract.Contacts.DISPLAY_NAME },
                to = new int[] { android.R.id.text1 });

        setListAdapter(adapter);
        myListView = getListView();
        myListView.setItemsCanFocus(false);
        myListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        clear_Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"Selection Cleared", Toast.LENGTH_SHORT).show();
                ClearSelections();
            }
        });

        /** When 'Done' Button Pushed: **/

        done_Button.setOnClickListener(new View.OnClickListener() {

            public void onClick (View v){
                String name = null;
                String number = null;
                long [] ids = myListView.getCheckedItemIds();
                for(long id : ids) {
                    Cursor contact = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { id + "" }, null);
                    while(contact.moveToNext()){
                        name = contact.getString(contact.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        //name+=name;
                        number = contact.getString(contact.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        //number+=number;
                    }
                    Toast.makeText(getApplicationContext(), "Name: " +name + "\n" + "Number: " + number , Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void ClearSelections() {
        int count = this.myListView.getAdapter().getCount();
        for (int i = 0; i < count; i++) {
            this.myListView.setItemChecked(i, false);
        }
    }

    @SuppressWarnings("deprecation")
    private Cursor getContacts() {
        // Run query
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        String[] projection = new String[] { ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME};
        String selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + " = '"
                + ("1") + "'";
        String[] selectionArgs = null;
        String sortOrder = ContactsContract.Contacts.DISPLAY_NAME
                + " COLLATE LOCALIZED ASC";

        return managedQuery(uri, projection, selection, selectionArgs,
                sortOrder);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
