package gr.auth.ee.mug.datacollectionapp.MandoUI;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import gr.auth.ee.mug.datacollectionapp.CaptureService;
import gr.auth.ee.mug.datacollectionapp.R;
import gr.auth.ee.mug.datacollectionapp.mandocapture.MandoCaptureManager;

public class StartWeightMonitoringDialogFragment extends DialogFragment {
    MandoCaptureManager mandoManager;
    Button startMando;
    CaptureService captureService;
    Boolean bound;

    /**
     * Class constructor, initializes parameters
     * @param mandoManager The mandometer manager created on a previous step
     */
    public StartWeightMonitoringDialogFragment(MandoCaptureManager mandoManager) {
        this.mandoManager = mandoManager;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Intent intent = new Intent(requireActivity().getApplicationContext(), CaptureService.class);
        requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_start_weight_monitoring, null);

        startMando = v.findViewById(R.id.startWeightMonitoringBtn);

        startMando.setOnClickListener(view -> {
            if (bound)
                captureService.setMandoManager(mandoManager);
            requireActivity().startForegroundService(intent);
            dismiss();
        });


        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(v);
        return builder.create();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        requireActivity().unbindService(connection);
        bound = false;
    }

    /**
     * Used to bind the fragment to the service so the service's functions can be used
     */
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CaptureService.LocalBinder binder = (CaptureService.LocalBinder) service;
            captureService = binder.getService();
            bound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };
}
