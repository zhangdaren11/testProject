package de.blinkt.openvpn.util;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;


/**
 * @author kim
 */
public class ToastCompat implements IToast {

    private IToast mIToast;

    public ToastCompat(Context context) {
        this(context, null, -1);
    }

    private static Handler mmHandler;


    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    ToastCompat(Context context, String text, int duration) {

        mmHandler = mHandler;
        if (!OSJudgementUtil.isMIUI()) {
            mIToast = new DefinedToast(context).setText(text).setDuration(duration)
                    .setGravity(Gravity.BOTTOM, 0, CommonTools.dip2px(context, 64));
        } else {
            mIToast = new SystemToast(context).setText(text).setDuration(duration);

        }


    }

    public static IToast makeText(Context context, String text, int duration) {
        return new ToastCompat(context, text, duration);
    }



    @Override
    public IToast setGravity(int gravity, int xOffset, int yOffset) {
        return mIToast.setGravity(gravity, xOffset, yOffset);
    }

    @Override
    public IToast setDuration(long durationMillis) {
        return mIToast.setDuration(durationMillis);
    }

    /**
     * 不能和{@link #setText(String)}一起使用，要么{@link #setView(View)} 要么{@link #setView(View)}
     *
     * @param view
     */
    @Override
    public IToast setView(View view) {
        return mIToast.setView(view);
    }

    @Override
    public IToast setMargin(float horizontalMargin, float verticalMargin) {
        return mIToast.setMargin(horizontalMargin, verticalMargin);
    }

    /**
     *
     * @param text
     */
    @Override
    public IToast setText(String text) {
        return mIToast.setText(text);
    }

    @Override
    public void show() {
        mIToast.show();
    }

    @Override
    public void cancel() {
        mIToast.cancel();
    }
}
