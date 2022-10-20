package com.example.fypmock;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class NavMethodSelectionDialog extends DialogFragment {
    private String selection;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String [] navMethod = {"Follow sequence",
                "Follow sequence + return to start",
                "Shortest path",
                "Shortest path + return to start"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select Navigation Method");
        builder.setSingleChoiceItems(navMethod, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selection = navMethod[which];
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (selection == null){
                    return;
                }

                MyDialogFragmentListener activity = (MyDialogFragmentListener) getActivity();
                activity.onReturnValue(selection);
            }
        });

        return builder.create();
    }
}
