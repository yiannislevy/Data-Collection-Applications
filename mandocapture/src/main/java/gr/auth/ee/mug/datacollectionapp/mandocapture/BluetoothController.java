package gr.auth.ee.mug.datacollectionapp.mandocapture;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * This bluetoothController is responsible for yielding Bluetooth Devices to the rest of
 * the application
 *
 * @author vassilis
 *
 */
@SuppressLint("MissingPermission")
public class BluetoothController {

	private final BluetoothAdapter mBluetoothAdapter;
	private List<BluetoothDevice> pairedDevices;

	/**
	 * Class constructor. Initialises the adapter.
	 */
	public BluetoothController () {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		getPairedDevices();
		Log.v("lemug","BluetoothController: constructed");
	}

	/**
	 * Retrieves all paired devices and creates a list in 'pairedDevices'
	 */
	private void getPairedDevices() throws SecurityException {
		if (!mBluetoothAdapter.isEnabled()) {
			// TODO Divide what happens here. Possible decisions
			//   a) Return with fail
			//   b) Ask the user to turn on bluetooth. This would have to check
			//      the case where the user refuses to turn on bluetooth 
		}

		// The following assumes mBluetoothAdapter.isEnabled()

		Set<BluetoothDevice> set1 = mBluetoothAdapter.getBondedDevices();

		pairedDevices = new ArrayList<BluetoothDevice>();

		for (BluetoothDevice dev : set1) {
			pairedDevices.add(dev);
		}
		Log.v("lemug","BluetoothController: Updated paired devices list");
	}

	/**
	 * Updates the 'pairedDevices' list, then returns a list of the paired
	 * devices names
	 * @return the list of the paired devices names
	 */
	public String[] getPairedDevicesNames() throws SecurityException {
		getPairedDevices();
		String[] pdnames = new String[pairedDevices.size()];
		for (int i=0; i<pairedDevices.size(); i++) {
			pdnames[i] = pairedDevices.get(i).getName();
		}
		Log.v("lemug","BluetoothController: Returning paired devices names");
		return pdnames;
	}
	
	/**
	 * Returns a bluetooth device
	 * @param i The index of the device in 'pairedDevices'
	 * @return The bluetooth device
	 */
	public BluetoothDevice getDevice(int i) {
		Log.v("lemug","BluetoothController: Returning bluetooth device #" + String.valueOf(i));
		return pairedDevices.get(i);
	}
	
	/**
	 * Returns a bluetooth device based on UUID. If no paired device with such a UUID is found, null is returned
	 * @param uuid The UUID of the device
	 * @return The bluetooth device
	 */
	public BluetoothDevice getDevice(String uuid) throws SecurityException {
		BluetoothDevice dev;
		for (int i=0; i<pairedDevices.size(); i++) {
			dev = pairedDevices.get(i);
			if (dev.getUuids().toString().equals(uuid)) {
				return dev;
			}
		}
		return null;
	}
}
