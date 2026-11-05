package com.rajumuke.voice;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import org.jetbrains.annotations.NotNull;

public class HandsFreeToggleAction extends ToggleAction {
    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return HandsFree.isArmed();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        if (state) {
            HandsFree.arm(e.getProject());
        } else {
            HandsFree.disarm();
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
