package com.rajumuke.voice;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;

import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import javax.swing.SwingUtilities;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;

final class TextInserter {
    private static final Logger LOG = Logger.getInstance(TextInserter.class);
    private static final long SEND_DELAY_MS = 300;
    private static final long CLIPBOARD_RESTORE_MS = 1000;

    private TextInserter() {
    }

    /** Must run on the EDT. Posts AWT events in-process, so no macOS Accessibility permission is needed. */
    static boolean insert(String text, boolean send) {
        Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focused == null) {
            return false;
        }
        Component chatInput = focused instanceof JTextComponent ? null : findChatInput(SwingUtilities.getWindowAncestor(focused));
        // Focus can sit on a Compose/Skia surface, where pasted text and Enter go nowhere
        Component target = chatInput != null ? chatInput : focused;
        if (chatInput != null) {
            chatInput.requestFocusInWindow();
        }
        LOG.info("Voice insert target: " + target.getClass().getName() + ", send=" + send);
        if (target instanceof JTextComponent textComponent && textComponent.isEditable()) {
            textComponent.replaceSelection(text);
        } else {
            pasteViaClipboard(target, text);
        }
        if (send) {
            AppExecutorUtil.getAppScheduledExecutorService().schedule(
                    () -> java.awt.EventQueue.invokeLater(() -> pressEnter(target)), SEND_DELAY_MS, TimeUnit.MILLISECONDS);
        }
        return true;
    }

    private static Component findChatInput(Component root) {
        if (root == null) {
            return null;
        }
        if (root instanceof JTextComponent && root.isShowing() && root.getClass().getName().contains("CopilotAgentInputTextArea")) {
            return root;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component found = findChatInput(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void pasteViaClipboard(Component target, String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable previous = clipboard.getContents(null);
        clipboard.setContents(new StringSelection(text), null);
        post(target, KeyEvent.KEY_PRESSED, InputEvent.META_DOWN_MASK, KeyEvent.VK_V, 'v');
        post(target, KeyEvent.KEY_RELEASED, InputEvent.META_DOWN_MASK, KeyEvent.VK_V, 'v');
        if (previous != null) {
            AppExecutorUtil.getAppScheduledExecutorService().schedule(
                    () -> clipboard.setContents(previous, null), CLIPBOARD_RESTORE_MS, TimeUnit.MILLISECONDS);
        }
    }

    private static void pressEnter(Component target) {
        post(target, KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_ENTER, '\n');
        post(target, KeyEvent.KEY_TYPED, 0, KeyEvent.VK_UNDEFINED, '\n');
        post(target, KeyEvent.KEY_RELEASED, 0, KeyEvent.VK_ENTER, '\n');
    }

    private static void post(Component target, int id, int modifiers, int keyCode, char keyChar) {
        Toolkit.getDefaultToolkit().getSystemEventQueue()
                .postEvent(new KeyEvent(target, id, System.currentTimeMillis(), modifiers, keyCode, keyChar));
    }
}
