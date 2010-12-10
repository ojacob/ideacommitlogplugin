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
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.*;

import javax.swing.*;
import java.io.File;
import java.util.*;

class GenerateCommentAction extends AnAction
{
  private static final String GENERATE = "Generate";
  private static final DataKey<JTextArea> CHANGES_BAR_COMMENT_EDITOR_DATA_KEY = DataKey.create(
    "CHANGES_BAR_COMMENT_EDITOR");
  private static final DataKey<LocalChangeList> SELECTED_CHANGE_LIST_DATA_KEY = DataKey.create("SELECTED_CHANGE_LIST");
  private static final String COMMIT_DIALOG_TEXT = "Generate a comment based on the files selected";
  private static final String COMMIT_DIALOG_DESC = "Generates a commit comment based on the files selected";
  private static final String CHANGES_BAR_TEXT = "Generate a comment using CommitLog plugin";
  private static final String CHANGES_BAR_DESC = "Generates a comment for the selected Changelist using CommitLog plugin";

  GenerateCommentAction()
  {
    super(COMMIT_DIALOG_TEXT, COMMIT_DIALOG_DESC,
          IconLoader.getIcon("/resources/generate.png"));
  }

  public void update(AnActionEvent e)
  {
    super.update(e);
    CheckinProjectPanel panel = (CheckinProjectPanel)e.getData(CheckinProjectPanel.PANEL_KEY);
    e.getPresentation().setVisible(true);
    e.getPresentation().setEnabled(true);
    e.getPresentation().setText(COMMIT_DIALOG_TEXT);
    e.getPresentation().setDescription(COMMIT_DIALOG_DESC);
    if (panel == null) {
      if ("CHANGES_BAR_COMMIT_COMMENT_TOOLBAR".equals(e.getPlace())) {
        if (e.getData(SELECTED_CHANGE_LIST_DATA_KEY) == null) {
          e.getPresentation().setEnabled(false);
        }
        e.getPresentation().setText(CHANGES_BAR_TEXT);
        e.getPresentation().setDescription(CHANGES_BAR_DESC);
      } else {
        e.getPresentation().setVisible(false);
        e.getPresentation().setEnabled(false);
      }
    }
  }

  public void actionPerformed(AnActionEvent e)
  {
    CheckinProjectPanel panel = (CheckinProjectPanel)e.getData(CheckinProjectPanel.PANEL_KEY);
    JTextArea commentEditor = e.getData(CHANGES_BAR_COMMENT_EDITOR_DATA_KEY);
    if (panel != null || commentEditor != null) {
      final Project project = panel != null ? panel.getProject() : DataKeys.PROJECT.getData(e.getDataContext());
      if (project != null) {
        int confirmation = Messages.showDialog(project, "Generate commit comment?         ", "Confirm Generate Comment",
                                               new String[]{GENERATE, CommonBundle.getCancelButtonText()},
                                               0, Messages.getQuestionIcon());
        if (confirmation == 0) {
          CommitLogProjectComponent projectComponent = project.getComponent(CommitLogProjectComponent.class);
          String commitMessage;
          Collection<File> files;
          if (panel != null) {
            commitMessage = panel.getCommitMessage();
            files = panel.getFiles();
          } else {
            commitMessage = commentEditor.getText();
            LocalChangeList changeList = e.getData(SELECTED_CHANGE_LIST_DATA_KEY);
            if (changeList == null) {
              return;
            }
            ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
            files = new HashSet<File>();
            for (Change change : changeList.getChanges()) {
              FilePath filepath = ChangesUtil.getFilePath(change);
              files.add(filepath.getIOFile());
            }
          }
          CommitLogBuilder commitLogBuilder = CommitLogBuilder.createCommitLogBuilder(
            projectComponent.getTextualCommitCommentTemplate(), commitMessage, project, files);
          try {
            String commitLog = commitLogBuilder.buildCommitLog(new Date());
            if (panel != null) {
              panel.setCommitMessage(commitLog);
            } else {
              commentEditor.setText(commitLog);
              commentEditor.requestFocusInWindow();
            }
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
}
