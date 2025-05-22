This app allows users to record their own key presses and replay them with configurable speed (in milliseconds). It's useful for automation tasks, accessibility improvements, or repeating actions in a safe and controlled environment.

This tool is for educational and personal use only. Do not use it in ways that violate terms of service, privacy laws, or ethical standards.


Core Stack & Roles:

Kotlin: Primary development language.

Swing (javax.swing.*): Provides the GUI components (window, buttons, input fields).

java.awt.Robot: Handles low-level input simulation (key presses/releases).

Kotlin Coroutines: Manages the background task for asynchronous, non-blocking key repetition.

KeyListener (java.awt.event): Captures user input for key recording.
