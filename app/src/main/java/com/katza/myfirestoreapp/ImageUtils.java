package com.katza.myfirestoreapp;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImageUtils {

    private static final int MAX_IMAGE_BYTES = 650000;

    private static final int MAX_DIMENSION = 1280;

    public static String convertBitmapToString(Bitmap bitmap) {
        byte[] bytes = compressImage(bitmap);
        if (bytes == null) return null;
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    public static Bitmap convertBase64StringToBitmap(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static byte[] compressImage(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return null;

        Bitmap current = bitmap;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int longest = Math.max(width, height);

        if (longest > MAX_DIMENSION) {
            float scale = (float) MAX_DIMENSION / longest;
            current = Bitmap.createScaledBitmap(
                    bitmap,
                    Math.max(1, Math.round(width * scale)),
                    Math.max(1, Math.round(height * scale)),
                    true);
        }

        if (current.hasAlpha()) {
            Bitmap opaque = Bitmap.createBitmap(
                    current.getWidth(),
                    current.getHeight(),
                    Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(opaque);
            canvas.drawColor(Color.WHITE);
            canvas.drawBitmap(current, 0, 0, null);
            if (current != bitmap) current.recycle();
            current = opaque;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        while (true) {
            for (int quality = 90; quality >= 30; quality -= 10) {
                stream.reset();
                boolean ok = current.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                if (!ok) {
                    if (current != bitmap) current.recycle();
                    return null;
                }
                if (stream.size() <= MAX_IMAGE_BYTES) {
                    byte[] result = stream.toByteArray();
                    if (current != bitmap) current.recycle();
                    return result;
                }
            }

            int newWidth = Math.max(1, (int) (current.getWidth() * 0.8));
            int newHeight = Math.max(1, (int) (current.getHeight() * 0.8));

            if (newWidth < 100 || newHeight < 100
                    || (newWidth == current.getWidth()
                    && newHeight == current.getHeight())) {
                if (current != bitmap) current.recycle();
                return null;
            }

            Bitmap smaller = Bitmap.createScaledBitmap(current, newWidth, newHeight, true);
            if (current != bitmap) current.recycle();
            current = smaller;
        }
    }

    public static Uri createCameraUri(Context context) throws IOException {
        File directory = new File(context.getCacheDir(), "camera");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create the camera folder.");
        }
        File file = File.createTempFile("photo_", ".jpg", directory);
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }

    public static Uri createGalleryFile(Context context) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "BuildLog_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BuildLog");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("Could not create a gallery file.");
        }
        return uri;
    }

    public static void publishPhoto(Context context, Uri uri) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.IS_PENDING, 0);
        context.getContentResolver().update(uri, values, null, null);
    }

    public static void discardPhoto(Context context, Uri uri) {
        if (uri != null) {
            try {
                context.getContentResolver().delete(uri, null, null);
            } catch (RuntimeException ignored) {

            }
        }
    }

    public static Bitmap loadBitmap(Context context, Uri uri) throws IOException {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), uri), (decoder, info, source) -> {
            int width = info.getSize().getWidth();
            int height = info.getSize().getHeight();
            float scale = Math.min(1f, (float) MAX_DIMENSION / Math.max(width, height));
            decoder.setTargetSize(Math.max(1, Math.round(width * scale)), Math.max(1, Math.round(height * scale)));
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
    }

    public static Uri copyToGallery(Context context, Uri source) throws IOException {
        Bitmap bitmap = loadBitmap(context, source);
        byte[] bytes;
        try {
            bytes = compressImage(bitmap);
        } finally {
            bitmap.recycle();
        }
        if (bytes == null) {
            throw new IOException("The image could not be compressed.");
        }
        Uri target = createGalleryFile(context);
        try {
            try (OutputStream output = context.getContentResolver().openOutputStream(target)) {
                if (output == null) {
                    throw new IOException("Could not open the gallery file.");
                }
                output.write(bytes);
            }
            publishPhoto(context, target);
            return target;
        } catch (Exception error) {
            discardPhoto(context, target);
            throw new IOException("Could not save the image to the gallery.", error);
        }
    }
}
