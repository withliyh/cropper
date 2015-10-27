package com.github.withliyh.tinycopper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;

/**
 * Created by Administrator on 2015/10/27.
 */
public class CroperHelper {
    private static final int REQUEST_CODE_CAPTURE_CAMEIA = 1458;

    private Activity mContext;
    private String mCurrentPhotoPath;
    private File mCacheDir;

    private OnResult onResult;

    public interface OnResult {
        void onSuccess(Uri uri);
        void onFailure();
    }
    public CroperHelper(Activity context) {
        mContext = context;
        mCacheDir = context.getCacheDir();
    }

    public void setOnResult(OnResult onResult) {
        this.onResult = onResult;
    }

    public void takePicktrue() {
        getImageFromCamera();
    }

    public void pickImage() {
        Crop.pickImage(mContext);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Crop.REQUEST_PICK) {
                beginCrop(result.getData());
            } else if (requestCode == Crop.REQUEST_CROP) {
                handleCrop(resultCode, result);
            } else if (requestCode == REQUEST_CODE_CAPTURE_CAMEIA) {
                if (mCurrentPhotoPath != null) {
                    beginCrop(Uri.fromFile(new File(mCurrentPhotoPath)));
                }
            }
        }
    }

    private void getImageFromCamera() {
        // create Intent to take a picture and return control to the calling
        // application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        String fileName = "avater_" + String.valueOf(System.currentTimeMillis());
        File cropFile = new File(mCacheDir, fileName);
        Uri fileUri = Uri.fromFile(cropFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file
        // name
        mCurrentPhotoPath = fileUri.getPath();
        // start the image capture Intent
        mContext.startActivityForResult(intent, REQUEST_CODE_CAPTURE_CAMEIA);
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == Activity.RESULT_OK) {
            //mAvaterImageVier.setImageURI(Crop.getOutput(result));
            if (onResult != null) {
                onResult.onSuccess(Crop.getOutput(result));
            }
        } else if (resultCode == Crop.RESULT_ERROR) {
            if (onResult != null) {
                onResult.onFailure();
            }
        }
    }


    private void beginCrop(Uri source) {
        String fileName = "Temp_" + String.valueOf(System.currentTimeMillis());
        File cropFile = new File(mCacheDir, fileName);
        Uri outputUri = Uri.fromFile(cropFile);
        new Crop(source).output(outputUri).start(mContext);
    }
}
