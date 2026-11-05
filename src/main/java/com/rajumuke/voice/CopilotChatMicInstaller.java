package com.rajumuke.voice;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import org.jetbrains.annotations.NotNull;

public class CopilotChatMicInstaller implements ToolWindowManagerListener {
    private static final String COPILOT_CHAT_ID = "GitHub Copilot Chat";

    @Override
    public void toolWindowShown(@NotNull ToolWindow toolWindow) {
        if (!COPILOT_CHAT_ID.equals(toolWindow.getId())) {
            return;
        }
        AnAction mic = ActionManager.getInstance().getAction("CopilotVoice.Dictate");
        AnAction handsFree = ActionManager.getInstance().getAction("CopilotVoice.HandsFree");
        if (mic != null && handsFree != null) {
            toolWindow.setTitleActions(java.util.List.of(handsFree, mic));
        }
    }
}
