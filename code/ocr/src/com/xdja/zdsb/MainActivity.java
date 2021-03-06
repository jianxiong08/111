package com.xdja.zdsb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.xdja.zdsb.utils.AuthInterface;
import com.xdja.zdsb.utils.AuthTools;
import com.xdja.zdsb.utils.FileUtils;
import com.xdja.zdsb.utils.SharePrefUtil;
import com.xdja.zdsb.utils.Zzlog;
import com.xdja.zdsb.view.CarPlateActivity;
import com.xdja.zdsb.view.DriverLicenseActivity;
import com.xdja.zdsb.view.IDCardRecognizeActivity;
import com.xdja.zdsb.view.PassportActivity;
import com.xdja.zdsb.view.PictureRecognizeActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static final int CAR_PLATE_REQUEST = 1; // request recognize car plate

    private static final int ID_CARD_REQUEST = 2;

    private static final int DRIVER_LICENSE_REQUEST = 3;

    private static final int PASSPORT_REQUEST = 4;

    private static final int TAKE_PHOTO_WITH_DATA = 5; // take photo use system camera.

    private static final int GET_IMAGE_ON_DEVICE = 6; // get image file from devices.

    private static final int RECOGNIZE_PIC = 7; // request 

    private String takePicFileName;

    private int picType = 0;

    AlertDialog alertDialog; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ocr_activity_main);

        // clear pic_path
        SharePrefUtil.saveString(this, "pic_path", "");
    }

    private void authProcess() {
        Zzlog.out(TAG, "authProcess()");
        final AuthTools authTools = new AuthTools(this, authInterface);
        authTools.start();
    }

    public void onCarPlateLayout(View view) {
        Zzlog.out(TAG, "onCarPlateLayout");
        Intent intent = new Intent(this, CarPlateActivity.class);
        intent.putExtra("packagename", "com.xdja");
        startActivityForResult(intent, CAR_PLATE_REQUEST);
    }

    public void onIDCardLayout(View view) {
        Intent intent = new Intent(this, IDCardRecognizeActivity.class);

        intent.putExtra("packagename", "com.xdja");
        startActivityForResult(intent, ID_CARD_REQUEST);
        Zzlog.out(TAG, "onIDCardLayout");
    }

    public void onDriverLicenseLayout(View view) {
        Intent intent = new Intent(this, DriverLicenseActivity.class);
        startActivityForResult(intent, DRIVER_LICENSE_REQUEST);
        Zzlog.out(TAG, "onDriverLisenceLayout");
    }

    public void onPassportLayout(View view) {
        Zzlog.out(TAG, "onPassportLayout");
        Intent intent = new Intent(this, PassportActivity.class);
        startActivityForResult(intent, PASSPORT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case CAR_PLATE_REQUEST:
                String number = null;
                try {
                    number = data.getStringExtra("number");
                } catch (Exception e) {
                    e.printStackTrace();Zzlog.eOut(e);;
                }
                Zzlog.out(TAG, "number = " + number);
                break;
            case ID_CARD_REQUEST:
            case DRIVER_LICENSE_REQUEST:
            case PASSPORT_REQUEST:
                String result = null;
                try {
                    result = data.getStringExtra("data");
                } catch (Exception e) {
                    e.printStackTrace();
                    Zzlog.eOut(e);;
                }
                Zzlog.out(TAG, "result = " + result);

                break;

            case TAKE_PHOTO_WITH_DATA:
                Zzlog.out(TAG, "Image saved to:" + takePicFileName);

                if (takePicFileName == null) {
                    takePicFileName = SharePrefUtil.getString(this, "pic_path", "");
                    if (TextUtils.isEmpty(takePicFileName)) {
                        Zzlog.out(TAG, "No image path:");
                        return;
                    }
                }

                File file = new File(takePicFileName);
                if (file.exists()) {
                    // car plate:
                    Intent intent = new Intent(this, PictureRecognizeActivity.class);
                    intent.putExtra("picture_type", picType);
                    intent.putExtra("path", takePicFileName);
                    startActivityForResult(intent, RECOGNIZE_PIC);
                }
                break;
            case GET_IMAGE_ON_DEVICE:
                String filename = getPictureFromDevice(data);

                Intent intent = new Intent(this, PictureRecognizeActivity.class);
                intent.putExtra("picture_type", picType);
                intent.putExtra("path", filename);
                startActivityForResult(intent, RECOGNIZE_PIC);

                break;
            case RECOGNIZE_PIC:
                result = null;
                try {
                    result = data.getStringExtra("data");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Zzlog.out(TAG, "RECOGNIZE_PIC result = " + result);

                break;
            default:
                Zzlog.out(TAG, "onActivityResult default: requestCode + "
                          + requestCode + ", resultCode = " + resultCode
                        + ", data = " + data);
            }
        }

    }

    // get picture from device and return filename.

    private String getPictureFromDevice(Intent data) {
        ContentResolver resolver = this.getContentResolver();
        String[] projection = { MediaStore.Images.Media.DATA};
        Uri uri = data.getData();
        CursorLoader cursorLoader = new CursorLoader(this, uri, projection, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(projection[0]));

        Zzlog.out(TAG, "path  = " + path);

        if (TextUtils.isEmpty(path)) {
            try {
                Bitmap bm = MediaStore.Images.Media.getBitmap(resolver, uri);
                takePicFileName = FileUtils.getStringFileName(FileUtils.MEDIA_TYPE_IMAGE);
                FileUtils.savePicture(takePicFileName, bm);
            } catch (FileNotFoundException e) {
                e.printStackTrace();Zzlog.eOut(e);;
            } catch (IOException e) {
                e.printStackTrace();Zzlog.eOut(e);;
            }
        }

        Zzlog.out(TAG, "takePicFilename = " + takePicFileName);

        return takePicFileName;
    }

    AuthInterface authInterface = new AuthInterface() {

        @Override
        public void onAuthSucceed() {
            Zzlog.out(TAG, "onAuthSucceed()");
        }

        @Override
        public void onAuthFailed() {
            Zzlog.out(TAG, "onAuthFailed()");
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_take_pic) {
            choosePicType(R.id.action_take_pic);
            return true;
        }
        if (id == R.id.action_choose_pic) {
            choosePicType(R.id.action_choose_pic);
        }
        if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    
    private void choosePicType(final int actionPic) {
        String[] items = new String[] { "??????", "?????????", "??????", "??????" };
        
        OnClickListener listener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Zzlog.out(TAG, "which = " + which);
                picType = which + 1;

                alertDialog.dismiss();
                alertDialog = null;

                if (R.id.action_take_pic == actionPic) {
                    takePic();

                } else if (R.id.action_choose_pic == actionPic) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent, GET_IMAGE_ON_DEVICE);
                }
            }
        };
        
        alertDialog = new AlertDialog.Builder(this).
                      setTitle(getString(R.string.choose_pic_type))
                      .setSingleChoiceItems(items, -1, listener).show();
    }

    private void takePic() {

        // Intent intent = new Intent(this, PictureRecognizeActivity.class);
        // startActivity(intent);

        Zzlog.out(TAG, "takePic()");

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);// create a intent
                                                             // to take picture
        takePicFileName = FileUtils.getStringFileName(FileUtils.MEDIA_TYPE_IMAGE);

        Zzlog.out(TAG, "takePic() takePicFilename = " + takePicFileName);

        Uri fileUri = Uri.fromFile(new File(takePicFileName));

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file

        SharePrefUtil.saveString(this, "pic_path", takePicFileName);

        startActivityForResult(intent, TAKE_PHOTO_WITH_DATA);
    }

}
