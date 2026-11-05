package com.rajumuke.voice;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class DictateAndSendAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        VoiceInput.toggle(e.getProject(), true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
