package com.example.elisl.mylab1;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SaveToInternalStorage extends AsyncTask<Bitmap, Integer, String> {

    private final Context applicationContext;
    private ProgressDialog progressDialog;

    public SaveToInternalStorage(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected String doInBackground(Bitmap... bitmaps) {

        Bitmap bitmapImage = bitmaps[0];
        String imageName = Long.toString(System.currentTimeMillis());
        //String imageName = "profile.jpg";

        ContextWrapper cw = new ContextWrapper(applicationContext);
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory, imageName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 85, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath() + "/" + imageName;
    }

    @Override
    protected void onPreExecute() {
        /*progressDialog = new ProgressDialog(applicationContext);
        progressDialog.setMessage("Sign in in progress");
        progressDialog.setTitle("Please wait");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();*/
        Toast.makeText(applicationContext, R.string.saving, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPostExecute(String s) {
        //Toast.makeText(applicationContext, R.string.saved, Toast.LENGTH_SHORT).show();
    }
}
