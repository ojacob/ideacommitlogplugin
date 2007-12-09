package com.anecdote.ideaplugins.commitlog;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;

import javax.swing.*;
import java.awt.*;

public class CommitLogWindowComponent extends JPanel
  implements DataProvider {
  private class CloseAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
      _contentManager.removeContent(_content, true);
    }

    public CloseAction() {
      super("Close", "", IconLoader.getIcon("/actions/cancel.png"));
    }
  }

  private final JComponent _component;
  private final ContentManager _contentManager;
  private Content _content;
  private final boolean _addToolbar;
  private final String _helpId;

  public CommitLogWindowComponent(JComponent component, boolean addDefaultToolbar, ActionGroup toolbarActions,
                                  ContentManager contentManager, String helpId) {
    super(new BorderLayout());
    _addToolbar = addDefaultToolbar;
    _component = component;
    _contentManager = contentManager;
    _helpId = helpId;
    add(_component, BorderLayout.CENTER);
    if (_addToolbar) {
      DefaultActionGroup actionGroup = new DefaultActionGroup(null, false);
      actionGroup.add(new CloseAction());
      if (toolbarActions != null)
        actionGroup.add(toolbarActions);
//            actionGroup.add(ActionManager.getInstance().getAction("ContextHelp"));
      add(ActionManager.getInstance().createActionToolbar("CommitLogToolbar", actionGroup, false).getComponent(),
        BorderLayout.WEST);
    }
  }

  public Object getData(String dataId) {
    if ("helpId".equals(dataId))
      return _helpId;
    else
      return null;
  }

  public JComponent getComponent() {
    return _component;
  }

  public void setContent(Content content) {
    _content = content;
  }

  public JComponent getShownComponent() {
    return _addToolbar ? this : _component;
  }


}