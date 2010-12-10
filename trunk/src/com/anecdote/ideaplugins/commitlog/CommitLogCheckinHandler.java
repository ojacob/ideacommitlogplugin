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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.history.*;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.util.*;
import java.util.List;

@SuppressWarnings(
  {"AssignmentToForLoopParameter", "CallToPrintStackTrace", "SSBasedInspection", "CallToPrintStackTrace",
   "CatchGenericClass"})
class CommitLogCheckinHandler extends CheckinHandler
{
  private final CommitLogProjectComponent _projectComponent;
  private final Project _project;
  private final CheckinProjectPanel _panel;
  private CommitLogBuilder _commitLogBuilder;
  private CommitLogCheckinHandler.AfterCheckinConfigPanel _afterCheckinConfigPanel = new AfterCheckinConfigPanel();

  CommitLogCheckinHandler(CommitLogProjectComponent projectComponent, CheckinProjectPanel panel)
  {
    _projectComponent = projectComponent;
    _project = projectComponent.getProject();
    _panel = panel;
  }

  @Override
  public RefreshableOnComponent getAfterCheckinConfigurationPanel(Disposable parentDisposable)
  {
    return _afterCheckinConfigPanel;
  }

  @SuppressWarnings({"GetterCallInLoop"})
  @Override
  public ReturnResult beforeCheckin()
  {
    CommitLogProjectComponent.log("CommitLogCheckinHandler::beforeCheckin Entered");
    final ReturnResult returnResult = super.beforeCheckin();
    if (_projectComponent.isGenerateTextualCommitLog()) {
      try {
        List<AbstractVcs> affectedVcses = new ArrayList<AbstractVcs>();
        _commitLogBuilder = CommitLogBuilder.createCommitLogBuilder(_projectComponent.getTextualCommitLogTemplate(),
                                                                    _panel.getCommitMessage(), _panel.getProject(),
                                                                    _panel.getFiles());
      } catch (Throwable e) {
        e.printStackTrace(); // protect IDE
      }
    }
    return returnResult;
  }

  @Override
  public void checkinFailed(List<VcsException> exception)
  {
    CommitLogProjectComponent.log("CommitLogCheckinHandler::checkinFailed() Entered");
    try {
      super.checkinFailed(exception);
      if (_projectComponent.isGenerateTextualCommitLog()) {
        outputCommitLog(true);
        _commitLogBuilder = null;
      }
    } catch (Throwable e) {
      // protect IDE
      e.printStackTrace();
    }
  }

  @Override
  public void checkinSuccessful()
  {
    CommitLogProjectComponent.log("CommitLogCheckinHandler::checkinSuccessful() Entered");
    try {
      if (_projectComponent.isGenerateTextualCommitLog()) {
        super.checkinSuccessful();
        outputCommitLog(false);
      }
    } catch (Throwable e) {
      e.printStackTrace(); // protect ide
    }
    _commitLogBuilder = null;
  }

  private void outputCommitLog(final boolean failed)
  {
    CommitLogProjectComponent.log("CommitLogCheckinHandler::outputCommitLog() Entered");
    CommitLogProjectComponent.log("CommitLogCheckinHandler.outputCommitLog : failed = " + failed);
//    final ChangeListManager changeListManager = ChangeListManager.getInstance(_project);
//    Runnable runnable = new Runnable()
//    {
//      public void run()
//      {
//        changeListManager.ensureUpToDate(false);
//      }
//    };
//    if (EventQueue.isDispatchThread()) {
//      runnable.run();
//    }
//    else {
//      try {
//        SwingUtilities.invokeAndWait(runnable);
//      } catch (InterruptedException e) {
//        Thread.currentThread().interrupt();
//      } catch (InvocationTargetException e) {
//        // todo : handle
//        e.printStackTrace();
//      }
//    }
    updateEntryVersions();
    _commitLogBuilder.removeUncommittedEntries();
    final Date date = new Date();
    String commitLog;
    try {
      commitLog = _commitLogBuilder.buildCommitLog(date);
    } catch (CommitLogTemplateParser.TextTemplateParserException e) {
      commitLog = e.getMessage();
    }
    final String changeListName = _commitLogBuilder.getChangeListName();
//      CommitLogProjectComponent.log(commitLog);
    final String finalCommitLog = commitLog;
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        CommitLogProjectComponent.log("CommitLogCheckinHandler::outputCommitLog Runnable.run() Entered");
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument(finalCommitLog);
        Editor viewer = editorFactory.createViewer(document, _project);
        EditorSettings editorsettings = viewer.getSettings();
        editorsettings.setFoldingOutlineShown(false);
        editorsettings.setLineMarkerAreaShown(false);
        editorsettings.setLineNumbersShown(false);
        editorsettings.setRightMarginShown(false);
        String tabTitle = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(date) + " : " +
                          changeListName;
        if (failed) {
          tabTitle += " [FAILED]";
        }
        CommitLogWindow window = _projectComponent.getCommitLogWindow();
        window.addCommitLog(tabTitle, viewer);
        window.ensureVisible(_project);
      }
    });
  }

  private void updateEntryVersions()
  {
    final Set<Map.Entry<Change.Type, Collection<CommitLogEntry>>> entries =
      _commitLogBuilder.getCommitLogEntriesByTypeByRoot(null).entrySet();
    for (final Map.Entry<Change.Type, Collection<CommitLogEntry>> mapEntry : entries) {
      for (final CommitLogEntry commitLogEntry : mapEntry.getValue()) {
        try {
          String version = getCurrentFileVersion(commitLogEntry.getVcs(), commitLogEntry.getFilePath());
          if (version == null) {
            version = commitLogEntry.getOldVersion();
          }
          commitLogEntry.setNewVersion(version);
        } catch (VcsException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Nullable
  private static String getCurrentFileVersion(@NotNull AbstractVcs vcs, FilePath filePath)
    throws VcsException
  {
    String version = null;
    final DiffProvider diffProvider = vcs.getDiffProvider();
    final VirtualFile file = filePath.getVirtualFile();
    if (diffProvider != null && file != null) {
      final VcsRevisionNumber revision = diffProvider.getCurrentRevision(file);
      if (revision != null) {
        version = revision.asString();
      }
    } else {
      // deleted or diff provider not supported - use alternate method but lookup will be slower.
      final VcsHistoryProvider historyProvider = vcs.getVcsHistoryProvider();
      if (historyProvider != null) {
        final VcsHistorySession session = historyProvider.createSessionFor(filePath);
        if (session != null) {
          if (!session.getRevisionList().isEmpty()) {
            version = session.getCurrentRevisionNumber().asString();
          }
        }
      }
    }
    return version;
  }

  private class AfterCheckinConfigPanel implements RefreshableOnComponent
  {
    private JCheckBox _generateCommitLog = new JCheckBox("Generate Commit Log");
    private JPanel _configPanel = new JPanel(new BorderLayout());

    private AfterCheckinConfigPanel()
    {
      _configPanel.add(_generateCommitLog, BorderLayout.WEST);
      _configPanel.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
    }

    public JComponent getComponent()
    {
      return _configPanel;
    }

    public void refresh()
    {
    }

    public void saveState()
    {
      _projectComponent.setGenerateTextualCommitLog(_generateCommitLog.isSelected());
    }

    public void restoreState()
    {
      _generateCommitLog.setSelected(_projectComponent.isGenerateTextualCommitLog());
    }
  }
}