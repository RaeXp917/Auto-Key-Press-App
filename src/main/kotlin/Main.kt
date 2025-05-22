import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeHookException
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import kotlinx.coroutines.*
import java.awt.FlowLayout
import java.awt.Robot
import java.awt.event.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.prefs.Preferences
import javax.swing.*

private lateinit var robot: Robot
private var recordedKeyCode: Int? = null
private var autoPressingJob: Job? = null
private var isListeningForKeyToPress = false

private lateinit var frame: JFrame
private lateinit var recordKeyButton: JButton
private lateinit var startButton: JButton
private lateinit var stopButton: JButton
private lateinit var intervalField: JTextField
private lateinit var intervalUnitSelector: JComboBox<String>
private lateinit var intervalLabel: JLabel
private lateinit var recordedKeyLabel: JLabel
private lateinit var statusLabel: JLabel
private lateinit var settingsButton: JButton
private lateinit var stopHotkeyInfoLabel: JLabel

private const val PREF_STOP_HOTKEY_MODIFIERS = "stopHotkeyModifiers"
private const val PREF_STOP_HOTKEY_KEYCODE = "stopHotkeyKeyCode"
private const val PREF_INTERVAL_UNIT = "intervalUnit"
private val prefs: Preferences = Preferences.userNodeForPackage(MainApp::class.java)

private var stopHotkeyModifiers = NativeKeyEvent.CTRL_MASK
private var stopHotkeyKeyCode = NativeKeyEvent.VC_S // Default: Ctrl+S

object GlobalHotkeyListener : NativeKeyListener {
    override fun nativeKeyPressed(e: NativeKeyEvent) {
        if (e.modifiers == stopHotkeyModifiers && e.keyCode == stopHotkeyKeyCode) {
            println("Global stop hotkey detected!")
            SwingUtilities.invokeLater { stopAutoPressing() }
        }
    }
    override fun nativeKeyReleased(e: NativeKeyEvent) {}
    override fun nativeKeyTyped(e: NativeKeyEvent) {}
}

object MainApp {}

fun main() {
    val logger = Logger.getLogger(GlobalScreen::class.java.getPackage().name)
    logger.level = Level.OFF
    logger.useParentHandlers = false

    try {
        robot = Robot()
    } catch (e: Exception) {
        handleFatalError("Failed to initialize Robot: ${e.message}")
        return
    }

    SwingUtilities.invokeLater {
        createAndShowGUI()
        loadSettings()
        updateStopHotkeyDisplay()

        try {
            GlobalScreen.registerNativeHook()
            GlobalScreen.addNativeKeyListener(GlobalHotkeyListener)
            println("Native hook registered successfully.")
        } catch (ex: NativeHookException) {
            System.err.println("Problem registering native hook: ${ex.message}")
            val parent = if (::frame.isInitialized) frame else null
            JOptionPane.showMessageDialog(parent,
                "Could not register global hotkey listener.\nStop hotkey might not work if app isn't focused.",
                "Hotkey Warning", JOptionPane.WARNING_MESSAGE)
        }
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        try {
            if (GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.removeNativeKeyListener(GlobalHotkeyListener)
                GlobalScreen.unregisterNativeHook()
                println("Native hook unregistered.")
            }
        } catch (e: Exception) {
            System.err.println("Error during native hook unregistration: ${e.message}")
        }
    })
}

private fun handleFatalError(message: String) {
    println("Fatal Error: $message")
    val parent = if (::frame.isInitialized) frame else null
    JOptionPane.showMessageDialog(parent, message, "Fatal Error", JOptionPane.ERROR_MESSAGE)
}

private fun loadSettings() {
    stopHotkeyModifiers = prefs.getInt(PREF_STOP_HOTKEY_MODIFIERS, NativeKeyEvent.CTRL_MASK)
    stopHotkeyKeyCode = prefs.getInt(PREF_STOP_HOTKEY_KEYCODE, NativeKeyEvent.VC_S)
    val savedUnit = prefs.get(PREF_INTERVAL_UNIT, "Seconds")
    if (::intervalUnitSelector.isInitialized) {
        intervalUnitSelector.selectedItem = savedUnit
        intervalLabel.text = "Interval (${intervalUnitSelector.selectedItem}):"
    }
    println("Loaded stop hotkey: ${formatNativeHotkey(stopHotkeyModifiers, stopHotkeyKeyCode)}")
    println("Loaded interval unit: $savedUnit")
}

private fun saveSettings() {
    prefs.putInt(PREF_STOP_HOTKEY_MODIFIERS, stopHotkeyModifiers)
    prefs.putInt(PREF_STOP_HOTKEY_KEYCODE, stopHotkeyKeyCode)
    if (::intervalUnitSelector.isInitialized) {
        prefs.put(PREF_INTERVAL_UNIT, intervalUnitSelector.selectedItem as String)
    }
    try {
        prefs.flush()
        println("Saved settings.")
    } catch (e: Exception) {
        println("Error saving preferences: ${e.message}")
    }
}

private fun updateStopHotkeyDisplay() {
    if (::stopHotkeyInfoLabel.isInitialized) {
        stopHotkeyInfoLabel.text = "Global Stop Hotkey: ${formatNativeHotkey(stopHotkeyModifiers, stopHotkeyKeyCode)}"
    }
}

private fun setControlsEnabledState(isPressing: Boolean = false, isRecordingKey: Boolean = false) {
    val normalModeEnabled = !isPressing && !isRecordingKey

    recordKeyButton.isEnabled = normalModeEnabled
    startButton.isEnabled = normalModeEnabled && recordedKeyCode != null
    stopButton.isEnabled = isPressing
    intervalField.isEnabled = normalModeEnabled
    intervalUnitSelector.isEnabled = normalModeEnabled
    settingsButton.isEnabled = normalModeEnabled

    if (isRecordingKey) {
        statusLabel.text = "Status: Press a key to be auto-pressed..."
    } else if (isPressing) {
        val keyText = if (recordedKeyCode != null) KeyEvent.getKeyText(recordedKeyCode!!) else "Unknown"
        statusLabel.text = "Status: Auto-pressing '$keyText'..."
    } else {
        statusLabel.text = "Status: Idle"
    }
}

private fun createAndShowGUI() {
    frame = JFrame("Kotlin Auto Key Presser")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    // frame.setSize(500, 200) // pack() will handle initial sizing

    val mainPanel = JPanel()
    mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS) // Stack panels vertically

    val topPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
    intervalLabel = JLabel("Interval (Seconds):")
    intervalField = JTextField("1.0", 5)
    val units = arrayOf("Seconds", "Milliseconds")
    intervalUnitSelector = JComboBox(units)
    intervalUnitSelector.addActionListener {
        intervalLabel.text = "Interval (${intervalUnitSelector.selectedItem}):"
    }
    topPanel.add(intervalLabel)
    topPanel.add(intervalField)
    topPanel.add(intervalUnitSelector)

    val middlePanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
    recordKeyButton = JButton("Record Key to Press")
    startButton = JButton("Start Auto-Press")
    stopButton = JButton("Stop")
    settingsButton = JButton("Settings")
    middlePanel.add(recordKeyButton)
    middlePanel.add(startButton)
    middlePanel.add(stopButton)
    middlePanel.add(settingsButton)

    val bottomPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
    recordedKeyLabel = JLabel("Key to Press: None")
    statusLabel = JLabel("Status: Idle")
    stopHotkeyInfoLabel = JLabel("Global Stop Hotkey: Initializing...")
    bottomPanel.add(recordedKeyLabel)
    bottomPanel.add(Box.createHorizontalStrut(10)) // Spacer
    bottomPanel.add(statusLabel)
    bottomPanel.add(Box.createHorizontalStrut(10)) // Spacer
    bottomPanel.add(stopHotkeyInfoLabel)

    mainPanel.add(topPanel)
    mainPanel.add(middlePanel)
    mainPanel.add(bottomPanel)
    frame.add(mainPanel)

    setControlsEnabledState()

    recordKeyButton.addActionListener {
        isListeningForKeyToPress = true
        setControlsEnabledState(isRecordingKey = true)
        frame.requestFocusInWindow()
    }

    startButton.addActionListener {
        if (recordedKeyCode == null) {
            JOptionPane.showMessageDialog(frame, "Please record a key to auto-press first!", "Error", JOptionPane.ERROR_MESSAGE)
            return@addActionListener
        }
        try {
            val intervalValue = intervalField.text.toDouble()
            if (intervalValue <= 0) {
                JOptionPane.showMessageDialog(frame, "Interval must be a positive number.", "Error", JOptionPane.ERROR_MESSAGE)
                return@addActionListener
            }
            val intervalMillis = if (intervalUnitSelector.selectedItem == "Seconds") {
                (intervalValue * 1000).toLong()
            } else {
                intervalValue.toLong()
            }
            startAutoPressing(intervalMillis)
        } catch (e: NumberFormatException) {
            JOptionPane.showMessageDialog(frame, "Invalid interval. Please enter a number.", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    stopButton.addActionListener { stopAutoPressing() }
    settingsButton.addActionListener { SettingsDialog(frame).isVisible = true }

    frame.isFocusable = true
    frame.addKeyListener(object : KeyListener {
        override fun keyTyped(e: KeyEvent?) {}
        override fun keyPressed(e: KeyEvent?) {
            if (isListeningForKeyToPress && e != null) {
                recordedKeyCode = e.keyCode
                val keyText = KeyEvent.getKeyText(e.keyCode)
                recordedKeyLabel.text = "Key to Press: $keyText (Code: ${e.keyCode})"
                isListeningForKeyToPress = false
                setControlsEnabledState()
            }
        }
        override fun keyReleased(e: KeyEvent?) {}
    })

    frame.pack()
    frame.minimumSize = frame.size
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
    frame.requestFocusInWindow()
}

fun formatNativeHotkey(modifiers: Int, keyCode: Int): String {
    val parts = mutableListOf<String>()
    if ((modifiers and NativeKeyEvent.CTRL_MASK) != 0) parts.add("Ctrl")
    if ((modifiers and NativeKeyEvent.ALT_MASK) != 0) parts.add("Alt")
    if ((modifiers and NativeKeyEvent.SHIFT_MASK) != 0) parts.add("Shift")
    if ((modifiers and NativeKeyEvent.META_MASK) != 0) parts.add("Meta/Win")
    parts.add(NativeKeyEvent.getKeyText(keyCode))
    return parts.joinToString(" + ")
}

private fun startAutoPressing(intervalMillis: Long) {
    if (autoPressingJob?.isActive == true) return
    val currentKeyCodeToPress = recordedKeyCode ?: return

    autoPressingJob = CoroutineScope(Dispatchers.Default).launch {
        try {
            SwingUtilities.invokeLater { setControlsEnabledState(isPressing = true) }
            while (isActive) {
                robot.keyPress(currentKeyCodeToPress)
                robot.delay(30)
                robot.keyRelease(currentKeyCodeToPress)
                delay(intervalMillis)
            }
        } catch (e: CancellationException) {
            println("Auto-pressing coroutine cancelled.")
        } catch (e: Exception) {
            println("Error during auto-pressing: ${e.message}")
            SwingUtilities.invokeLater {
                statusLabel.text = "Status: Error during pressing."
                JOptionPane.showMessageDialog(frame, "Error during auto-pressing: ${e.message}", "Runtime Error", JOptionPane.ERROR_MESSAGE)
            }
        } finally {
            SwingUtilities.invokeLater { setControlsEnabledState() }
        }
    }
}

fun stopAutoPressing() {
    autoPressingJob?.cancel()
    SwingUtilities.invokeLater {
        statusLabel.text = "Status: Stop requested."
        if (autoPressingJob == null || !(autoPressingJob!!.isActive)) {
            setControlsEnabledState()
        }
    }
}

class SettingsDialog(owner: JFrame) : JDialog(owner, "Settings", true), NativeKeyListener {
    private var tempModifiers: Int = stopHotkeyModifiers
    private var tempKeyCode: Int = stopHotkeyKeyCode
    private val hotkeyStatusLabel: JLabel
    private var isListeningForStopHotkey = false
    private val recordStopHotkeyButton: JButton
    private val saveButton: JButton // Made saveButton a class member

    init {
        title = "Hotkey Settings"
        // setSize(400, 150) // pack() will handle sizing
        // layout = FlowLayout(FlowLayout.CENTER, 10, 10) // Will use BoxLayout for contentPane
        setLocationRelativeTo(owner)

        hotkeyStatusLabel = JLabel("Current: ${formatNativeHotkey(tempModifiers, tempKeyCode)}")
        recordStopHotkeyButton = JButton("Record New Stop Hotkey")
        saveButton = JButton("Save") // Initialize class member
        val cancelButton = JButton("Cancel")

        recordStopHotkeyButton.addActionListener {
            isListeningForStopHotkey = true
            hotkeyStatusLabel.text = "Press new hotkey combination..."
            recordStopHotkeyButton.isEnabled = false
            saveButton.isEnabled = false
            GlobalScreen.addNativeKeyListener(this)
            requestFocusInWindow()
        }

        saveButton.addActionListener {
            if (isListeningForStopHotkey) {
                GlobalScreen.removeNativeKeyListener(this)
                isListeningForStopHotkey = false
            }
            stopHotkeyModifiers = tempModifiers
            stopHotkeyKeyCode = tempKeyCode
            saveSettings()
            updateStopHotkeyDisplay()
            dispose()
        }

        cancelButton.addActionListener {
            if (isListeningForStopHotkey) {
                GlobalScreen.removeNativeKeyListener(this)
                isListeningForStopHotkey = false
            }
            dispose()
        }

        val topPanelDialog = JPanel(FlowLayout(FlowLayout.CENTER))
        topPanelDialog.add(hotkeyStatusLabel)

        val bottomPanelDialog = JPanel(FlowLayout(FlowLayout.CENTER))
        bottomPanelDialog.add(recordStopHotkeyButton)
        bottomPanelDialog.add(saveButton)
        bottomPanelDialog.add(cancelButton)

        contentPane.layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)
        contentPane.add(topPanelDialog)
        contentPane.add(bottomPanelDialog)

        pack()
        minimumSize = size

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                if (isListeningForStopHotkey) {
                    GlobalScreen.removeNativeKeyListener(this@SettingsDialog)
                }
            }
        })
    }

    override fun nativeKeyPressed(e: NativeKeyEvent) {
        if (isListeningForStopHotkey) {
            val consumedMask: Short = 0x01
            try {
                var currentClass: Class<*>? = e::class.java
                var reservedField: java.lang.reflect.Field? = null
                while (currentClass != null && reservedField == null) {
                    try {
                        reservedField = currentClass.getDeclaredField("reserved")
                    } catch (nsf: NoSuchFieldException) { /* ignore */ }
                    currentClass = currentClass.superclass
                }
                if (reservedField != null) {
                    reservedField.isAccessible = true
                    val currentReservedValue = reservedField.get(e) as Short
                    reservedField.set(e, (currentReservedValue.toInt() or consumedMask.toInt()).toShort())
                } else {
                    println("WORKAROUND WARNING: Could not find 'reserved' field.")
                }
            } catch (ex: Exception) {
                println("WORKAROUND ERROR: ${ex.message}")
            }

            tempModifiers = e.modifiers
            tempKeyCode = e.keyCode
            SwingUtilities.invokeLater {
                hotkeyStatusLabel.text = "New: ${formatNativeHotkey(tempModifiers, tempKeyCode)}"
                isListeningForStopHotkey = false
                recordStopHotkeyButton.isEnabled = true
                saveButton.isEnabled = true // Directly enable the saveButton member
            }
            GlobalScreen.removeNativeKeyListener(this)
        }
    }
    override fun nativeKeyReleased(e: NativeKeyEvent) {}
    override fun nativeKeyTyped(e: NativeKeyEvent) {}
}