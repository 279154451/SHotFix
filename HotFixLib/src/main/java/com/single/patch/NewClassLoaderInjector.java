package com.single.patch;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;

public class NewClassLoaderInjector {

    public static ClassLoader inject(Application app, ClassLoader oldClassLoader) throws Throwable {
        ClassLoader newClassLoader
                = createNewClassLoader(app, oldClassLoader, oldClassLoader.getParent());
        doInject(app, newClassLoader);
        return newClassLoader;
    }

    private static ClassLoader createNewClassLoader(Context context, ClassLoader oldClassLoader,
                                                    ClassLoader dispatchClassLoader) throws Throwable {
        //得到pathList
        Field pathListField = PatchReflectUtil.findField(oldClassLoader, "pathList");
        Object oldPathList = pathListField.get(oldClassLoader);

        //dexElements
        Field dexElementsField = PatchReflectUtil.findField(oldPathList, "dexElements");
        Object[] oldDexElements = (Object[]) dexElementsField.get(oldPathList);

        //从Element上得到 dexFile
        Field dexFileField = PatchReflectUtil.findField(oldDexElements[0], "dexFile");

        // 获得原始的dexPath用于构造classloader
        StringBuilder dexPathBuilder = new StringBuilder();
        String packageName = context.getPackageName();
        boolean isFirstItem = true;
        for (Object oldDexElement : oldDexElements) {
            String dexPath = null;
            DexFile dexFile = (DexFile) dexFileField.get(oldDexElement);
            if (dexFile != null) {
                dexPath = dexFile.getName();
            }
            if (dexPath == null || dexPath.isEmpty()) {
                continue;
            }
            if (!dexPath.contains("/" + packageName)) {
                continue;
            }
            if (isFirstItem) {
                isFirstItem = false;
            } else {
                dexPathBuilder.append(File.pathSeparator);
            }
            dexPathBuilder.append(dexPath);
        }

        final String combinedDexPath = dexPathBuilder.toString();

        //  app的native库（so） 文件目录 用于构造classloader
        Field nativeLibraryDirectoriesField = PatchReflectUtil.findField(oldPathList, "nativeLibraryDirectories");
        List<File> oldNativeLibraryDirectories = (List<File>) nativeLibraryDirectoriesField.get(oldPathList);


        StringBuilder libraryPathBuilder = new StringBuilder();
        isFirstItem = true;
        for (File libDir : oldNativeLibraryDirectories) {
            if (libDir == null) {
                continue;
            }
            if (isFirstItem) {
                isFirstItem = false;
            } else {
                libraryPathBuilder.append(File.pathSeparator);
            }
            libraryPathBuilder.append(libDir.getAbsolutePath());
        }

        String combinedLibraryPath = libraryPathBuilder.toString();

        //父类加载器不能是 oldClassLoader
        ClassLoader result = new PathClassLoader(combinedDexPath, combinedLibraryPath, dispatchClassLoader);

//        ShareReflectUtil.findField(oldPathList, "definingContext").set(oldPathList, result);

        return result;
    }


    private static void doInject(Application app, ClassLoader classLoader) throws Throwable {
        Thread.currentThread().setContextClassLoader(classLoader);

        Context baseContext = (Context) PatchReflectUtil.findField(app, "mBase").get(app);
        Object basePackageInfo = PatchReflectUtil.findField(baseContext, "mPackageInfo").get(baseContext);
        PatchReflectUtil.findField(basePackageInfo, "mClassLoader").set(basePackageInfo, classLoader);

        if (Build.VERSION.SDK_INT < 27) {
            Resources res = app.getResources();
            try {
                PatchReflectUtil.findField(res, "mClassLoader").set(res, classLoader);

                final Object drawableInflater = PatchReflectUtil.findField(res, "mDrawableInflater").get(res);
                if (drawableInflater != null) {
                    PatchReflectUtil.findField(drawableInflater, "mClassLoader").set(drawableInflater, classLoader);
                }
            } catch (Throwable ignored) {
                // Ignored.
            }
        }
    }
}
