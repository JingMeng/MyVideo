package com.video.takephoto;

import android.os.Handler;
import android.os.Looper;

import java.io.File;

public abstract class TakePhotoResult {
    private Looper mLooper = Looper.getMainLooper();

    public TakePhotoResult() {
    }

    public TakePhotoResult(Looper mLooper) {
        this.mLooper = mLooper;
    }

    public abstract void onFailure(String message);

    /**
     * 拍照成功将会调用此方法
     *
     * @param compressedFile 压缩文件
     * @param originalFile 原始文件
     */
    public void onResultFile(File compressedFile, File originalFile) {
    }

    /**
     * 拍照成功，有缩略图情况下，将会调用此方法
     *
     * @param compressedFile 压缩文件
     * @param originalFile 原始文件
     * @param thumbnailFile  缩略文件
     */
    public void onResultFile(File compressedFile, File originalFile, File thumbnailFile) {
    }

    protected final void onResult(final TakePhotoOptions options) {
        //主UI线程去处理
        new Handler(mLooper).post(new Runnable() {
            @Override
            public void run() {
                File compressedFile = options.getCompressedOptions().file;
                if (options.isCreateThumbnail()) {
                    onResultFile(compressedFile, options.getOriginalFile(), options.getThumbnailOptions().file);
                } else
                    onResultFile(compressedFile, options.getOriginalFile());
            }
        });
    }
}
