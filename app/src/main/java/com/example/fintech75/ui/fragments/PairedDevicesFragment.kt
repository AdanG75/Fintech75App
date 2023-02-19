package com.example.fintech75.ui.fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.example.fintech75.R
import com.example.fintech75.databinding.FragmentPairedDevicesBinding
import com.example.fintech75.hardware.BtClass

class PairedDevicesFragment : Fragment(R.layout.fragment_paired_devices) {
    private val fragmentName = this::class.java.toString()

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val mapUUID: MutableMap<String, Array<ParcelUuid>?> = mutableMapOf()

    private lateinit var binding: FragmentPairedDevicesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothAdapter = BtClass.bluetoothManager?.adapter
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentPairedDevicesBinding.bind(view)

        binding.rlPairedLoading.visibility = View.GONE
        binding.cvPairedDevices.visibility = View.VISIBLE

        val pairedDevicesArrayAdapter: ArrayAdapter<String> =
            ArrayAdapter(view.context, R.layout.device_name)

        binding.lvPairedDevices.adapter = pairedDevicesArrayAdapter
        binding.lvPairedDevices.onItemClickListener = mDeviceClickListener


        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

        if (pairedDevices == null || pairedDevices.isEmpty()){
            val noDevices: String = getString(R.string.no_devices_paired)
            pairedDevicesArrayAdapter.add(noDevices)
        }

        pairedDevices?.forEach { device ->
            pairedDevicesArrayAdapter.add("${device.name}\n${device.address}")

            val uuidsDevice: Array<ParcelUuid>? = device.uuids

            if (uuidsDevice != null) {
                if (uuidsDevice.isNotEmpty()) {
                    mapUUID[device.name.toString()] = uuidsDevice
                }
            }

        }

    }

    private val mDeviceClickListener =
        AdapterView.OnItemClickListener { _, v, _, _ -> // Cancel discovery because it's costly and we're about to connect
            binding.tvPairedMsg.text = getString(R.string.setting_bluetooth)
            binding.rlPairedLoading.visibility = View.VISIBLE
            binding.cvPairedDevices.visibility = View.GONE

            // Get the device MAC address, which is the last 17 chars in the View
            val info: String = (v as TextView).text.toString()
            val address: String = info.substring(info.length - 17)
            val name: String = info.substring(0, info.length - 18)

            val uuidsDevice: Array<ParcelUuid>? = mapUUID[name]

            uuidsDevice?.forEach { _ ->
                Log.d(fragmentName, "BtDevice: $name")
            }

            val bundle = bundleOf(
                "btAddressKey" to address,
                "btNameKey" to name,
                "btUUIDsKey" to uuidsDevice
            )
            findNavController().previousBackStackEntry?.savedStateHandle?.set("btDevice", bundle)


            activity?.onBackPressed() // To return to FirstFragment
        }
}