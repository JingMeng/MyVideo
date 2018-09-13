package com.video.takephoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class TakePhotoUtil {
    private TakePhotoUtil() {
    }

    public static File getCacheDir(String dirName, Context context) {
        File result;
        if (existsSdcard()) {
            result = new File(dirName);
        } else {
            result = new File(context.getCacheDir(), dirName);
        }
        if (result.exists() || result.mkdirs()) {
            return result;
        } else {
            return null;
        }
    }

    /**
     * 检查磁盘空间是否大于10mb
     *
     * @return true 大于
     */
    public static boolean isDiskAvailable() {
        long size = getDiskAvailableSize();
        return size > 10 * 1024 * 1024; // > 10bm
    }

    /**
     * 获取磁盘可用空间
     *
     * @return byte 单位 kb
     */
    public static long getDiskAvailableSize() {
        if (!existsSdcard()) return 0;
        File path = Environment.getExternalStorageDirectory(); // 取得sdcard文件路径
        StatFs stat = new StatFs(path.getAbsolutePath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    public static Boolean existsSdcard() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 创建缩略图
     *
     * @param originalPath   原图路径
     * @param compressedPath 输出图路径
     * @param width          输出图片宽
     * @param height         输出图片高
     */
    public static boolean createImageThumbnail(String originalPath, String compressedPath, int width, int height) throws Exception {
        Bitmap bitmapFromFile = getBitmapFromFile(originalPath, width, height);
        return saveBitmapToLocal(bitmapFromFile, compressedPath);
    }

    public static Bitmap getBitmapFromFile(String path, int width, int height) throws Exception {
        BitmapFactory.Options opts = null;
        if (path != null) {
            if (width > 0 && height > 0) {
                opts = new BitmapFactory.Options();
                ////设置为true, 加载器不会返回图片, 而是设置Options对象中以out开头的字段.即仅仅解码边缘区域
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, opts);
                final int minSideLength = Math.min(width, height);
                //设置位图缩放比例
                opts.inSampleSize = computeSampleSize(opts, minSideLength, width * height);
                // 指定加载可以加载出图片.
                opts.inJustDecodeBounds = false;
                //为位图设置100K的缓存
                opts.inTempStorage = new byte[100 * 1024];
                //设置位图颜色显示优化方式
                opts.inPreferredConfig = Bitmap.Config.RGB_565;
                //设置图片可以被回收，创建Bitmap用于存储Pixel的内存空间在系统内存不足时可以被回收
                opts.inPurgeable = true;
                //设置解码位图的尺寸信息
                opts.inInputShareable = true;
            }
            //解码位图
            Bitmap thbBitmap = BitmapFactory.decodeFile(path, opts);
            //获取图片的旋转角度，有些系统把拍照的图片旋转了，有的没有旋转
            int degree = readPictureDegree(path);
            Bitmap rotateBitmap = rotateImageView(degree, thbBitmap);
            return rotateBitmap;
        } else return null;
    }

    private static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }
        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math
                .sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(Math
                .floor(w / minSideLength), Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            return lowerBound;
        }

        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    /**
     * 读取图片属性：旋转的角度
     *
     * @param path 图片绝对路径
     * @return degree旋转的角度
     */
    private static int readPictureDegree(String path) throws Exception {
        int degree = 0;
        ExifInterface exifInterface = new ExifInterface(path);
        int orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                degree = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degree = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degree = 270;
                break;
        }
        return degree;
    }

    /**
     * 旋转图片
     *
     * @param angle
     * @param bitmap
     * @return Bitmap
     */
    private static Bitmap rotateImageView(int angle, Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizedBitmap;
    }

    /**
     * 根据bitmap保存图片到本地
     *
     * @param bitmap
     * @return
     */
    public static boolean saveBitmapToLocal(Bitmap bitmap, String filePath) {
        if (null == bitmap) {
            return false;
        }
        FileOutputStream fileOutput = null;
        try {
            File fileDir = new File(filePath.substring(0, filePath.lastIndexOf(File.separator)));
            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }
            File imgFile = new File(filePath);
            if (!imgFile.exists())
                imgFile.createNewFile();
            fileOutput = new FileOutputStream(imgFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, fileOutput);
            fileOutput.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (null != fileOutput) {
                try {
                    fileOutput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    public static Bitmap decodeUriAsBitmap(Context context, Uri uri) {
        if (context == null || uri == null) return null;

        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    /**
     * 按宽/高缩放图片到指定大小并进行裁剪得到中间部分图片 <br>
     * @param bitmap 源bitmap
     * @param w 缩放后指定的宽度
     * @param h 缩放后指定的高度
     * @return 缩放后的中间部分图片 Bitmap
     */
    public static Bitmap zoomBitmap(Bitmap bitmap, int w, int h) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.i("TAG", "zoomBitmap---" + "width:" + width + "---" + "height:" + height);
        float scaleWidht, scaleHeight, x, y;
        Bitmap newbmp;
        Matrix matrix = new Matrix();
        if (width > height) {
            scaleWidht = ((float) h / height);
            scaleHeight = ((float) h / height);
            x = (width - w * height / h) / 2;// 获取bitmap源文件中x做表需要偏移的像数大小
            y = 0;
        } else if (width < height) {
            scaleWidht = ((float) w / width);
            scaleHeight = ((float) w / width);
            x = 0;
            y = (height - h * width / w) / 2;// 获取bitmap源文件中y做表需要偏移的像数大小
        } else {
            scaleWidht = ((float) w / width);
            scaleHeight = ((float) w / width);
            x = 0;
            y = 0;
        }
        matrix.postScale(scaleWidht, scaleHeight);
        try {
            newbmp = Bitmap.createBitmap(bitmap, (int) x, (int) y, (int) (width - x), (int) (height - y), matrix, true);// createBitmap()方法中定义的参数x+width要小于或等于bitmap.getWidth()，y+height要小于或等于bitmap.getHeight()
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return newbmp;
    }
}
