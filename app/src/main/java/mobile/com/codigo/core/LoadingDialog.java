package mobile.com.codigo.core;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;

import mobile.com.codigo.R;

/**
 * Created by angga on 17/01/17.
 */

public class LoadingDialog {

    private static Dialog loadingDialog;

    public static Dialog build(Context context) {
        if (loadingDialog != null) return loadingDialog;

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.loading_layout);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(false);
        loadingDialog = dialog;

        return dialog;
    }
}
