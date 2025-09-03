package com.example.taboolacontroller

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.taboolacontroller.databinding.ActivityMainBinding
import com.example.tabooladisplayapp.presentation.service.ICellColorService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var cellColorService: ICellColorService? = null
    private var isServiceBound = false

    companion object {
        private const val TAG = "TaboolaController"
        private const val TARGET_PACKAGE = "com.example.tabooladisplayapp"
        private const val SERVICE_ACTION = "com.example.tabooladisplayapp.action.CHANGE_CELL_COLOR"
        private const val SECURITY_TOKEN = "TABOOLA_DEMO_CTRL_COLOR_V1"
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            cellColorService = ICellColorService.Stub.asInterface(service)
            isServiceBound = true
            updateConnectionStatus("Connected to Display App")
            binding.btnSend.isEnabled = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            cellColorService = null
            isServiceBound = false
            updateConnectionStatus("Disconnected from Display App")
            binding.btnSend.isEnabled = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        bindToService()
    }

    private fun setupUI() {
        // Set default values
        binding.etPosition.setText("0")
        binding.etColorHex.setText("#FF5722")
        binding.switchVisibility.isChecked = true

        // Disable send button initially
        binding.btnSend.isEnabled = false
        updateConnectionStatus("Connecting...")

        // Set up click listeners
        binding.btnSend.setOnClickListener {
            sendColorUpdate()
        }

        binding.btnConnect.setOnClickListener {
            if (isServiceBound) {
                unbindService()
            } else {
                bindToService()
            }
        }

        // Color preset buttons
        binding.btnColorRed.setOnClickListener { binding.etColorHex.setText("#FF5722") }
        binding.btnColorBlue.setOnClickListener { binding.etColorHex.setText("#2196F3") }
        binding.btnColorGreen.setOnClickListener { binding.etColorHex.setText("#4CAF50") }
        binding.btnColorYellow.setOnClickListener { binding.etColorHex.setText("#FFEB3B") }
    }

    private fun bindToService() {
        updateConnectionStatus("Connecting to Display App...")

        try {
            val intent = Intent(SERVICE_ACTION).apply {
                setPackage(TARGET_PACKAGE)
            }

            val success = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

            if (!success) {
                updateConnectionStatus("Failed to bind to service. Is Display App installed?")
                showError("Cannot connect to Display App. Please ensure it's installed and running.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error binding to service", e)
            updateConnectionStatus("Error connecting to Display App")
            showError("Error connecting to Display App: ${e.message}")
        }
    }

    private fun unbindService() {
        if (isServiceBound) {
            try {
                unbindService(serviceConnection)
                isServiceBound = false
                cellColorService = null
                updateConnectionStatus("Disconnected")
                binding.btnSend.isEnabled = false
            } catch (e: Exception) {
                Log.e(TAG, "Error unbinding service", e)
            }
        }
    }

    private fun sendColorUpdate() {
        if (!isServiceBound || cellColorService == null) {
            showError("Not connected to Display App")
            return
        }

        // Validate inputs
        val positionText = binding.etPosition.text.toString().trim()
        val colorHex = binding.etColorHex.text.toString().trim()
        val isVisible = binding.switchVisibility.isChecked

        if (TextUtils.isEmpty(positionText)) {
            showError("Please enter a position")
            return
        }

        val position = try {
            positionText.toInt()
        } catch (e: NumberFormatException) {
            showError("Invalid position. Please enter a number between 0-100")
            return
        }

        if (position < 0 || position > 100) {
            showError("Position must be between 0 and 100")
            return
        }

        if (!isValidHexColor(colorHex)) {
            showError("Invalid color format. Use #RRGGBB format (e.g., #FF5722)")
            return
        }

        // Send the update
        try {
            binding.btnSend.isEnabled = false
            updateStatus("Sending color update...")

            val success = cellColorService!!.updateCellBackgroundColor(
                position,
                colorHex,
                isVisible,
                SECURITY_TOKEN
            )

            if (success) {
                updateStatus("✓ Color updated successfully!")
                showSuccess("Cell $position updated with color $colorHex (visible: $isVisible)")
            } else {
                updateStatus("✗ Update failed")
                showError("Failed to update cell color. Check Display App logs.")
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Remote exception during service call", e)
            updateStatus("✗ Communication error")
            showError("Communication error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during service call", e)
            updateStatus("✗ Unexpected error")
            showError("Unexpected error: ${e.message}")
        } finally {
            binding.btnSend.isEnabled = true
        }
    }

    private fun isValidHexColor(colorHex: String): Boolean {
        if (colorHex.length != 7 || !colorHex.startsWith("#")) {
            return false
        }

        return try {
            Color.parseColor(colorHex)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun updateConnectionStatus(message: String) {
        binding.tvConnectionStatus.text = "Connection: $message"
        binding.btnConnect.text = if (isServiceBound) "Disconnect" else "Connect"

        // Update connection indicator color
        val color = when {
            isServiceBound -> ContextCompat.getColor(this, android.R.color.holo_green_dark)
            message.contains("Connecting") -> ContextCompat.getColor(this, android.R.color.holo_orange_dark)
            else -> ContextCompat.getColor(this, android.R.color.holo_red_dark)
        }
        binding.tvConnectionStatus.setTextColor(color)
    }

    private fun updateStatus(message: String) {
        binding.tvStatus.text = message
        Log.d(TAG, message)
    }

    private fun showError(message: String) {
        Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
        updateStatus("Error: $message")
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        updateStatus(message)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService()
    }
}