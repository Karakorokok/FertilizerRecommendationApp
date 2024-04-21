package com.example.fertiguide

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog

open class MainActivity : AppCompatActivity() {

    private lateinit var imgBtnRice: ImageButton
    private lateinit var imgBtnCorn: ImageButton
    private lateinit var imgBtnSugarCane: ImageButton
    private lateinit var btnHectare: Button
    private lateinit var btnSquareMeter: Button
    private lateinit var btnNext: Button
    private lateinit var txtSelectCrop: TextView
    private var selectedCrop = ""
    private var unit = ""
    private lateinit var inputLandArea: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = getColor(R.color.white)
        } else {
            window.statusBarColor = resources.getColor(R.color.white)
        }

        imgBtnRice = findViewById<ImageButton>(R.id.imgBtnRice)
        imgBtnCorn = findViewById<ImageButton>(R.id.imgBtnCorn)
        imgBtnSugarCane = findViewById<ImageButton>(R.id.imgBtnSugarCane)
        txtSelectCrop = findViewById<TextView>(R.id.txtSelectCrop)

        imgBtnRice.setOnClickListener {
            selectedCrop = "Rice"
            txtSelectCrop.text = "Selected Crop: Rice"
            imgBtnRice.setBackgroundColor(resources.getColor(R.color.white))
            imgBtnCorn.setBackgroundColor(resources.getColor(R.color.Grey))
            imgBtnSugarCane.setBackgroundColor(resources.getColor(R.color.Grey))
        }

        imgBtnCorn.setOnClickListener {
            selectedCrop = "Corn"
            txtSelectCrop.text = "Selected Crop: Corn"
            imgBtnRice.setBackgroundColor(resources.getColor(R.color.Grey))
            imgBtnCorn.setBackgroundColor(resources.getColor(R.color.white))
            imgBtnSugarCane.setBackgroundColor(resources.getColor(R.color.Grey))
        }

        imgBtnSugarCane.setOnClickListener {
            selectedCrop = "Sugar Cane"
            txtSelectCrop.text = "Selected Crop: Sugar Cane"
            imgBtnRice.setBackgroundColor(resources.getColor(R.color.Grey))
            imgBtnCorn.setBackgroundColor(resources.getColor(R.color.Grey))
            imgBtnSugarCane.setBackgroundColor(resources.getColor(R.color.white))
        }

        btnHectare = findViewById<Button>(R.id.btnHectare)
        btnSquareMeter = findViewById<Button>(R.id.btnSquareMeter)

        btnHectare.setOnClickListener {
            btnHectare.setBackgroundColor(resources.getColor(R.color.white))
            btnSquareMeter.setBackgroundColor(resources.getColor(R.color.Grey))
            btnHectare.setTextColor(resources.getColor(R.color.DarkGrey))
            btnSquareMeter.setTextColor(resources.getColor(R.color.white))
            unit = "Hectare"
        }

        btnSquareMeter.setOnClickListener {
            btnHectare.setBackgroundColor(resources.getColor(R.color.Grey))
            btnSquareMeter.setBackgroundColor(resources.getColor(R.color.white))
            btnHectare.setTextColor(resources.getColor(R.color.white))
            btnSquareMeter.setTextColor(resources.getColor(R.color.DarkGrey))
            unit = "Square meter"
        }

        btnNext = findViewById<Button>(R.id.btnNext)
        btnNext.setOnClickListener {
            inputLandArea = findViewById(R.id.inputLandArea)
            val landAreaText = inputLandArea.text.toString().trim()

            if (selectedCrop.isEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Please select a crop.")
                    .setPositiveButton("OK", null)
                    .show()
            }
            else if (landAreaText.isEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Please enter land area value.")
                    .setPositiveButton("OK", null)
                    .show()
            }
            else if (unit.isEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Please select a unit (hectare or square meter).")
                    .setPositiveButton("OK", null)
                    .show()
            }
            else {
                try {
                    val landArea = landAreaText.toDouble()

                    val intent = Intent(this, BTActivity::class.java)
                    intent.putExtra("selectedCrop", selectedCrop)
                    intent.putExtra("landArea", landArea)
                    intent.putExtra("unit", unit)
                    startActivity(intent)
                } catch (e: NumberFormatException) {
                    AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("Please enter a valid number for land area.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

}