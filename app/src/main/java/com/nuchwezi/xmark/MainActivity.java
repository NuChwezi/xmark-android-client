package com.nuchwezi.xmark;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Intents;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.qrcode.QRCodeWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int QRCODE_SIZE = 500;
    public static final String TAG = "XMARK";
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();
        checkAppUpdates();

        final Button btnScan = (Button) findViewById(R.id.btnScan);
        final int[] btnBackgrounds = new int[]{
                R.drawable.buttonshape,
                R.drawable.buttonshape_active
        };

        btnScan.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == (MotionEvent.ACTION_UP)) {
                    //Do whatever you want after press
                    btnScan.setBackgroundResource(btnBackgrounds[1]);
                    launchScanner();
                } else {
                    btnScan.setBackgroundResource(btnBackgrounds[0]);
                }
                return true;
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            showSettings();
            return true;
        }

        if (id == R.id.action_myxmark) {
            showMyXMark();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PREFERENCES {
        public static final String APP_PREFERENCES = "APP_PREFERENCES";
        public static final String KEY_DISPLAY_NAME = "PREF_KEY_SETTINGS_DISPLAY_NAME";
        public static final String KEY_MOBILE = "PREF_KEY_SETTINGS_PHONE_NUMBER";
        public static final String KEY_EMAIL = "PREF_KEY_SETTINGS_EMAIL_ADDRESS";
        public static final String KEY_COMPANY = "PREF_KEY_SETTINGS_COMPANY";
        public static final String KEY_TITLE = "PREF_KEY_SETTINGS_TITLE";
    }

    public static String getSetting(String KEY, String DEFAULT, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                PREFERENCES.APP_PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getString(KEY, DEFAULT);
    }

    private void showMyXMark() {
        JSONObject xmark = new JSONObject();
        try {
            xmark.put(XMark.PARAMS.DisplayName,getSetting(PREFERENCES.KEY_DISPLAY_NAME,"XMark: Unknown",this));
            xmark.put(XMark.PARAMS.MobileNumber,getSetting(PREFERENCES.KEY_MOBILE,"XMark: Unknown",this));
            xmark.put(XMark.PARAMS.Email,getSetting(PREFERENCES.KEY_EMAIL,"XMark: Unknown",this));
            xmark.put(XMark.PARAMS.Company,getSetting(PREFERENCES.KEY_COMPANY,"XMark: Unknown",this));
            xmark.put(XMark.PARAMS.JobTitle,getSetting(PREFERENCES.KEY_TITLE,"XMark: Unknown",this));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String jXMark = xmark.toString();
        showXMark(jXMark);

    }

    private void showXMark(String jXMark) {
        //set up dialog
        Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.view_my_xmark);
        dialog.setTitle("Preview XMark");
        dialog.setCancelable(true);
        //there are a lot of settings, for dialog, check them all out!

        //set up image view
        ImageView img = (ImageView) dialog.findViewById(R.id.imageViewXMark);
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(jXMark, BarcodeFormat.QR_CODE, QRCODE_SIZE, QRCODE_SIZE);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            img.setImageBitmap(bmp);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        //set up button
        Button button = (Button) dialog.findViewById(R.id.btnCancel);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //now that the dialog is set up, it's time to show it
        dialog.show();
    }

    private void showSettings() {
        Intent _intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(_intent);
    }

    private void launchScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt(getString(R.string.message_scan));
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            if (intent != null) {
                final String scannedJSON = intent.getStringExtra(Intents.Scan.RESULT);
                String format = intent.getStringExtra(Intents.Scan.RESULT_FORMAT);

                try {
                    JSONObject jsonObject = new JSONObject(scannedJSON);
                    parseAndCreateContact(jsonObject);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else if (resultCode == RESULT_CANCELED) {

            }
        }
    }


    private void showAbout() {

        Utility.showAlert(
                this.getString(R.string.app_name),
                String.format("Version %s (Build %s)\n\n%s",
                        Utility.getVersionName(this),
                        Utility.getVersionNumber(this),
                        this.getString(R.string.powered_by)),
                R.mipmap.ic_launcher, this);

    }

    private void parseAndCreateContact(JSONObject jXMark) {
        String DisplayName = null;
        try {
            DisplayName = jXMark.getString(XMark.PARAMS.DisplayName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String MobileNumber = null;
        try {
            MobileNumber = jXMark.getString(XMark.PARAMS.MobileNumber);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /*
        String HomeNumber = "1111";
        String WorkNumber = "2222";
        */

        String emailID = null;
        try {
            emailID = jXMark.getString(XMark.PARAMS.Email);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String company = null;
        try {
            company = jXMark.getString(XMark.PARAMS.Company);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String jobTitle = null;
        try {
            jobTitle = jXMark.getString(XMark.PARAMS.JobTitle);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        ops.add(ContentProviderOperation.newInsert(
                ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        //------------------------------------------------------ Names
        if (DisplayName != null) {
            ops.add(ContentProviderOperation.newInsert(
                    ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(
                            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                            DisplayName).build());
        }

        //------------------------------------------------------ Mobile Number
        if (MobileNumber != null) {
            ops.add(ContentProviderOperation.
                    newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, MobileNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());
        }

        //------------------------------------------------------ Home Numbers
        /*
        if (HomeNumber != null) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, HomeNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
                    .build());
        }

        //------------------------------------------------------ Work Numbers
        if (WorkNumber != null) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, WorkNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
                    .build());
        }
        */

        //------------------------------------------------------ Email
        if (emailID != null) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.DATA, emailID)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                    .build());
        }

        //------------------------------------------------------ Organization
        if (!company.equals("") && !jobTitle.equals("")) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                    .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                    .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, jobTitle)
                    .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                    .build());
        }

        // Asking the Contact provider to create a new contact
        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            Toast.makeText(this, String.format("XMARK: %s has been exported into your contacts", DisplayName), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAppUpdates() {
        final int versionCODE = Utility.getVersionNumber(this);


        Utility.getPublishedVersionInfo(MainActivity.this, new ParametricCallbackJSONObject() {
            @Override
            public void call(JSONObject publishedUpdateInfo) {
                if (publishedUpdateInfo == null)
                    return;

                if (Utility.isPublishedVersionNewer(versionCODE,
                        publishedUpdateInfo)) {

                    boolean autoUpdate = publishedUpdateInfo.optBoolean(
                            Utility.APK.KEY_AUTOUPDATE,
                            Utility.APK.DEFAULT_AUTOUPDATE);
                    final boolean forceUpdate = publishedUpdateInfo.optBoolean(
                            Utility.APK.KEY_FORCEUPDATE,
                            Utility.APK.DEFAULT_FORCEUPDATE);

                    final String updateMessage = Utility
                            .getUpdateMessage(publishedUpdateInfo);

                    if (autoUpdate || forceUpdate) {

                        final AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(
                                MainActivity.this);
                        myAlertDialog.setTitle(String.format("%s UPDATE",
                                MainActivity.this
                                        .getString(R.string.app_name)));
                        myAlertDialog.setIcon(R.drawable.ic_menu_autoupdate);
                        myAlertDialog
                                .setMessage(String
                                        .format("%s\nInstalled Version: %s\n\nUpgrading is %s!",
                                                updateMessage,
                                                Utility.getVersionName(MainActivity.this),
                                                forceUpdate ? "REQUIRED"
                                                        : "RECOMMENDED"));
                        myAlertDialog.setPositiveButton("Yes, Upgrade",
                                new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface arg0,
                                                        int arg1) {
                                        Utility.autoUpdateAPK(
                                                MainActivity.this
                                                        .getString(R.string.app_name),
                                                Utility.APK.APK_UPDATE_URI,
                                                MainActivity.this, handler);
                                    }
                                });
                        myAlertDialog.setNegativeButton("No, Quit!",
                                new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface arg0,
                                                        int arg1) {
                                        if (forceUpdate) {
                                            Utility.showAlert(
                                                    String.format(
                                                            "%s UPDATE",
                                                            MainActivity.this
                                                                    .getString(R.string.app_name)),
                                                    "Upgrading to this version is Mandatory! Sorry, can't proceed without this update...",
                                                    R.mipmap.ic_error,
                                                    MainActivity.this);
                                            MainActivity.this.finish();
                                        }
                                    }
                                });
                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                myAlertDialog.show();

                            }

                        });

                    } else {

                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                Utility.showAlert(
                                        String.format(
                                                "%s UPDATE",
                                                MainActivity.this
                                                        .getString(R.string.app_name)),
                                        String.format(
                                                "%s\nInstalled Version: %s\n\nYou may upgrade anytime using the Upgrade option on the menu...!",
                                                updateMessage,
                                                Utility.getVersionName(MainActivity.this)),
                                        R.drawable.ic_menu_autoupdate,
                                        MainActivity.this);

                            }

                        });
                    }
                }
            }
        });


    }

}
