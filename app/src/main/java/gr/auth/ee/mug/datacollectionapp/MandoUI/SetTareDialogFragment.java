package gr.auth.ee.mug.datacollectionapp.MandoUI;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import gr.auth.ee.mug.datacollectionapp.R;
import gr.auth.ee.mug.datacollectionapp.mandocapture.MandoCaptureManager;

public class SetTareDialogFragment extends DialogFragment {
    MandoCaptureManager mandoManager;
    TextView guideTxtView;
    Button setTareBtn;

    /**
     * Class constructor, initializes parameters
     * @param mandoManager The mandometer manager created on a previous step
     */
    public SetTareDialogFragment(MandoCaptureManager mandoManager) {
        this.mandoManager = mandoManager;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_set_tare, null);

        guideTxtView = v.findViewById(R.id.setTareTxtView);
        setTareBtn = v.findViewById(R.id.setTareButton);

        guideTxtView.setText(R.string.Set_tare_guide);

        setTareBtn.setOnClickListener(view -> {
            this.mandoManager.setMandoTare();
            DialogFragment newFragment = new StartWeightMonitoringDialogFragment(this.mandoManager);
            newFragment.setCancelable(false);
            newFragment.show(((FragmentActivity) v.getContext()).getSupportFragmentManager(), "TareSet");
            dismiss();
        });


        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(v);
        return builder.create();
    }
}