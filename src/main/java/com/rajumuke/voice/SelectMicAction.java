package com.rajumuke.voice;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import org.jetbrains.annotations.NotNull;

public class SelectMicAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String current = MicSelector.selected();
        JBPopupFactory.getInstance()
                .createPopupChooserBuilder(MicSelector.availableInputs())
                .setTitle("Select Microphone")
                .setSelectedValue(current, true)
                .setItemChosenCallback(name -> {
                    MicSelector.select(name);
                    VoiceInput.notify(e.getProject(), "Microphone: " + name
                            + (HandsFree.isArmed() ? " (applies the next time hands-free is switched on)" : ""),
                            NotificationType.INFORMATION);
                })
                .createPopup()
                .showCenteredInCurrentWindow(e.getProject());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
