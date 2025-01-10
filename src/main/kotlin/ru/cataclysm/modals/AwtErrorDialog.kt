package ru.cataclysm.modals

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import ru.cataclysm.services.Log
import kotlin.system.exitProcess

object AwtErrorDialog {
    fun showError(message: String, t: Throwable?) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (ignored: ClassNotFoundException) {
        } catch (ignored: InstantiationException) {
        } catch (ignored: IllegalAccessException) {
        } catch (ignored: UnsupportedLookAndFeelException) {
        }

        Log.err(t, message)

        if (t == null) {
            JOptionPane.showMessageDialog(null, message, "Ошибка", JOptionPane.ERROR_MESSAGE)
        } else {
            val dialog = JDialog(null as Frame?, "Ошибка", true)
            dialog.layout = GridBagLayout()

            val gbc = GridBagConstraints()

            gbc.gridx = 0
            gbc.gridy = 0
            gbc.anchor = GridBagConstraints.WEST
            gbc.weightx = 1.0
            gbc.insets[10, 10, 10] = 10
            dialog.add(JLabel(message), gbc)
            dialog.setSize(500, 400)
            dialog.setLocationRelativeTo(null)

            gbc.gridy = 1
            gbc.fill = GridBagConstraints.BOTH
            gbc.weightx = 1.0
            gbc.weighty = 1.0
            gbc.insets[0, 10, 0] = 10

            val ta = JTextArea()
            ta.font = Font("Consolas", Font.PLAIN, 12)
            ta.text = t.stackTraceToString()

            val scroll = JScrollPane(
                ta, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            )
            scroll.maximumSize = Dimension(400, 500)
            dialog.add(scroll, gbc)

            val ok = JButton("Ок")
            ok.addActionListener { e: ActionEvent? -> dialog.isVisible = false }

            gbc.fill = GridBagConstraints.NONE
            gbc.anchor = GridBagConstraints.CENTER
            gbc.insets[10, 10, 10] = 10
            gbc.gridy = 2
            gbc.weightx = 0.0
            gbc.weighty = 0.0
            dialog.add(ok, gbc)

            dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            dialog.isVisible = true
        }

        exitProcess(1)
    }
}