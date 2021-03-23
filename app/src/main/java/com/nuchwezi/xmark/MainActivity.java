package com.nuchwezi.xmark;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Intents;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.qrcode.QRCodeWriter;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import androidx.core.content.ContextCompat;
import fr.tkeunebr.gravatar.Gravatar;

public class MainActivity extends AppCompatActivity {

    private static final int QRCODE_SIZE = 800;
    public static final String TAG = "XMARK";
    private static final int WEBVIEW_HEIGHT = 400;
    private Handler handler;

    enum ScanIntent {
        CreateContact,
        InitiateCall,
        InitiateSMS,
        InitiateEmail,
        ViewImage,
        ViewXMarkInfo
    }

    ScanIntent currentScanIntent = ScanIntent.CreateContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();

        if(getOrRequestWriteStoragePermission()) {
            checkAppUpdates();
        }

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
                    if(getOrRequestCreateContactPermission()) {
                        currentScanIntent = ScanIntent.CreateContact;
                        launchScanner();
                    }
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

        if (id == R.id.action_sharexmark) {
            shareXMark();
            return true;
        }

        if (id == R.id.action_about) {
            showAbout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void scanXMarkAndCall(View view) {
        if(getOrRequestCallPermission()) {
            currentScanIntent = ScanIntent.InitiateCall;
            launchScanner();
        }
    }

    public void scanXMarkAndSMS(View view) {
        if(getOrRequestSMSPermission()) {
            currentScanIntent = ScanIntent.InitiateSMS;
            launchScanner();
        }
    }

    public void scanXMarkAndEmail(View view) {
        currentScanIntent = ScanIntent.InitiateEmail;
        launchScanner();
    }

    public void scanXMarkAndViewPic(View view) {
        currentScanIntent = ScanIntent.ViewImage;
        launchScanner();
    }

    private void shareXMark() {
        showQRCODE(Utility.APK.APK_DOWNLOAD_PATH, "Share XMark via URL");
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
            xmark.put(XMark.PARAMS.DisplayName, getSetting(PREFERENCES.KEY_DISPLAY_NAME, "XMark: Unknown", this));
            xmark.put(XMark.PARAMS.MobileNumber, getSetting(PREFERENCES.KEY_MOBILE, "XMark: Unknown", this));
            xmark.put(XMark.PARAMS.Email, getSetting(PREFERENCES.KEY_EMAIL, "XMark: Unknown", this));
            xmark.put(XMark.PARAMS.Company, getSetting(PREFERENCES.KEY_COMPANY, "XMark: Unknown", this));
            xmark.put(XMark.PARAMS.JobTitle, getSetting(PREFERENCES.KEY_TITLE, "XMark: Unknown", this));
        } catch (JSONException e) {
            e.printStackTrace();
        }


        showQRCODE(xmark, "Preview XMark");

    }

    private void showQRCODE(String data, String title) {
        //set up dialog
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.view_my_xmark);
        dialog.setTitle(title);
        dialog.setCancelable(true);
        //there are a lot of settings, for dialog, check them all out!

        //set up image view
        ImageView img = (ImageView) dialog.findViewById(R.id.imageViewXMark);
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, QRCODE_SIZE, QRCODE_SIZE);
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
                dialog.dismiss();
            }
        });
        //now that the dialog is set up, it's time to show it
        dialog.show();
    }

    private void showQRCODE(JSONObject jXMark, String title) {
        //set up dialog
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.view_my_xmark);
        dialog.setTitle(title);
        dialog.setCancelable(true);
        //there are a lot of settings, for dialog, check them all out!

        renderDetails(jXMark,dialog);

        String data = jXMark.toString();

        //set up image view
        ImageView img = (ImageView) dialog.findViewById(R.id.imageViewXMark);
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, QRCODE_SIZE, QRCODE_SIZE);
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
                dialog.dismiss();
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

                    switch (currentScanIntent) {
                        case CreateContact: {
                            parseAndCreateContact(jsonObject);
                            break;
                        }
                        case InitiateCall: {
                            parseAndMakeCall(jsonObject);
                            break;
                        }
                        case InitiateSMS: {
                            parseAndSendSMS(jsonObject);
                            break;
                        }
                        case InitiateEmail: {
                            parseAndSendEmail(jsonObject);
                            break;
                        }
                        case ViewImage: {
                            parseAndShowImage(jsonObject);
                            break;
                        }
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                    Utility.showAlert("INVALID XMark", "It is possible that this is an invalid XMark. Please ensure to scan a valid XMark as generated from \n\nxmark.chwezi.tech", this);
                }

            } else if (resultCode == RESULT_CANCELED) {

            }
        }
    }

    private boolean hasPermissionCreateContact() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private boolean getOrRequestCreateContactPermission() {
        if(hasPermissionCreateContact()){
            return true;
        }else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CONTACTS}, 101);
        }

        return false;
    }

    private boolean hasPermissionSMS() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private boolean getOrRequestSMSPermission() {
        if(hasPermissionSMS()){
            return true;
        }else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 101);
        }

        return false;
    }

    private boolean hasPermissionCall() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private boolean getOrRequestCallPermission() {
        if(hasPermissionCall()){
            return true;
        }else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 101);
        }

        return false;
    }


    private boolean hasPermissionWriteStorage() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private boolean getOrRequestWriteStoragePermission() {
        if(hasPermissionWriteStorage()){
            return true;
        }else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }

        return false;
    }


    private void parseAndShowImage(JSONObject jXMark) {

        String EmailAddress = null;
        try {
            EmailAddress = jXMark.getString(XMark.PARAMS.Email);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (EmailAddress != null) {

            //set up dialog
            final Dialog dialog = new Dialog(MainActivity.this);
            dialog.setContentView(R.layout.view_my_xmark);
            dialog.setTitle("The XMark Gravatar");
            dialog.setCancelable(true);
            //there are a lot of settings, for dialog, check them all out!

            renderDetails(jXMark,dialog);


            //set up button
            Button button = (Button) dialog.findViewById(R.id.btnCancel);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            //set up image view
            ImageView img = (ImageView) dialog.findViewById(R.id.imageViewXMark);

            String gravatarUrl = Gravatar.init().with(EmailAddress).size(QRCODE_SIZE).build();
            Log.d("XMARK",gravatarUrl);

            Picasso.with(this)
                    .load(gravatarUrl)
                    .placeholder(R.drawable.loading)
                    .error(R.drawable.erorr)
                    .into(img);

            //now that the dialog is set up, it's time to show it
            dialog.show();

        }else {
            Toast.makeText(this, String.format("XMARK: There is no email address information to use to fetch the contact image from Gravatar!"), Toast.LENGTH_LONG).show();
        }
    }

    private void renderDetails(JSONObject jXMark, Dialog parentView) {

        String EmailAddress = null;
        try {
            EmailAddress = jXMark.getString(XMark.PARAMS.Email);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        TextView tEmail = (TextView) parentView.findViewById(R.id.txtEmail);
        if(EmailAddress != null){
            tEmail.setText(EmailAddress);
        }else
            tEmail.setText("");


        String MobileNumber = null;
        try {
            MobileNumber = jXMark.getString(XMark.PARAMS.MobileNumber);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String DisplayName = null;
        try {
            DisplayName = jXMark.getString(XMark.PARAMS.DisplayName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        TextView tPhone = (TextView) parentView.findViewById(R.id.txtPhone);
        if(MobileNumber != null){
            tPhone.setText(MobileNumber);
        }else
            tPhone.setText("");

        TextView tName = (TextView) parentView.findViewById(R.id.txtDisplayName);
        if(DisplayName != null){
            tName.setText(DisplayName);
        }else
            tName.setText("");


        StringBuilder builder = new StringBuilder();
        try {
            builder.append(String.format("\n%s: %s\n", Utility.toTitleCase(XMark.PARAMS.Company), jXMark.getString(XMark.PARAMS.Company) ));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            builder.append(String.format("\n%s: %s\n", Utility.toTitleCase(XMark.PARAMS.JobTitle), jXMark.getString(XMark.PARAMS.JobTitle) ));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String meta = null;
        try {
            meta = jXMark.getString(XMark.PARAMS.Meta);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(meta != null) {
            //first, we try to see if meta is a json object...
            JSONObject metaDict = null;
            try {
                metaDict = new JSONObject(meta);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (metaDict != null) {

                Iterator<?> keys = metaDict.keys();

                while( keys.hasNext() ) {
                    String key = (String)keys.next();
                    try {
                        String val = metaDict.getString(key);

                        builder.append(String.format("\n%s: %s\n", Utility.toTitleCase(key), val ));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }else {
                // we try for if it's a URL...
                Uri uri = null;
                if(URLUtil.isValidUrl(meta.trim())) {
                    try {
                        uri = Uri.parse(meta.trim());

                        builder.append(String.format("\nURI: %s\n", uri));

                        //also, load page in webview...
                        LinearLayout linearLayout = (LinearLayout) parentView.findViewById(R.id.metaContainer);

                        /*final TouchyWebView web = new TouchyWebView(this);
                        web.getSettings().setJavaScriptEnabled(true);
                        web.setWebViewClient(new WebViewClient() {
                            @Override
                            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                                view.loadUrl(url);
                                return false;
                            }
                        });
                        web.loadUrl(uri.toString());*/

                        WebView web = new WebView(this);

                        web.loadUrl(uri.toString());
                        web.getSettings().setJavaScriptEnabled(true);
                        web.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
                        web.setWebViewClient(new WebViewClient() {
                            @Override
                            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                                view.loadUrl(url);
                                return true;
                            }

                            @Override
                            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error){
                            }

                            @Override
                            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                                super.onPageStarted(view, url, favicon);
                            }

                            public void onPageFinished(WebView view, String url) {
                            }
                        });


                        LinearLayout.LayoutParams webViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, WEBVIEW_HEIGHT);
                        web.setLayoutParams(webViewParams);
                        linearLayout.addView(web);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                //if uri was null, we just render as text...
                if(uri == null){
                    builder.append(String.format("\n** Meta **\n" +
                            "\n %s", meta.trim()));
                }
            }
        }

        TextView tOther = (TextView) parentView.findViewById(R.id.txtOther);
        tOther.setMovementMethod(LinkMovementMethod.getInstance());
        if(builder.length() > 0){
            tOther.setText(builder.toString());
        }else
            tOther.setText("");

    }


    private void parseAndSendEmail(JSONObject jXMark) {


        String EmailAddress = null;
        try {
            EmailAddress = jXMark.getString(XMark.PARAMS.Email);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (EmailAddress != null) {

            Intent i = new Intent(Intent.ACTION_SEND);
            i.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{ EmailAddress });
            i.putExtra(android.content.Intent.EXTRA_TEXT, "Sent using XMark");
            startActivity(Intent.createChooser(i, "Send Email"));

        }else {
            Toast.makeText(this, String.format("XMARK: There is no email address information to use to create the Email!"), Toast.LENGTH_LONG).show();
        }

    }

    private void parseAndSendSMS(JSONObject jXMark) {
        String MobileNumber = null;
        try {
            MobileNumber = jXMark.getString(XMark.PARAMS.MobileNumber);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String DisplayName = null;
        try {
            DisplayName = jXMark.getString(XMark.PARAMS.DisplayName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (MobileNumber != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);

            intent.setData(Uri.parse("sms:" + MobileNumber));

            if(DisplayName != null) {
                intent.putExtra("sms_body", String.format("Hi %s,\n\n", DisplayName));
            }

            /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Toast.makeText(this, String.format("XMARK: The Call Permission is required, in order to place this call!"), Toast.LENGTH_LONG).show();
                return;
            }*/
            startActivity(intent);
        }else {
            Toast.makeText(this, String.format("XMARK: There is no phone contact information to use to create the SMS!"), Toast.LENGTH_LONG).show();
        }
    }

    private void parseAndMakeCall(JSONObject jXMark) {
        String MobileNumber = null;
        try {
            MobileNumber = jXMark.getString(XMark.PARAMS.MobileNumber);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (MobileNumber != null) {
            Intent intent = new Intent(Intent.ACTION_CALL);

            intent.setData(Uri.parse("tel:" + MobileNumber));
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Toast.makeText(this, String.format("XMARK: The Call Permission is required, in order to place this call!"), Toast.LENGTH_LONG).show();
                return;
            }
            startActivity(intent);
        }else {
            Toast.makeText(this, String.format("XMARK: There is no phone contact information to use to place the call!"), Toast.LENGTH_LONG).show();
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
