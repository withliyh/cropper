package com.github.withliyh.tinycopper;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.opengl.GLES10;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;

import com.github.withliyh.tinycopper.view.CutAvatarView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TinyCopperActivity extends AppCompatActivity {
    private static final boolean IN_MEMORY_CROP = Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1;
    private static final int SIZE_DEFAULT = 2048;
    private static final int SIZE_LIMIT = 4096;

    private int exifRotation;
    private RotateBitmap rotateBitmap;

    private Uri sourceUri;
    private Uri saveUri;

    private boolean isSaving;

    private int sampleSize;

    private CutAvatarView mCutAvatarView;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tinycropper_main);
        initViews();
        setupFromIntent();
        startCrop();
    }

    private void initViews() {
        mCutAvatarView = (CutAvatarView) findViewById(R.id.tiny_cropper_image_view);
        findViewById(R.id.tiny_cropper_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        findViewById(R.id.tiny_cropper_save).setOnClickListener(doCut());
    }

    private View.OnClickListener doCut() {
        return new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (bitmap != null && bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                bitmap = mCutAvatarView.clip(true);
                saveOutput(bitmap);
            }
        };
    }

    private void setupFromIntent() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            saveUri = extras.getParcelable(MediaStore.EXTRA_OUTPUT);
        }
        sourceUri = intent.getData();
        if(sourceUri != null) {
            exifRotation = CropUtil.getExifRotation( CropUtil.getFromMediaUri( getContentResolver(), sourceUri));

            InputStream is = null;
            try {
                sampleSize = calculateBitmapSampleSize( sourceUri);
                is = getContentResolver().openInputStream( sourceUri);
                BitmapFactory.Options option = new BitmapFactory.Options();
                option.inSampleSize = sampleSize;
                rotateBitmap = new RotateBitmap( BitmapFactory.decodeStream( is, null, option), exifRotation);
            } catch(IOException e) {
                Log.e("Error reading image: " + e.getMessage(), e.toString());
                setResultException( e);
            } catch(OutOfMemoryError e) {
                Log.e( "OOM reading image: " + e.getMessage(), e.toString());
                setResultException( e);
            } finally {
                CropUtil.closeSilently( is);
            }
        }
    }

    private int calculateBitmapSampleSize(Uri bitmapUri) throws IOException {
        InputStream is = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            is = getContentResolver().openInputStream( bitmapUri);
            BitmapFactory.decodeStream( is, null, options); // Just get image size
        } finally {
            CropUtil.closeSilently( is);
        }

        int maxSize = getMaxImageSize();
        int sampleSize = 1;
        while(options.outHeight / sampleSize > maxSize || options.outWidth / sampleSize > maxSize) {
            sampleSize = sampleSize << 1;
        }
        return sampleSize;
    }


    private int getMaxImageSize() {
        int textureLimit = getMaxTextureSize();
        if(textureLimit == 0) {
            return SIZE_DEFAULT;
        }
        else {
            return Math.min( textureLimit, SIZE_LIMIT);
        }
    }

    private int getMaxTextureSize() {
        // The OpenGL texture size is the maximum size that can be drawn in an ImageView
        int[] maxSize = new int[1];
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0);
        return maxSize[0];
    }

    private void setResultUri(Uri uri) {
        setResult(RESULT_OK, new Intent().putExtra(MediaStore.EXTRA_OUTPUT, uri));
    }

    private void setResultException(Throwable throwable) {
        setResult(Crop.RESULT_ERROR, new Intent().putExtra( Crop.Extra.ERROR, throwable));
    }

    private void startCrop() {
        if (isFinishing()) {
            return;
        }

        mCutAvatarView.setImageBitmap(rotateBitmap.getBitmap());
    }

    private void saveOutput(Bitmap croppedImage) {
        if(saveUri != null) {
            OutputStream outputStream = null;
            try {
                outputStream = getContentResolver().openOutputStream( saveUri);
                if(outputStream != null) {

                    if(exifRotation > 0) {
                        try {
                            Matrix matrix = new Matrix();
                            matrix.reset();
                            matrix.postRotate( exifRotation);
                            Bitmap bMapRotate =
                                    Bitmap.createBitmap( croppedImage, 0, 0, croppedImage.getWidth(), croppedImage.getHeight(), matrix,
                                            true);
                            bMapRotate.compress( Bitmap.CompressFormat.PNG, 70, outputStream);

                        } catch(Exception e) {
                            e.printStackTrace();
                            croppedImage.compress( Bitmap.CompressFormat.PNG, 70, outputStream);
                        } finally {
                            if(croppedImage != null && !croppedImage.isRecycled()) {
                                croppedImage.recycle();
                            }
                        }
                    }
                    else {
                        croppedImage.compress( Bitmap.CompressFormat.PNG, 70, outputStream);
                    }
                }

            } catch(IOException e) {
                setResultException( e);
                Log.e( "Cannot open file: " + saveUri, e.toString());
            } finally {
                CropUtil.closeSilently( outputStream);
            }

            if(!IN_MEMORY_CROP) {
                // In-memory crop negates the rotation
                CropUtil.copyExifRotation( CropUtil.getFromMediaUri( getContentResolver(), sourceUri),
                        CropUtil.getFromMediaUri( getContentResolver(), saveUri));
            }

            setResultUri( saveUri);
        }

        final Bitmap b = croppedImage;
        b.recycle();

        finish();
    }

}
