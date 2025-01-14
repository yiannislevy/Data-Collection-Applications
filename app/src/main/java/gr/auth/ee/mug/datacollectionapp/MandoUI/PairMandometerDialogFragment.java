package gr.auth.ee.mug.datacollectionapp.MandoUI;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import gr.auth.ee.mug.datacollectionapp.MainActivity;
import gr.auth.ee.mug.datacollectionapp.R;

public class PairMandometerDialogFragment extends DialogFragment {
    MainActivity underlyingActivity;
    LinearLayoutManager layoutManager;
    BTDevicesAdapter devicesAdapter;
    ImageButton cancel;
    RecyclerView devices;

    /**
     * Class constructor, initializes parameters
     * @param underlyingActivity The activity from where the fragment was launched, used to change said activity's UI
     *                           (cannot be done with requireActivity())
     */
    public PairMandometerDialogFragment(MainActivity underlyingActivity) {
        this.underlyingActivity = underlyingActivity;
    }

    public MainActivity getUnderlyingActivity() {
        return this.underlyingActivity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_mandometer_pairing, null);

        cancel = v.findViewById(R.id.cancelPairingButtonPairing);
        devices = v.findViewById(R.id.BTDevicesRecView);

        cancel.setOnClickListener(view -> {
            underlyingActivity.pairMandometerButton.setEnabled(true);
            dismiss();
        });

        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        devices.setLayoutManager(layoutManager);

        devicesAdapter = new BTDevicesAdapter(this, requireActivity().getApplicationContext());
        devices.setAdapter(devicesAdapter);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(v);
        return builder.create();
    }
}
