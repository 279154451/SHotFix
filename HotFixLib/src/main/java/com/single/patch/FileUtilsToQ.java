package com.single.patch;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 创建时间：2020/3/12
 * 创建人：singleCode
 * 功能描述：
 **/
public class FileUtilsToQ {
    private static String TAG = "FileUtilsToQ";

    public   static byte[] readFileToByteArray(String path) {
        File file = new File(path);
        if (!file.exists()) {
            Log.e(TAG, "File doesn't exist!");
            return null;
        }
        FileInputStream in = null;
        try {

            in = new FileInputStream(file);
            long inSize = in.getChannel().size();//判断FileInputStream中是否有内容
            if (inSize == 0) {
                Log.d(TAG, "The FileInputStream has no content!");
                return null;
            }

            byte[] buffer = new byte[in.available()];//in.available() 表示要读取的文件中的数据长度
            in.read(buffer);  //将文件中的数据读到buffer中
            return buffer;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                return null;
            }
            //或IoUtils.closeQuietly(in);
        }
    }
    /**
     *
     * @param context
     * @param fileName  文件名，不是绝对路径。 如：1244342342.jpg
     * @param uri  查询出来的文件Uri
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static String getCachedFilePath(Context context, String fileName, Uri uri){
        ParcelFileDescriptor descriptor = null;
        try {
            descriptor = context.getContentResolver().openFile(uri, "r", null);
            FileInputStream inputStream = new FileInputStream(descriptor.getFileDescriptor());
            byte[] byteArray = readBinaryStream(inputStream, (int) descriptor.getStatSize());
            if (!TextUtils.isEmpty(fileName)) {
                File cachedFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),fileName);
                boolean fileSaved = writeFile(cachedFile, byteArray);
                if (fileSaved) {
                    return cachedFile.getAbsolutePath();
                } else {
                    return null;
                }
            }
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            if (descriptor != null) {
                try {
                    descriptor.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    public static boolean isContentUriExists(Context context, Uri uri){
        if (null == context) {
            return false;
        }
        ContentResolver cr = context.getContentResolver();
        try {
            AssetFileDescriptor afd = cr.openAssetFileDescriptor(uri, "r");
            if (null == afd) {
                return false;
            } else {
                try {
                    afd.close();
                } catch (IOException e) {
                }
            }
        } catch (FileNotFoundException e) {
            return false;
        }

        return true;
    }

    public static byte[] readBinaryStream(FileInputStream inputStream, int byteCount) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if(byteCount<=0){
            byteCount = 4096;
        }
        byte[] buffer = new byte[byteCount];
        try {
            int readCount = 0;
            while ((readCount = inputStream.read(buffer))>0){
                output.write(buffer,0,readCount);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return output.toByteArray();
        }
    }

    public static boolean writeFile(File cachedFile, byte[] byteArray) {
        BufferedOutputStream outputStream = null;
        boolean save = false;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(cachedFile));
            outputStream.write(byteArray);
            outputStream.flush();
            save = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return save;
        }

    }

    public  static boolean isAndroidQ(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            return true;
        }else {
            return false;
        }
    }
    public static String checkAndroidQFile(Context context, String path,String fileName){
        if(isAndroidQ()){
            File file = getCachedFile(context,fileName,getUriForFile(context,new File(path)));
            if(file != null){
                return file.getAbsolutePath();
            }else {
                return path;
            }
        }else {
            return path;
        }
    }
    public static Uri getUriForFile(Context context, File file) {
        if (file == null) {
            throw new NullPointerException();
        }
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            uri = FileProvider.getUriForFile(context, context.getPackageName(), file);
            String authority = context.getPackageName()+".fileprovider";
            Log.d(TAG, "getUriForFile: authority="+authority);
            uri =  FileProvider.getUriForFile(context, authority,file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }
    public static File getCachedFile(Context context, String fileName, Uri uri){
        Log.d(TAG, "getCachedFile: "+fileName+" uri="+uri);
        ParcelFileDescriptor descriptor = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                descriptor = context.getContentResolver().openFile(uri, "r", null);
            }else {
                return null;
            }
            FileInputStream inputStream = new FileInputStream(descriptor.getFileDescriptor());
            byte[] byteArray = readBinaryStream(inputStream, (int) descriptor.getStatSize());
            if (!TextUtils.isEmpty(fileName)) {
                File cachedFile = new File(context.getExternalCacheDir(),fileName);
                Log.d(TAG, "getCachedFile: cachedFile="+cachedFile.getAbsolutePath());
                boolean fileSaved = writeFile(cachedFile, byteArray);
                Log.d(TAG, "fileSaved: ="+fileSaved);
                if (fileSaved) {
                    return cachedFile;
                } else {
                    return null;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (descriptor != null) {
                try {
                    descriptor.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
