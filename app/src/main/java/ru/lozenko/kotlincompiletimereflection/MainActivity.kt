package ru.lozenko.kotlincompiletimereflection

import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val settings by lazy { compileSettings }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        readSettingButton.setOnClickListener { setting_editText.text = SpannableStringBuilder(settings.setting5) }
        writeSettingButton.setOnClickListener { settings.setting5 = setting_editText.text.toString() }
    }
}
