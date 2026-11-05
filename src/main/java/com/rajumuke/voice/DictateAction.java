package com.rajumuke.voice;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class DictateAction extends AnAction {
    private static final Icon MIC = IconLoader.getIcon("/icons/mic.svg", DictateAction.class);
    private static final Icon MIC_RECORDING = IconLoader.getIcon("/icons/micRecording.svg", DictateAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        VoiceInput.toggle(e.getProject(), false);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        boolean on = VoiceInput.isRecording();
        e.getPresentation().setIcon(on ? MIC_RECORDING : MIC);
        e.getPresentation().setText(on ? "Mic On (listening) - click to stop and insert" : "Mic Off - click to start dictating");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
