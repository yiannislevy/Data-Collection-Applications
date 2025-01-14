package gr.auth.ee.mug.datacollectionapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;

public class SignUpDialogFragment extends DialogFragment {
    Button signUpButton, deleteUsernameButton;

    ImageButton exitButton;

    TextInputEditText textField;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_sign_up, null);

        signUpButton = v.findViewById(R.id.signUpBtn);
        exitButton = v.findViewById(R.id.exitSignUpBtn);
        textField = v.findViewById(R.id.usernameInput);
        deleteUsernameButton = v.findViewById(R.id.deleteUsernameBtn);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireActivity().getApplicationContext());
        if(preferences.contains("JWT") && preferences.contains("Username"))
        {
            signUpButton.setEnabled(false);
            textField.setEnabled(false);
            textField.setText(preferences.getString("Username", "-"));
        }
        else {
            deleteUsernameButton.setEnabled(false);
        }

        signUpButton.setOnClickListener(v1 -> {
            ConnectivityManager connectivityManager = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
            if(activeNetworkInfo != null && activeNetworkInfo.isConnected() && textField.getText() != null) {
                Server.requestSignUp("eating-dataset", textField.getText().toString(), requireActivity().getApplicationContext());
                dismiss();
            } else
                Toast.makeText(requireActivity().getApplicationContext(), "Please establish network connection and try again!", Toast.LENGTH_SHORT).show();
        });

        deleteUsernameButton.setOnClickListener(v1 -> {
            SharedPreferences preferences1 = PreferenceManager.getDefaultSharedPreferences(requireActivity().getApplicationContext());
            preferences1.edit().clear().commit();
            Toast.makeText(requireActivity().getApplicationContext(), "Username deleted!", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        exitButton.setOnClickListener(v1 -> dismiss());

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(v);
        return builder.create();
    }
}
