package com.emulation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.constraint_layout)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                systemBars.bottom
            )
            insets
        }

        val isEmulator = EmulatorDetector.isEmulator(this)

        val imgCircle = findViewById<ImageView>(R.id.imgCircle)
        val imgPhone = findViewById<ImageView>(R.id.imgPhone)
        val txtTitle = findViewById<TextView>(R.id.txtTitle)
        val txtMessage = findViewById<TextView>(R.id.txtMessage)
        val btnSupport = findViewById<Button>(R.id.btnSupport)
        val txtDeviceInfo = findViewById<TextView>(R.id.txtDeviceInfo)

        if (isEmulator) {
            imgCircle.setImageResource(R.drawable.ic_circle_red)
            imgPhone.setImageResource(R.drawable.ic_emulation_no)
            txtTitle.text = getString(R.string.unsafe_mode_title)
            txtMessage.text = getString(R.string.unsafe_mode_message)
        } else {
            imgCircle.setImageResource(R.drawable.ic_circle_green)
            imgPhone.setImageResource(R.drawable.ic_emulation_yes)
            txtTitle.text = getString(R.string.safe_mode_title)
            txtMessage.text = getString(R.string.safe_mode_message)
        }

        btnSupport.setOnClickListener {
            val urlIntent = Intent(
                Intent.ACTION_VIEW,
                "https://mobile.hackingforce.com.br/vulnerabilities/emulation".toUri()
            )
            startActivity(urlIntent)
        }

        val deviceInfo = getString(
            R.string.device_info_template,
            if (isEmulator) "Sim" else "Não",
            Build.BRAND,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.VERSION.RELEASE,
            Build.TAGS.ifEmpty { "unknown" }
        )

        txtDeviceInfo.text = deviceInfo
    }
}