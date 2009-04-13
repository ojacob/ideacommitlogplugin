/*
 * Copyright 2009 Nathan Brown
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anecdote.ideaplugins.commitlog;

import com.intellij.CommonBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.CheckinProjectPanel;

import java.util.Date;

class GenerateCommentAction extends AnAction
{
  private static final String GENERATE = "Generate";

  GenerateCommentAction()
  {
    super("Generate a comment based on the files selected", "Generates a commit comment based on the files selected",
          IconLoader.getIcon("/resources/commitlogsmall.png"));
  }

  public void update(AnActionEvent e)
  {
    super.update(e);
    CheckinProjectPanel panel = (CheckinProjectPanel)e.getData(CheckinProjectPanel.PANEL_KEY);
    if (panel == null) {
      e.getPresentation().setVisible(false);
      e.getPresentation().setEnabled(false);
    } else {
      e.getPresentation().setVisible(true);
      e.getPresentation().setEnabled(true);
    }
  }

  public void actionPerformed(AnActionEvent e)
  {
    CheckinProjectPanel panel = (CheckinProjectPanel)e.getData(CheckinProjectPanel.PANEL_KEY);
    if (panel != null) {
      final Project project = panel.getProject();
      int confirmation = Messages.showDialog(project, "Generate commit comment?         ", "Confirm Generate Comment",
                                             new String[]{GENERATE, CommonBundle.getCancelButtonText()},
                                             0, Messages.getQuestionIcon());
      if (confirmation == 0) {
        CommitLogProjectComponent projectComponent = project.getComponent(CommitLogProjectComponent.class);
        CommitLogBuilder commitLogBuilder = CommitLogBuilder.createCommitLogBuilder(
          projectComponent.getTextualCommitCommentTemplate(), panel);
        try {
          panel.setCommitMessage(commitLogBuilder.buildCommitLog(new Date()));
        } catch (CommitLogTemplateParser.TextTemplateParserException e1) {
          int result = Messages.showDialog(project, "Error parsing Comment Template :\n" +
                                                    e1.getMessage() + "\n\nEdit Comment Template now?",
                                           "Error Generating Comment",
                                           new String[]{CommonBundle.getYesButtonText(),
                                                        CommonBundle.getNoButtonText()},
                                           0, Messages.getErrorIcon());
          if (result == 0) {
            projectComponent.setFocusCommentTemplateEditor(true);
            ShowSettingsUtil.getInstance().editConfigurable(project, projectComponent);
          }
        }
      }
    }
  }
}
