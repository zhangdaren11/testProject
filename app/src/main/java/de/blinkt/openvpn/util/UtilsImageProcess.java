package de.blinkt.openvpn.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class UtilsImageProcess {

	/**
	 * 将得到的一个Bitmap保存到SD卡上，得到一个URI地址
	 */
	public static Uri saveBitmap(Bitmap bm) {
		//在SD卡上创建目录
		File tmpDir = new File(Environment.getExternalStorageDirectory() + "/aixoaqi");
		if (!tmpDir.exists()) {
			tmpDir.mkdirs();
		}

		File img = new File(tmpDir.getAbsolutePath() + "test.png");
		if(!img.exists()){
			try {

				img.createNewFile();
			}catch (Exception e){

			}
		}
		try {
			FileOutputStream fos = new FileOutputStream(img);
			bm.compress(Bitmap.CompressFormat.PNG, 85, fos);
			fos.flush();
			fos.close();
			return Uri.fromFile(img);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 将得到的一个Bitmap保存到SD卡上，得到一个绝对路径
	 */
	public static String getPath(Bitmap bm) {
		//在SD卡上创建目录

		File tmpDir = new File(Environment.getExternalStorageDirectory(), "/aixiaoqi");
		if (!tmpDir.exists()) {
			tmpDir.mkdirs();
		}
		File img1 = new File(tmpDir.getAbsolutePath() + "/test.png");
		if (img1.exists()) {
			img1.delete();
		}
		File img = new File(tmpDir.getAbsolutePath() + "/test.png");
		if(!img.exists()){
			try {

				img.createNewFile();
			}catch (Exception e){

			}
		}
		try {
			FileOutputStream fos = new FileOutputStream(img);
			bm.compress(Bitmap.CompressFormat.PNG, 100, fos);
			fos.flush();
			fos.close();
			return img.getCanonicalPath();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 将图库中选取的图片的URI转化为URI
	 */
	public static Uri convertUri(Uri uri, Context context) {
		InputStream is;
		try {
			is = context.getContentResolver().openInputStream(uri);
			Bitmap bitmap = BitmapFactory.decodeStream(is);
			if(is!=null)
				is.close();
			return saveBitmap(bitmap);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}



	/**
	 * 计算图片的缩放值
	 */
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		return inSampleSize;
	}

	/**
	 * 根据路径获得图片并压缩，返回bitmap用于显示
	 */
	public static Bitmap getSmallBitmap(String filePath) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, 480, 800);
		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		// 设置为true，画质更好一点，加载时间略长
		options.inPreferQualityOverSpeed = true;

		return BitmapFactory.decodeFile(filePath, options);
	}
}