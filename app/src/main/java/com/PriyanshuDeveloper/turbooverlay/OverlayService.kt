package com.PriyanshuDeveloper.turbooverlay

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.PriyanshuDeveloper.turbooverlay.databinding.OverlayHandleBinding
import com.PriyanshuDeveloper.turbooverlay.databinding.OverlayPanelBinding
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var handleView: View? = null
    private var panelView: View? = null
    private var _handleBinding: OverlayHandleBinding? = null
    private var _panelBinding: OverlayPanelBinding? = null

    private val metricsReader = MetricsReader()
    private val handler = Handler(Looper.getMainLooper())
    private var backgroundHandler: Handler? = null
    private var backgroundThread: android.os.HandlerThread? = null

    @Volatile
    private var isDestroyed = false

    private val cpuHistory = FloatArray(240)
    private val batHistory = FloatArray(240)
    private var historyCount = 0
    private var historyHead = 0
    private val maxHistoryPoints = 240

    @Volatile
    private var activeTab = "CPU"
    @Volatile
    private var isPerfPMEnabled = false
    private var lastBoostTime = 0L

    // For dragging handle
    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var handleStartX = 0
    private var handleStartY = 0
    private var screenWidth = 0
    private var screenHeight = 0

    // For Panel dragging and gestures
    private var isPanelDragging = false
    private var panelStartX = 0
    private var panelStartY = 0
    private var isPanelHiding = false

    private val panelLongPressRunnable = Runnable {
        if (!isDestroyed && panelView != null) {
            isPanelDragging = true
            panelView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            _panelBinding?.panelDragHandleLeft?.visibility = View.VISIBLE
            _panelBinding?.panelDragHandleRight?.visibility = View.VISIBLE
        }
    }

    private val batMetrics = MetricsReader.BatteryPowerMetrics()
    private val netMetrics = MetricsReader.NetSpeedMetrics()
    private var lastTempFetchTime = 0L
    private var lastNetFetchTime = 0L
    private var lastLatencyFetchTime = 0L
    private var cachedTemp = 0f
    private var cachedCoreFreqs = Array(8) { "Off" }

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isDestroyed) return

            if (!ShizukuShellManager.isReady()) {
                ShizukuShellManager.init(this@OverlayService)
                showUnavailableMetrics()
                if (!isDestroyed) {
                    handler.postDelayed(this, 1000)
                }
                return
            }

            val isPanelVisible = panelView != null
            val currentActiveTab = activeTab
            val displayedChild = _panelBinding?.cpuViewFlipper?.displayedChild ?: 0

            backgroundHandler?.post {
                if (isDestroyed) return@post

                val cpu = metricsReader.getCpuUsage()

                val now = System.currentTimeMillis()
                if (now - lastTempFetchTime >= 1000 || cachedTemp == 0f) {
                    cachedTemp = metricsReader.getBatteryTemp(this@OverlayService)
                    lastTempFetchTime = now
                }

                if (isPanelVisible) {
                    if (currentActiveTab == "CPU" && displayedChild == 1) {
                        cachedCoreFreqs = metricsReader.getCpuCoreFreqs()
                    } else if (currentActiveTab == "BAT") {
                        metricsReader.getBatteryPowerMetrics(this@OverlayService, batMetrics)
                    } else if (currentActiveTab == "NET") {
                        if (now - lastNetFetchTime >= 1500) {
                            metricsReader.getNetSpeed(netMetrics)
                            lastNetFetchTime = now
                        }

                        if (now - lastLatencyFetchTime >= 2000) {
                            netMetrics.latencyMs = metricsReader.getLatency()
                            netMetrics.signalDbm = metricsReader.getSignalStrength()
                            lastLatencyFetchTime = now
                        }
                    }
                }

                if (!isDestroyed) {
                    handler.post {
                        if (isDestroyed) return@post
                        updateUI(cpu, cachedTemp, cachedCoreFreqs)
                        handler.postDelayed(this, 500)
                    }
                }

                if (isPerfPMEnabled) {
                    val boostNow = System.currentTimeMillis()
                    if (boostNow - lastBoostTime >= 60000) {
                        lastBoostTime = boostNow
                        runPerformanceCommands(showToast = true, customMessage = getString(R.string.perf_auto_boost_applied))
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        updateScreenDimensions()

        backgroundThread = android.os.HandlerThread("MetricsReaderWorker").apply { start() }
        backgroundHandler = Handler(backgroundThread!!.looper)

        startForegroundService()
        ShizukuShellManager.init(this)
        showHandle()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateScreenDimensions()
    }

    private fun updateScreenDimensions() {
        val metrics = resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
    }

    private fun startForegroundService() {
        val channelId = "TurboOverlay"
        val channel = NotificationChannel(channelId, "Overlay Service", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = Notification.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_title))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun showHandle() {
        _handleBinding = OverlayHandleBinding.inflate(LayoutInflater.from(this))
        handleView = _handleBinding?.root
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 24
        }

        handleView?.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val params = v.layoutParams as? WindowManager.LayoutParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isDragging = false
                        dragStartX = event.rawX
                        dragStartY = event.rawY
                        handleStartX = params.x
                        handleStartY = params.y

                        _handleBinding?.handleLine?.setBackgroundColor(Color.argb(200, 129, 199, 132))
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - dragStartX).toInt()
                        val deltaY = (event.rawY - dragStartY).toInt()

                        val isLeftEdge = handleStartX < screenWidth / 2
                        val swipeOut = if (isLeftEdge) deltaX > 80 else deltaX < -80

                        if (swipeOut && !isDragging) {
                            if (panelView == null && !isPanelHiding) showPanel()
                            return true
                        }

                        if (!isDragging && (abs(deltaX) > 10 || abs(deltaY) > 10)) {
                            isDragging = true
                            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }

                        if (isDragging) {
                            params.x = (handleStartX + deltaX).coerceIn(0, (screenWidth - v.width).coerceAtLeast(0))
                            params.y = (handleStartY + deltaY).coerceIn(0, (screenHeight - v.height).coerceAtLeast(0))
                            try {
                                windowManager.updateViewLayout(v, params)
                            } catch (_: Exception) {}
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        _handleBinding?.handleLine?.setBackgroundColor(Color.argb(230, 255, 255, 255))

                        val deltaX = (event.rawX - dragStartX).toInt()
                        val isLeftEdge = handleStartX < screenWidth / 2
                        val swipeOut = if (isLeftEdge) deltaX > 80 else deltaX < -80

                        if (swipeOut) {
                            if (panelView == null && !isPanelHiding) showPanel()
                        } else if (!isDragging) {
                            togglePanel()
                        }

                        isDragging = false
                        v.performClick()
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        _handleBinding?.handleLine?.setBackgroundColor(Color.argb(230, 255, 255, 255))
                        isDragging = false
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager.addView(handleView, params)
        } catch (_: Exception) {}
        handler.post(updateRunnable)
    }

    private fun togglePanel() {
        if (panelView == null && !isPanelHiding) showPanel() else hidePanel()
    }

    private val gestureDetector by lazy {
        android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (abs(velocityX) > abs(velocityY) && abs(velocityX) > 100) {
                    toggleSubSection()
                    return true
                }
                return false
            }
        })
    }

    private fun toggleSubSection() {
        _panelBinding?.cpuViewFlipper?.let { flipper ->
            val nextChild = when (activeTab) {
                "CPU" -> if (flipper.displayedChild == 0) 1 else 0
                "BAT" -> if (flipper.displayedChild == 0) 2 else 0
                "NET" -> 3
                else -> return@let
            }
            if (nextChild != flipper.displayedChild) {
                flipper.displayedChild = nextChild
                refreshTabUI()
            }
        }
    }

    private fun showPanel() {
        if (panelView != null || isPanelHiding) return

        _panelBinding = OverlayPanelBinding.inflate(LayoutInflater.from(this))
        panelView = _panelBinding?.root

        val handleParams = handleView?.layoutParams as? WindowManager.LayoutParams
        val handleX = handleParams?.x ?: 0
        val handleY = handleParams?.y ?: 24

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (handleX < screenWidth / 2) handleX + 12 else handleX - 320
            y = handleY
        }

        panelView?.alpha = 0f
        panelView?.translationX = if (handleX < screenWidth / 2) -360f else 360f

        try {
            windowManager.addView(panelView, params)
        } catch (_: Exception) {
            panelView = null
            _panelBinding = null
            return
        }

        panelView?.animate()
            ?.translationX(0f)
            ?.alpha(1f)
            ?.setDuration(300)
            ?.setInterpolator(FastOutSlowInInterpolator())
            ?.start()

        _panelBinding?.apply {
            cpuViewFlipper.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }

            btnTabCpu.setOnClickListener {
                if (activeTab == "CPU") toggleSubSection() else {
                    activeTab = "CPU"
                    cpuViewFlipper.displayedChild = 0
                }
                refreshTabUI()
            }
            btnTabBat.setOnClickListener {
                if (activeTab == "BAT") toggleSubSection() else {
                    activeTab = "BAT"
                    cpuViewFlipper.displayedChild = 0
                }
                refreshTabUI()
            }
            btnTabNet.setOnClickListener {
                activeTab = "NET"
                cpuViewFlipper.displayedChild = 3
                refreshTabUI()
            }
            btnTabPerf.setOnClickListener {
                activeTab = "PERF"
                refreshTabUI()
            }
            btnClosePanel.setOnClickListener { hidePanel() }
            btnClosePanel.setOnTouchListener { v, event ->
                val panelParams = panelView?.layoutParams as? WindowManager.LayoutParams ?: return@setOnTouchListener false
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dragStartX = event.rawX
                        dragStartY = event.rawY
                        panelStartX = panelParams.x
                        panelStartY = panelParams.y
                        isPanelDragging = false
                        handler.removeCallbacks(panelLongPressRunnable)
                        handler.postDelayed(panelLongPressRunnable, 3000)

                        v.backgroundTintList = ColorStateList.valueOf(0xFF81C784.toInt())
                        v.scaleX = 0.95f
                        v.scaleY = 0.95f
                        false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - dragStartX).toInt()
                        val deltaY = (event.rawY - dragStartY).toInt()

                        if (!isPanelDragging && (abs(deltaX) > 20 || abs(deltaY) > 20)) {
                            handler.removeCallbacks(panelLongPressRunnable)
                        }

                        if (isPanelDragging) {
                            panelParams.x = (panelStartX + deltaX).coerceIn(0, (screenWidth - (panelView?.width ?: 0)).coerceAtLeast(0))
                            panelParams.y = (panelStartY + deltaY).coerceIn(0, (screenHeight - (panelView?.height ?: 0)).coerceAtLeast(0))
                            try {
                                windowManager.updateViewLayout(panelView, panelParams)
                            } catch (_: Exception) {}

                            val isLeftEdge = panelStartX < screenWidth / 2
                            val swipeIn = if (isLeftEdge) deltaX < -120 else deltaX > 120
                            if (swipeIn) {
                                hidePanel()
                                isPanelDragging = false
                                handler.removeCallbacks(panelLongPressRunnable)
                            }
                            true
                        } else {
                            val isLeftEdge = panelStartX < screenWidth / 2
                            val swipeIn = if (isLeftEdge) deltaX < -80 else deltaX > 80
                            if (swipeIn) {
                                handler.removeCallbacks(panelLongPressRunnable)
                                hidePanel()
                                true
                            } else {
                                false
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(panelLongPressRunnable)
                        v.scaleX = 1f
                        v.scaleY = 1f
                        _panelBinding?.let { updateIconButtonStates(it) }

                        val wasDragging = isPanelDragging
                        isPanelDragging = false
                        _panelBinding?.panelDragHandleLeft?.visibility = View.GONE
                        _panelBinding?.panelDragHandleRight?.visibility = View.GONE

                        if (event.action == MotionEvent.ACTION_UP && !wasDragging && !isPanelHiding) {
                            v.performClick()
                            true
                        } else {
                            wasDragging
                        }
                    }
                    else -> false
                }
            }

            btnPerfAction.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                backgroundHandler?.post {
                    runPerformanceCommands(showToast = true, customMessage = getString(R.string.perf_boost_applied))
                    handler.post {
                        if (!isDestroyed && _panelBinding != null) {
                            updateIconButtonStates(this@apply)
                        }
                    }
                }
            }
            btnPerfPMAction.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                isPerfPMEnabled = !isPerfPMEnabled
                refreshTabUI()
                if (isPerfPMEnabled) {
                    backgroundHandler?.post {
                        runPerformanceCommands(showToast = true, customMessage = getString(R.string.perf_pm_on))
                    }
                } else {
                    showBottomToast(getString(R.string.perf_pm_off))
                }
            }

            installPressFeedback(btnTabCpu)
            installPressFeedback(btnTabBat)
            installPressFeedback(btnTabNet)
            installPressFeedback(btnTabPerf)
            installPressFeedback(btnPerfAction)
            installPressFeedback(btnPerfPMAction)
        }
        refreshTabUI()
    }

    private fun hidePanel() {
        if (panelView == null || isPanelHiding) return
        isPanelHiding = true
        handler.removeCallbacks(panelLongPressRunnable)

        val handleParams = handleView?.layoutParams as? WindowManager.LayoutParams
        val destX = if (handleParams != null && handleParams.x < screenWidth / 2) -360f else 360f

        panelView?.animate()
            ?.translationX(destX)
            ?.alpha(0f)
            ?.setDuration(200)
            ?.setInterpolator(FastOutLinearInInterpolator())
            ?.withEndAction {
                try {
                    if (panelView != null) {
                        windowManager.removeView(panelView)
                    }
                } catch (_: Exception) {}
                panelView = null
                _panelBinding = null
                isPanelHiding = false
            }
            ?.start()
    }

    private fun updateUI(cpu: Float, bat: Float, coreFreqs: Array<String>) {
        if (historyCount < maxHistoryPoints) {
            cpuHistory[historyCount] = cpu
            batHistory[historyCount] = bat
            historyCount++
        } else {
            cpuHistory[historyHead] = cpu
            batHistory[historyHead] = bat
            historyHead = (historyHead + 1) % maxHistoryPoints
        }

        val binding = _panelBinding ?: return

        binding.tvCpuLive.text = getString(R.string.cpu_format, cpu.toInt())

        updateTemperatureUI(bat, binding.tvBatLive, binding.btnTabBat)

        setPerformanceButtonsEnabled(binding, true)

        val displayedChild = binding.cpuViewFlipper.displayedChild
        when (activeTab) {
            "CPU" -> {
                if (displayedChild == 0) {
                    binding.graphView.setData(cpuHistory, historyCount, historyHead, 100f, "%")
                } else {
                    updateCpuCoresInfo(binding, coreFreqs)
                }
            }
            "BAT" -> {
                if (displayedChild == 0) {
                    binding.graphView.setData(batHistory, historyCount, historyHead, 50f, "°C")
                } else {
                    updateBatteryInfo(binding)
                }
            }
            "NET" -> {
                updateNetInfo(binding)
            }
        }
    }

    private fun updateCpuCoresInfo(binding: OverlayPanelBinding, freqs: Array<String>) {
        binding.tvCore0.text = "C0: ${freqs[0]}"
        binding.tvCore1.text = "C1: ${freqs[1]}"
        binding.tvCore2.text = "C2: ${freqs[2]}"
        binding.tvCore3.text = "C3: ${freqs[3]}"
        binding.tvCore4.text = "C4: ${freqs[4]}"
        binding.tvCore5.text = "C5: ${freqs[5]}"
        binding.tvCore6.text = "C6: ${freqs[6]}"
        binding.tvCore7.text = "C7: ${freqs[7]}"
    }

    private fun updateBatteryInfo(binding: OverlayPanelBinding) {
        binding.tvBatVoltage.text = getString(R.string.voltage_format, batMetrics.voltageV.format3Decimals())
        binding.tvBatCurrent.text = getString(R.string.current_format, batMetrics.currentMA.format3Decimals())
        binding.tvBatPower.text = getString(R.string.power_format, batMetrics.powerW.format3Decimals())
    }

    private fun updateNetInfo(binding: OverlayPanelBinding) {
        binding.tvNetUp.text = getString(R.string.net_up_format, netMetrics.upKBps.formatNetSpeed())
        binding.tvNetDown.text = getString(R.string.net_down_format, netMetrics.downKBps.formatNetSpeed())
        binding.tvNetLatency.text = if (netMetrics.latencyMs >= 0) {
            getString(R.string.net_latency_format, netMetrics.latencyMs)
        } else getString(R.string.latency_na)
        binding.tvNetSignal.text = getString(R.string.net_signal_format, netMetrics.signalDbm)
    }

    private fun updateTemperatureUI(tempC: Float, textView: TextView, statusIcon: ImageView) {
        textView.text = getString(R.string.bat_format, tempC.format1Decimal())

        val textColor = when {
            tempC < 33.1f -> Color.GREEN
            tempC <= 36.0f -> Color.WHITE
            tempC <= 40.0f -> Color.YELLOW
            else -> Color.RED
        }

        val colorAnim = ValueAnimator.ofArgb(textView.currentTextColor, textColor)
        colorAnim.duration = 200
        colorAnim.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            textView.setTextColor(color)
            statusIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
        colorAnim.start()
    }

    private fun showUnavailableMetrics() {
        _panelBinding?.let { binding ->
            binding.tvCpuLive.text = getString(R.string.cpu_na)
            binding.tvBatLive.text = getString(R.string.bat_na)
            setPerformanceButtonsEnabled(binding, false)
        }
    }

    private fun refreshTabUI() {
        _panelBinding?.apply {
            if (activeTab == "PERF") {
                cpuViewFlipper.visibility = View.GONE
                performancePanel.visibility = View.VISIBLE
            } else {
                cpuViewFlipper.visibility = View.VISIBLE
                performancePanel.visibility = View.GONE
            }
            updateIconButtonStates(this)
        }
    }

    private fun runPerformanceCommands(showToast: Boolean, customMessage: String? = null) {
        if (!ShizukuShellManager.isReady()) return
        val msg = customMessage ?: getString(R.string.perf_boost_applied)
        if (showToast) showBottomToast(msg)

        val cmds = arrayOf(
            "cmd power set-adaptive-power-saver-enabled false 2>/dev/null",
            "cmd power set-fixed-performance-mode-enabled true 2>/dev/null",
            "settings put global low_power 0 2>/dev/null",
            "settings put global power_saving_mode 0 2>/dev/null",
            "cmd deviceidle disable all 2>/dev/null",
            "cmd activity compaction perform 2>/dev/null",
            "am trim-memory RUNNING_CRITICAL 2>/dev/null",
            "am kill-all 2>/dev/null"
        )
        for (cmd in cmds) metricsReader.executeShellCommand(cmd)
    }

    private fun setPerformanceButtonsEnabled(binding: OverlayPanelBinding, enabled: Boolean) {
        val alpha = if (enabled) 1f else 0.45f
        binding.btnPerfAction.isEnabled = enabled
        binding.btnPerfAction.alpha = alpha
        binding.btnPerfPMAction.isEnabled = enabled
        binding.btnPerfPMAction.alpha = alpha
    }

    private fun updateIconButtonStates(binding: OverlayPanelBinding) {
        val activeTint = ColorStateList.valueOf(0xFF4CAF50.toInt())
        val inactiveTint = ColorStateList.valueOf(0xFF333333.toInt())
        val pmTint = ColorStateList.valueOf(if (isPerfPMEnabled) 0xFF4CAF50.toInt() else 0xFF333333.toInt())

        binding.btnTabCpu.backgroundTintList = if (activeTab == "CPU") activeTint else inactiveTint
        binding.btnTabBat.backgroundTintList = if (activeTab == "BAT") activeTint else inactiveTint
        binding.btnTabNet.backgroundTintList = if (activeTab == "NET") activeTint else inactiveTint
        binding.btnTabPerf.backgroundTintList = if (activeTab == "PERF") activeTint else inactiveTint
        binding.btnPerfPMAction.backgroundTintList = pmTint
        binding.btnPerfAction.backgroundTintList = inactiveTint
    }

    private fun installPressFeedback(button: ImageButton) {
        val pressedTint = ColorStateList.valueOf(0xFF81C784.toInt())
        button.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    button.backgroundTintList = pressedTint
                    view.scaleX = 0.95f
                    view.scaleY = 0.95f
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.scaleX = 1f
                    view.scaleY = 1f
                    _panelBinding?.let { updateIconButtonStates(it) }
                }
            }
            false
        }
    }

    private fun showBottomToast(message: String) {
        handler.post {
            if (!isDestroyed) {
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        isDestroyed = true
        handler.removeCallbacksAndMessages(null)
        backgroundHandler?.removeCallbacksAndMessages(null)
        backgroundThread?.quitSafely()

        handleView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        panelView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        handleView = null
        panelView = null
        _handleBinding = null
        _panelBinding = null

        ShizukuShellManager.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
