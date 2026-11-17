package com.example.poppymenu

import android.app.Service
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class FloatingMenuService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: View
    private lateinit var menuPanel: View
    private lateinit var buttonParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams
    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences("menu_prefs", Context.MODE_PRIVATE)

        addMenuPanel()
        addFloatingButton()
        restoreState()
    }

    private fun overlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun addFloatingButton() {
        floatingButton = LayoutInflater.from(this)
            .inflate(R.layout.layout_floating_button, null)

        buttonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        windowManager.addView(floatingButton, buttonParams)

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        floatingButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = buttonParams.x
                    initialY = buttonParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved = true
                    buttonParams.x = initialX + dx
                    buttonParams.y = initialY + dy
                    windowManager.updateViewLayout(floatingButton, buttonParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) togglePanel()
                    true
                }
                else -> false
            }
        }
    }

    private fun addMenuPanel() {
        menuPanel = LayoutInflater.from(this)
            .inflate(R.layout.layout_menu_panel, null)

        panelParams = WindowManager.LayoutParams(
            300.dp,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        menuPanel.visibility = View.GONE
        windowManager.addView(menuPanel, panelParams)

        menuPanel.findViewById<TextView>(R.id.btnClose).setOnClickListener {
            menuPanel.visibility = View.GONE
        }

        menuPanel.findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveState()
            Toast.makeText(this, "تم حفظ الإعدادات", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePanel() {
        menuPanel.visibility =
            if (menuPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun restoreState() {
        val swFly = menuPanel.findViewById<Switch>(R.id.swFly)
        val swNoclip = menuPanel.findViewById<Switch>(R.id.swNoclip)
        val swEsp = menuPanel.findViewById<Switch>(R.id.swEsp)
        val swFreeze = menuPanel.findViewById<Switch>(R.id.swFreeze)

        val sbPlayerSpeed = menuPanel.findViewById<SeekBar>(R.id.sbPlayerSpeed)
        val sbEnemySlow = menuPanel.findViewById<SeekBar>(R.id.sbEnemySlow)
        val sbGameSpeed = menuPanel.findViewById<SeekBar>(R.id.sbGameSpeed)

        swFly.isChecked = prefs.getBoolean("fly", false)
        swNoclip.isChecked = prefs.getBoolean("noclip", false)
        swEsp.isChecked = prefs.getBoolean("esp", false)
        swFreeze.isChecked = prefs.getBoolean("freeze", false)

        sbPlayerSpeed.progress = prefs.getInt("playerSpeed", 0)
        sbEnemySlow.progress = prefs.getInt("enemySlow", 0)
        sbGameSpeed.progress = prefs.getInt("gameSpeed", 0)

        updateLabels()

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateLabels()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        sbPlayerSpeed.setOnSeekBarChangeListener(listener)
        sbEnemySlow.setOnSeekBarChangeListener(listener)
        sbGameSpeed.setOnSeekBarChangeListener(listener)
    }

    private fun updateLabels() {
        val sbPlayerSpeed = menuPanel.findViewById<SeekBar>(R.id.sbPlayerSpeed)
        val sbEnemySlow = menuPanel.findViewById<SeekBar>(R.id.sbEnemySlow)
        val sbGameSpeed = menuPanel.findViewById<SeekBar>(R.id.sbGameSpeed)

        menuPanel.findViewById<TextView>(R.id.tvPlayerSpeed).text = "${sbPlayerSpeed.progress + 1}x"
        menuPanel.findViewById<TextView>(R.id.tvEnemySlow).text = "${sbEnemySlow.progress + 1}x"
        menuPanel.findViewById<TextView>(R.id.tvGameSpeed).text = "${sbGameSpeed.progress + 1}x"
    }

    private fun saveState() {
        val swFly = menuPanel.findViewById<Switch>(R.id.swFly)
        val swNoclip = menuPanel.findViewById<Switch>(R.id.swNoclip)
        val swEsp = menuPanel.findViewById<Switch>(R.id.swEsp)
        val swFreeze = menuPanel.findViewById<Switch>(R.id.swFreeze)

        val sbPlayerSpeed = menuPanel.findViewById<SeekBar>(R.id.sbPlayerSpeed)
        val sbEnemySlow = menuPanel.findViewById<SeekBar>(R.id.sbEnemySlow)
        val sbGameSpeed = menuPanel.findViewById<SeekBar>(R.id.sbGameSpeed)

        prefs.edit()
            .putBoolean("fly", swFly.isChecked)
            .putBoolean("noclip", swNoclip.isChecked)
            .putBoolean("esp", swEsp.isChecked)
            .putBoolean("freeze", swFreeze.isChecked)
            .putInt("playerSpeed", sbPlayerSpeed.progress)
            .putInt("enemySlow", sbEnemySlow.progress)
            .putInt("gameSpeed", sbGameSpeed.progress)
            .apply()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingButton.isInitialized) windowManager.removeView(floatingButton)
        if (::menuPanel.isInitialized) windowManager.removeView(menuPanel)
    }

    override fun onBind(intent: android.content.Intent?): IBinder? = null
}
