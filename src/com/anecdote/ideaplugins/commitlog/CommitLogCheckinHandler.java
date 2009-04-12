package com.anecdote.ideaplugins.commitlog;

/**
 * Copyright 2007 Nathan Brown
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.history.*;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
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
  private String _changeListName;
  private CommitLogBuilder _commitLogBuilder;
  private CommitLogCheckinHandler.AfterCheckinConfigPanel _afterCheckinConfigPanel = new AfterCheckinConfigPanel();

  CommitLogCheckinHandler(CommitLogProjectComponent projectComponent, CheckinProjectPanel panel)
  {
    _projectComponent = projectComponent;
    _project = projectComponent.getProject();
    _panel = panel;
  }

  @Override
  @Nullable
  public RefreshableOnComponent getAfterCheckinConfigurationPanel()
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
      _commitLogBuilder = new CommitLogBuilder(_projectComponent.getTextualCommitLogTemplate(),
                                               _panel.getCommitMessage());
      try {
        final List<AbstractVcs> affectedVcses = _panel.getAffectedVcses();
        final Collection<File> files = _panel.getFiles();
        for (final File file : files) {
          final FilePath filePath = file.exists() ?
                                    VcsUtil.getFilePath(file) : VcsUtil.getFilePathForDeletedFile(file.getPath(),
                                                                                                  false);
          ChangeListManager changeListManager = ChangeListManager.getInstance(_project);
          final Change change = changeListManager.getChange(filePath);
          if (_changeListName == null) {
            _changeListName = changeListManager.getChangeList(change).getName();
          }
          if (change != null) {
            final Change.Type changeType = change.getType();
            final ContentRevision beforeRevision = changeType == Change.Type.NEW ? null : change.getBeforeRevision();
            final VirtualFile vcsRoot = VcsUtil.getVcsRootFor(_project, filePath);
            final String vcsRootName = vcsRoot != null ? vcsRoot.getPresentableName() : "";
            for (final AbstractVcs affectedVcs : affectedVcses) {
              if (affectedVcs.fileIsUnderVcs(filePath)) {
                String packageName = getPackageName(filePath);
                String pathFromRoot = getPathFromRoot(vcsRoot, filePath);
                final CommitLogEntry commitLogEntry = new CommitLogEntry(file, filePath, vcsRootName, pathFromRoot,
                                                                         packageName, affectedVcs,
                                                                         changeType);
                _commitLogBuilder.addCommitLogEntry(commitLogEntry);
                if (beforeRevision != null) {
                  commitLogEntry.setOldVersion(beforeRevision.getRevisionNumber().asString());
                }
                break;
              }
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace(); // protect IDE
      }
    }
    return returnResult;
  }

  private static String getPathFromRoot(VirtualFile vcsRoot, FilePath filePath)
  {
    String pathFromRoot = null;
    FilePath path = filePath.getParentPath();
    while (path != null && !path.getVirtualFile().equals(vcsRoot)) {
      String name = path.getName();
      pathFromRoot = pathFromRoot != null ? name + '/' + pathFromRoot : name;
      path = path.getParentPath();
    }
    return pathFromRoot;
  }

  @Override
  public void checkinFailed(List<VcsException> exception)
  {
    CommitLogProjectComponent.log("CommitLogCheckinHandler::checkinFailed() Entered");
    try {
      super.checkinFailed(exception);
      if (_projectComponent.isGenerateTextualCommitLog()) {
        outputCommitLog(true);
        _changeListName = null;
      }
    } catch (Exception e) {
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
    } catch (Exception e) {
      e.printStackTrace(); // protect ide
    }
    _changeListName = null;
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
    final String changeListName = _changeListName;
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
      _commitLogBuilder.getCommitLogEntriesByType(null).entrySet();
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

  private String getPackageName(FilePath filePath)
  {
    String text;
    final VirtualFile parent = filePath.getVirtualFileParent();
    final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(_project).getFileIndex();
    if (parent != null) {
      text = projectFileIndex.getPackageNameByDirectory(parent);
    } else {
      text = "";
    }
    return text;
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