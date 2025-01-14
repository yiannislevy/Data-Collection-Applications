package gr.auth.ee.mug.datacollectionapp.MandoUI;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;


import gr.auth.ee.mug.datacollectionapp.R;
import gr.auth.ee.mug.datacollectionapp.mandocapture.BluetoothController;
import gr.auth.ee.mug.datacollectionapp.mandocapture.MandoCaptureManager;


public class BTDevicesAdapter extends RecyclerView.Adapter<BTDevicesAdapter.ViewHolder> {
    PairMandometerDialogFragment origin;
    BluetoothController pairingController;
    MandoCaptureManager mandoManager;
    String[] btDevices;
    Context context;

    /**
     * Class constructor, initializes properties
     * @param origin The fragment where the adapter originates
     * @param context Context to be passed to mandometer manager
     */
    public BTDevicesAdapter(PairMandometerDialogFragment origin, Context context) {
        this.origin = origin;
        this.pairingController = new BluetoothController();
        this.btDevices = this.pairingController.getPairedDevicesNames();
        this.context = context;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        Button chooseBtn;

        public ViewHolder(View btDevicesView) {
            super(btDevicesView);
            name = btDevicesView.findViewById(R.id.BTDeviceName);
            chooseBtn = btDevicesView.findViewById(R.id.chooseBTDevicebtn);
        }
    }

    @NonNull
    @Override
    public BTDevicesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bt_device_layout, parent, false);
        return new BTDevicesAdapter.ViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(BTDevicesAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.name.setText(String.valueOf(btDevices[position]));
        holder.chooseBtn.setOnClickListener(view -> {
            Toast.makeText(this.context, "Connecting...", Toast.LENGTH_SHORT).show();
            mandoManager = new MandoCaptureManager(this.context, pairingController.getDevice(position));
            if(mandoManager.notAvailable() == 0) {
                DialogFragment newFragment = new SetTareDialogFragment(mandoManager);
                newFragment.setCancelable(false);
                newFragment.show(((FragmentActivity) view.getContext()).getSupportFragmentManager(), "TareSet");
            } else {
                Toast.makeText(this.context, "Device not available! Please turn it on and retry.", Toast.LENGTH_SHORT).show();
                origin.getUnderlyingActivity().pairMandometerButton.setEnabled(true);
            }
            origin.dismiss();
        });
    }

    @Override
    public int getItemCount() {
        return btDevices.length;
    }
}
