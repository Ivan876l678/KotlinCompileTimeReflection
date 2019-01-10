package ru.lozenko.kotlincompiletimereflection

import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        readSettingButton.setOnClickListener { setting_editText.text = SpannableStringBuilder(compileSettings.setting5) }
        writeSettingButton.setOnClickListener { compileSettings.setting5 = setting_editText.text.toString() }
    }
}
