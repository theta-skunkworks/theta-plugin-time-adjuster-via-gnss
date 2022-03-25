package skunkworks.gnsstimeadjuster;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.widget.ImageView;

public class DateTimeNotAutoDaialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        ImageView imageView = new ImageView(getContext());
        imageView.setImageResource( R.drawable.daialog_datetimeauto );

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("notice")
                .setView(  imageView )
                .setMessage("Exit the plug-in.\n" +
                        "Please use after turning on \"Date / time setting\"-> \"Auto\" in the menu UI.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // ボタンを押した時の処理
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    }
                });
        return builder.create();
    }

}
