package com.ovaku.utils.imageUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.ovaku.app.AppData;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

@SuppressLint("SdCardPath")
public class ImageUtilsCameraGallery {

    private Context context;
    private Activity current_activity;
    private Boolean toChooseMultipleImage = false;

    private ImageAttachmentListener imageAttachment_callBack;

    private String selected_path = "";
    private Uri imageUri;

    private int from = 0;
    private boolean isFragment = false;

    private String pickType = "";

    public ImageUtilsCameraGallery(Activity act, boolean isFragment, ImageAttachmentListener imageAttachment_callBack, Boolean toChooseMultipleImage) {

        this.context = act;
        this.current_activity = act;
        this.imageAttachment_callBack = imageAttachment_callBack;/*(ImageAttachmentListener) act;*/
        this.toChooseMultipleImage = toChooseMultipleImage;
        if (isFragment) {
            this.isFragment = true;
        }
    }

    public ImageUtilsCameraGallery(Activity act) {
        this.current_activity = act;
    }


    public boolean isDeviceSupportCamera() {
        return this.context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_ANY);
    }


    /** Compress Image
     * @param filePath
     * @return */

    public Bitmap convertToBitmapWithOriginal(String filePath) {
        File f = new File(filePath);
        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;
        options.inSampleSize = 1;
        //  inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

        try {
            //  load the bitmap from its path
            scaledBitmap = BitmapFactory.decodeFile(f.getAbsolutePath(), options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();

        }
        ExifInterface exif;
        try {

            exif = new ExifInterface(f.getAbsolutePath());

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return scaledBitmap;
    }

    public Bitmap createBitmapWithContentResolver(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = current_activity.getContentResolver().openFileDescriptor(uri, "r");
        assert parcelFileDescriptor != null;
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap scaledBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        ExifInterface exif;
        InputStream inputStream = null;

        try {
            current_activity.getContentResolver().notifyChange(uri, null);
            inputStream = current_activity.getContentResolver().openInputStream(uri);
            assert inputStream != null;
            exif = new ExifInterface(inputStream);

            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            Log.d("TAG EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();

            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    return scaledBitmap;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    // matrix.setRotate(90);
                    matrix.setScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.setRotate(180);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.setRotate(90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setRotate(-90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    Log.d("TAG EXIF", "Exif 1: " + orientation);
                    matrix.setRotate(-90);
                    break;
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null)
                inputStream.close();
        }
        return scaledBitmap;
    }




    /**
     * ImageSize Calculation
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

    public void imagePicker(final int from) {
        this.from = from;
        pickType = "Image";
        final CharSequence[] items;

        if (isDeviceSupportCamera()) {
            items = new CharSequence[2];
            items[0] = AppData.IMAGE_PICK_TYPE.CAMERA;
            items[1] = AppData.IMAGE_PICK_TYPE.GALLERY;
        } else {
            items = new CharSequence[1];
            items[0] = AppData.IMAGE_PICK_TYPE.GALLERY;
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(current_activity);
        alertDialog.setTitle("Add Image");
        alertDialog.setItems(items, (dialog, item) -> {
            if (items[item].equals(AppData.IMAGE_PICK_TYPE.CAMERA)) {
                camera_call();
            } else if (items[item].equals(AppData.IMAGE_PICK_TYPE.GALLERY)) {
                galley_call();
            }
        });
        alertDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        alertDialog.show();
    }

    public void openVideoPicker(final int from) {
        pickType = "Video";
        Intent pickPhoto = new Intent();
        pickPhoto.setAction(Intent.ACTION_OPEN_DOCUMENT);
        pickPhoto.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "video/*");
        pickPhoto.addCategory(Intent.CATEGORY_OPENABLE);

        pickPhoto.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickPhoto.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        if (toChooseMultipleImage)
            pickPhoto.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        current_activity.startActivityForResult(pickPhoto, 1);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSSSSSS", Locale.getDefault()).format(new Date());
        String imageFileName = timeStamp + "_JPEG";
        File storageDir = current_activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        selected_path = image.getAbsolutePath();
        return image;
    }

    private void camera_call() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(current_activity.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(current_activity,
                        current_activity.getPackageName() + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                current_activity.startActivityForResult(takePictureIntent, 0);
            }
        }
    }

    /**
     * pick image from Gallery
     */

    private void galley_call() {

        Intent pickPhoto = new Intent();
        pickPhoto.setAction(Intent.ACTION_OPEN_DOCUMENT);
        pickPhoto.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        pickPhoto.addCategory(Intent.CATEGORY_OPENABLE);

        pickPhoto.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickPhoto.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        if (toChooseMultipleImage)
            pickPhoto.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        current_activity.startActivityForResult(pickPhoto, 1);
    }


    /**
     * Intent ActivityResult
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String file_name;
        Bitmap bitmap;
        try {
            if (resultCode == Activity.RESULT_OK) {
                if (pickType.equals("Image")) {
                    switch (requestCode) {
                        case 0:
                            Log.i("Camera Selected", "Photo");
                            file_name = selected_path.substring(selected_path.lastIndexOf("/") + 1);
                            bitmap = createBitmapWithContentResolver(imageUri);
                            galleryAddPic();
                            imageAttachment_callBack.image_attachment(from, file_name, bitmap, imageUri, 1);
                            break;
                        case 1:
                            Log.i("Gallery", "Photo");
                            if (!toChooseMultipleImage) {
                                String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSSSSSS", Locale.getDefault()).format(new Date());
                                Uri selectedImage = data.getData();
                                file_name = timeStamp + "_JPEG.jpg";
                                Log.d("TAG", file_name);
                                bitmap = createBitmapWithContentResolver(selectedImage);
                                imageAttachment_callBack.image_attachment(from, file_name, bitmap, selectedImage, 1);

                            } else {
                                if (data.getData() != null) {
                                    String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSSSSSS", Locale.getDefault()).format(new Date());
                                    Uri selectedImage = data.getData();
                                    file_name = timeStamp + "_JPEG.jpg";
                                    bitmap = createBitmapWithContentResolver(selectedImage);
                                    imageAttachment_callBack.image_attachment(from, file_name, bitmap, selectedImage, 1);

                                } else {
                                    if (data.getClipData() != null) {
                                        ClipData mClipData = data.getClipData();
                                        for (int i = 0; i < mClipData.getItemCount(); i++) {
                                            String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSSSSSS", Locale.getDefault()).format(new Date());
                                            ClipData.Item item = mClipData.getItemAt(i);
                                            Uri selectedImage = item.getUri();
                                            file_name = timeStamp + "_JPEG.jpg";
                                            bitmap = createBitmapWithContentResolver(selectedImage);
                                            imageAttachment_callBack.image_attachment(from, file_name, bitmap, selectedImage, mClipData.getItemCount());
                                        }
                                        Log.v("LOG_TAG", "Selected Images" + mClipData.getItemCount());
                                    }
                                }
                            }
                            break;
                    }
                } else if (pickType.equals("Video")) {
                    switch (requestCode) {
                        case 0:
                            Log.i("Camera Selected", "Photo");
                            /*file_name = selected_path.substring(selected_path.lastIndexOf("/") + 1);
                            bitmap = createBitmapWithContentResolver(imageUri);
                            galleryAddPic();
                            imageAttachment_callBack.image_attachment(from, file_name, bitmap, imageUri, 1);*/
                            Bitmap bMap = ThumbnailUtils.createVideoThumbnail(Objects.requireNonNull(imageUri.getPath()), MediaStore.Video.Thumbnails.MICRO_KIND);
                            imageAttachment_callBack.image_attachment(from, getFileName(imageUri), bMap, imageUri, 1);
                            break;
                        case 1:
                            Log.i("Gallery", "Photo");
                            if (!toChooseMultipleImage) {
                                Uri returnUri = data.getData();
                                Bitmap bMap0 = loadVideoThumbnail(returnUri);
                                imageAttachment_callBack.image_attachment(from, getFileName(returnUri), bMap0, returnUri, 1);

                            } else {
                                if (data.getData() != null) {
                                    Uri returnUri = data.getData();
                                    Bitmap bMap1 = loadVideoThumbnail(returnUri);
                                    imageAttachment_callBack.image_attachment(from, getFileName(returnUri), bMap1, returnUri, 1);

                                } else {
                                    if (data.getClipData() != null) {
                                        ClipData mClipData = data.getClipData();
                                        for (int i = 0; i < mClipData.getItemCount(); i++) {
                                            ClipData.Item item = mClipData.getItemAt(i);
                                            Uri returnUri = item.getUri();
                                            Bitmap bMap2 = loadVideoThumbnail(returnUri);
                                            imageAttachment_callBack.image_attachment(from, getFileName(returnUri), bMap2, returnUri, 1);
                                        }
                                        Log.v("LOG_TAG", "Selected Images" + mClipData.getItemCount());
                                    }
                                }
                            }
                            break;
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


// Image Attachment Callback

    public interface ImageAttachmentListener {
        public void image_attachment(int from, String filename, Bitmap file, Uri uri, int count);

    }


    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(selected_path);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        current_activity.sendBroadcast(mediaScanIntent);
    }

    private Bitmap loadVideoThumbnail(Uri videoPath) throws IOException {
        MediaMetadataRetriever mMMR = new MediaMetadataRetriever();
        mMMR.setDataSource(context, videoPath);
        final byte[] data = mMMR.getEmbeddedPicture();
        Bitmap bitmap = null;
        if (data != null) {
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        }
        if (bitmap == null)
            bitmap = mMMR.getFrameAtTime();
        mMMR.release();
        return bitmap;
    }

    private String getFileName(Uri returnUri) {
        Cursor returnCursor =
                context.getContentResolver().query(returnUri, null, null, null, null);

        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();

        return returnCursor.getString(nameIndex);
    }
}