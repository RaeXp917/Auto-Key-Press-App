This is a simple desktop application that lets you record a specific key from your keyboard. Once recorded, you can set a time interval 
(speed) and make the app automatically press and release that key repeatedly until you tell it to stop.

Core Stack & Roles:

    Kotlin: Primary development language.

    Swing (javax.swing.*): Provides the GUI components (window, buttons, input fields).

    java.awt.Robot: Handles low-level input simulation (key presses/releases).

    Kotlin Coroutines: Manages the background task for asynchronous, non-blocking key repetition.

    KeyListener (java.awt.event): Captures user input for key recording.
