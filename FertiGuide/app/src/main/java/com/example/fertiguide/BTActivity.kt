package com.example.fertiguide

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import android.Manifest
import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BTActivity : MainActivity() {

    private lateinit var txtCropAndAreaFromMain: TextView
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var deviceList: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val REQUEST_ENABLE_BLUETOOTH = 1

    private lateinit var btnConnectBluetoothModule: Button
    private lateinit var btnComputeFertilizer: Button
    private lateinit var btnReScan: Button

    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private lateinit var readThread: Thread
    private lateinit var txtReceivedData: TextView

    private var pHValue: Double? = null
    private var nitrogenValue: Double? = null
    private var phosphorusValue: Double? = null
    private var potassiumValue: Double? = null

    private var receivedDataBuffer = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_btactivity)

        val selectedCrop = intent.getStringExtra("selectedCrop")
        val landArea = intent.getDoubleExtra("landArea", 0.0)
        val unit = intent.getStringExtra("unit")

        txtCropAndAreaFromMain = findViewById<TextView>(R.id.txtCropAndAreaFromMain)
        txtCropAndAreaFromMain.text = "Crop: $selectedCrop\nLand Area: $landArea $unit"

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        deviceList = findViewById(R.id.listOfPairedDevices)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        deviceList.adapter = adapter

        txtReceivedData = findViewById(R.id.txtReceivedData)

        btnConnectBluetoothModule = findViewById<Button>(R.id.btnPairedDevices)
        btnConnectBluetoothModule.setOnClickListener {
            checkBluetoothStatus()
        }

        deviceList.setOnItemClickListener { parent, view, position, id ->
            val deviceName = adapter.getItem(position)
            val devices = bluetoothAdapter.bondedDevices
            var selectedDevice: BluetoothDevice? = null
            for (device in devices) {
                if (device.name == deviceName) {
                    selectedDevice = device
                    break
                }
            }
            if (selectedDevice != null) {
                connectToDevice(selectedDevice)
            } else {
                Toast.makeText(this, "Device not found", Toast.LENGTH_SHORT).show()
            }
        }

        btnComputeFertilizer = findViewById<Button>(R.id.btnComputeFertilizer)
        btnComputeFertilizer.setOnClickListener() {

            filteredResults()

            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle("Calculated Fertilizer Amount")

            val pH = pHValue
            val n = nitrogenValue
            val p = phosphorusValue
            val k = potassiumValue

            val sqmN = (n?.div(100))?.times((landArea / 10000))
            val sqmP = (p?.div(100))?.times((landArea / 10000))
            val sqmK = (k?.div(100))?.times((landArea / 10000))

            val sqmNBags = (n?.div(50))?.times((landArea / 10000))
            val sqmPBags = (p?.div(50))?.times((landArea / 10000))
            val sqmKBags = (k?.div(50))?.times((landArea / 10000))


            if (pH != null) {
                if(pH < 5.0 || pH > 7.0) {
                    val message = "pH: $pH\n\npH value is not suitable, adjust soil acidity to continue"
                    alertDialog.setMessage(message)
                }
                else {
                    if(unit == "Hectare") {
                        val message = "pH: $pH\nNitrogen: $n mg/L\nPhosphorus: $p mg/L\nPotassium: $k mg/L\n\n" +
                                "Crop: $selectedCrop\nLand Area: $landArea $unit\n\nRecommended Fertilizer:\n\n" +
                                "Nitrogen (N): $n kg, or  ${String.format("%.2f", n?.div(50))} bags\n" +
                                "Phosphorus (P): $p kg, or ${String.format("%.2f", p?.div(50))} bags\n" +
                                "Potassium (K): $k kg, or ${String.format("%.2f", k?.div(50))} bags"

                        alertDialog.setMessage(message)
                    }
                    else if(unit == "Square meter") {
                        val message = "pH: $pH\nNitrogen: $n mg/L\nPhosphorus: $p mg/L\nPotassium: $k mg/L\n\n" +
                                "Crop: $selectedCrop\nLand Area: $landArea $unit\n\nRecommended Fertilizer:\n\n" +
                                "Nitrogen (N): ${String.format("%.2f", sqmN)} kg, or  ${String.format("%.2f", sqmNBags)} bags\n" +
                                "Phosphorus (P): ${String.format("%.2f", sqmP)} kg, or ${String.format("%.2f", sqmPBags)} bags\n" +
                                "Potassium (K): ${String.format("%.2f", sqmK)} kg, or ${String.format("%.2f", sqmKBags)} bags"

                        alertDialog.setMessage(message)
                    }
                    else {
                        val message = "Error unit"
                        alertDialog.setMessage(message)
                    }
                }
            }

            alertDialog.setNegativeButton("Download Data") { dialog, which ->
                if (pH != null) {
                    if (pH < 5.0 || pH > 7.0) {
                        Toast.makeText(
                            this,
                            "Download restricted for unsuitable pH value",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else {
                        val folderName = Environment.DIRECTORY_DOWNLOADS
                        val currentDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(
                            Date()
                        )
                        var version = 1
                        var fileName = "FertiGuideLogs_$currentDate.txt"

                        // Check if the file with the same name already exists, if yes, increment the version number
                        val directory = Environment.getExternalStoragePublicDirectory(folderName)
                        while (File(directory, fileName).exists()) {
                            version++
                            fileName = "FertiGuideLogs_$currentDate($version).txt"
                        }

                        val fileContent =
                            (dialog as AlertDialog).findViewById<TextView>(android.R.id.message)?.text.toString()
                        val fileOutputStream: FileOutputStream

                        try {
                            val file = File(directory, fileName)
                            if (file.exists()) {
                                file.delete()
                            }
                            val filePath = file.absolutePath
                            fileOutputStream = FileOutputStream(file)
                            fileOutputStream.write(fileContent.toByteArray())
                            fileOutputStream.close()

                            Toast.makeText(
                                this,
                                "File saved successfully to Downloads Folder",
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Log.e("BTActivity", "Failed to save file", e)
                            Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            alertDialog.setPositiveButton("Close") { dialog, which ->
                dialog.dismiss()
            }
            alertDialog.show()
        }

        btnReScan = findViewById<Button>(R.id.btnReScan)
        btnReScan.setOnClickListener() {
            sendData("a")
            Toast.makeText(this, "ReScan Signal Sent", Toast.LENGTH_SHORT).show()

        }
    }

    private fun checkBluetoothStatus() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 2)
        }

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
        } else {
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
            } else {
                getPaired()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth turned on", Toast.LENGTH_SHORT).show()
                getPaired()
            } else {
                Toast.makeText(this, "Bluetooth must be enabled to proceed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getPaired() {
        if (bluetoothAdapter.isEnabled) {
            adapter.clear()
            val devices = bluetoothAdapter.bondedDevices
            for (device in devices) {
                val deviceName = device.name
                adapter.add(deviceName)
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
            bluetoothSocket?.connect()

            outputStream = bluetoothSocket?.outputStream

            Toast.makeText(this, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
            inputStream = bluetoothSocket?.inputStream
            startReadingThread()
        } catch (e: IOException) {
            Toast.makeText(this, "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            try {
                bluetoothSocket?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun startReadingThread() {
        readThread = Thread(Runnable {
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            while (true) {
                try {
                    val line = bufferedReader.readLine()
                    receivedDataBuffer.append("$line\n")
                    runOnUiThread {
                        txtReceivedData.text = receivedDataBuffer.toString()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        })

        readThread.start()
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    txtReceivedData.text = receivedDataBuffer.toString()
                    receivedDataBuffer.clear()
                }
            }
        }, 0, 10000)//10 seconds

    }

    private fun filteredResults() {
        val receivedData = receivedDataBuffer.toString()
        val parts = receivedData.split("\\s+".toRegex())

        for (i in parts.indices) {
            if (parts[i] == "pH:") {
                pHValue = parts[i + 1].toDoubleOrNull()
            }
            if (parts[i] == "Nitrogen:") {
                nitrogenValue = parts[i + 1].toDoubleOrNull()
            }
            if (parts[i] == "Phosphorus:") {
                phosphorusValue = parts[i + 1].toDoubleOrNull()
            }
            if (parts[i] == "Potassium:") {
                potassiumValue = parts[i + 1].toDoubleOrNull()
            }
        }
    }

    private var outputStream: OutputStream? = null
    private fun sendData(data: String) {
        try {
            outputStream?.write(data.toByteArray())
            outputStream?.flush()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to send data", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            inputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}