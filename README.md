This app allows users to record their own key presses and replay them with configurable speed (in milliseconds). It's useful for automation tasks, accessibility improvements, or repeating actions in a safe and controlled environment.

This tool is for educational and personal use only. Do not use it in ways that violate terms of service, privacy laws, or ethical standards.



Core Stack & Technologies

Programming Language
Kotlin – The primary language used for writing application logic and managing program structure.

User Interface Toolkit
Java Swing (javax.swing.*) – Provides the GUI elements including windows, buttons, and input fields.
AWT Event System (java.awt.event.KeyListener) – Captures and processes user keyboard input events.

System-Level Interaction
AWT Robot (java.awt.Robot) – Enables low-level keyboard automation by simulating key presses and releases programmatically.

Concurrency & Asynchronous Processing
Kotlin Coroutines – Manages background tasks such as repeated key execution in an asynchronous and non-blocking manner.
