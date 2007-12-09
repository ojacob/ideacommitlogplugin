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

import com.intellij.openapi.application.*;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.FilePathImpl;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Date;

public class CommitLogConfigurationPanel extends JPanel
{
  private final CommitLogProjectComponent _projectComponent;
  private boolean _modified;
  private Editor _templateEditor;
  private Document _templateDocument;
  private Editor _sampleEditor;
  private Document _sampleDocument;
  private DocumentListener _templateDocumentListener = new DocumentListener()
  {
    public void beforeDocumentChange(DocumentEvent event)
    {
    }

    public void documentChanged(DocumentEvent event)
    {
      templateDocumentChanged();
    }
  };

  private static final String COMMIT_LOG_TEMPLATE_REFERENCE_RESOURCE = "/resources/CommitLogTemplateReference.txt";
  private static final FileType TEMPLATE_FILE_TYPE = FileTypeManager.getInstance().getFileTypeByExtension("txt");
  private static final String SAMPLE_COMMIT_MESSAGE = "This is a sample commit message." +
                                                    "  I hope you usually write more " +
                                                    "than this for your commits ;^)";
  private AbstractAction _resetTemplateAction = new AbstractAction(null, IconLoader.getIcon(
    "/actions/undo.png"))
  {
    public void actionPerformed(ActionEvent e)
    {
      DialogWrapper dialogWrapper = new DialogWrapper(CommitLogConfigurationPanel.this, false)
      {
        {
          setOKButtonText("Reset the Template");
          setCancelButtonText("Keep the Current Template");
          init();
        }

        @Override
        @Nullable
        protected JComponent createCenterPanel()
        {
          JPanel jpanel = new JPanel(new BorderLayout());
          String s = "This will reset the Commit Log Template to the default template.  Continue?";
          JLabel jlabel = new JLabel(s);
          jlabel.setIconTextGap(10);
          jlabel.setIcon(Messages.getQuestionIcon());
          jpanel.add(jlabel, "Center");
          jpanel.add(Box.createVerticalStrut(10), "South");
          return jpanel;
        }
      };
      dialogWrapper.setModal(true);
      dialogWrapper.setTitle("Confirm Template Reset");
      dialogWrapper.pack();
      dialogWrapper.centerRelativeToParent();
      dialogWrapper.show();
      if (dialogWrapper.isOK()) {
        _projectComponent.resetCommitLogTemplate();
        load();
      }
    }
  };

  public CommitLogConfigurationPanel(CommitLogProjectComponent projectComponent)
  {
    super(new BorderLayout());

    JTabbedPane tabbedPane = new JTabbedPane();
    add(tabbedPane, BorderLayout.CENTER);
    
    _projectComponent = projectComponent;
    EditorFactory editorFactory = EditorFactory.getInstance();
    _templateDocument = editorFactory.createDocument(projectComponent.getTextualCommitLogTemplate());
    _templateDocument.addDocumentListener(_templateDocumentListener);
    _templateEditor = editorFactory.createEditor(_templateDocument, projectComponent.getProject(), TEMPLATE_FILE_TYPE,
                                                 false);
    initEditor(_templateEditor);

    JToolBar templateEditorToolBar = new JToolBar();
    templateEditorToolBar.setFloatable(false);
    templateEditorToolBar.setBorderPainted(false);
    final JButton resetButton = templateEditorToolBar.add(_resetTemplateAction);
    resetButton.setText("Reset To Default");
    resetButton.setHorizontalTextPosition(JButton.RIGHT);

    JPanel templateEditorPanel = new JPanel(new BorderLayout());
    JComponent comp = _templateEditor.getComponent();
    templateEditorPanel.add(comp, BorderLayout.CENTER);
    templateEditorPanel.add(templateEditorToolBar, BorderLayout.NORTH);

    _sampleDocument = editorFactory.createDocument(projectComponent.getTextualCommitLogTemplate());
    _sampleEditor = editorFactory.createViewer(_sampleDocument, projectComponent.getProject());
    initEditor(_sampleEditor);
    JPanel sampleEditorPanel = new JPanel(new BorderLayout());
    comp = _sampleEditor.getComponent();
    sampleEditorPanel.add(comp, BorderLayout.CENTER);

    JTextArea referenceEditor = new JTextArea(CommitLogProjectComponent.readResourceAsString(
      COMMIT_LOG_TEMPLATE_REFERENCE_RESOURCE));
    referenceEditor.setLineWrap(true);
    referenceEditor.setWrapStyleWord(true);
    JScrollPane referenceEditorPane = new JScrollPane(referenceEditor, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    referenceEditorPane.setPreferredSize(new Dimension(512, 384));

    JTabbedPane templateTabbedPane = new JTabbedPane();
    templateTabbedPane.add("Sample Commit Log", sampleEditorPanel);
    templateTabbedPane.add("Commit Log Template Reference Documentation", referenceEditorPane);

    updateSampleDocument();

    JSplitPane commitLogConfigPage = new JSplitPane(JSplitPane.VERTICAL_SPLIT, templateEditorPanel, templateTabbedPane);
    commitLogConfigPage.setDividerLocation(0.5D);
    commitLogConfigPage.setResizeWeight(0.5D);
    commitLogConfigPage.setSize(commitLogConfigPage.getPreferredSize());
    commitLogConfigPage.validate();
    commitLogConfigPage.setBorder(new EmptyBorder(5, 5, 5, 5));
    tabbedPane.add("Commit Log Template", commitLogConfigPage);

  }

  private void templateDocumentChanged()
  {
    _modified = true;
    updateSampleDocument();
    _resetTemplateAction.setEnabled(!_templateDocument.getText().equals(CommitLogProjectComponent.DEFAULT_COMMIT_LOG_TEMPLATE));
  }

  private void updateSampleDocument()
  {
    final String template = _templateDocument.getText();
    try {
      CommitLogBuilder sampleCommitLogBuilder =
        new CommitLogBuilder(template, SAMPLE_COMMIT_MESSAGE);
      addSampleCommitLogEntry(sampleCommitLogBuilder, "ModifiedClass1", Change.Type.MODIFICATION, "MyVCSModule");
      addSampleCommitLogEntry(sampleCommitLogBuilder, "ModifiedClass2", Change.Type.MODIFICATION, "MyVCSModule");
      addSampleCommitLogEntry(sampleCommitLogBuilder, "ObsoleteClass", Change.Type.DELETED, "MyVCSModule");
      addSampleCommitLogEntry(sampleCommitLogBuilder, "NewClass", Change.Type.NEW, "MyVCSModule");
      addSampleCommitLogEntry(sampleCommitLogBuilder, "ModifiedClass1", Change.Type.MODIFICATION, "AnotherVCSModule");
      addSampleCommitLogEntry(sampleCommitLogBuilder, "ModifiedClass2", Change.Type.MODIFICATION, "AnotherVCSModule");
      final String sample = sampleCommitLogBuilder.buildCommitLog(new Date());
      ApplicationManager.getApplication().runWriteAction(new Runnable()
      {
        public void run()
        {
          _sampleDocument.setText(sample);
        }
      });
    } catch (final CommitLogTemplateParser.TextTemplateParserException e) {
      ApplicationManager.getApplication().runWriteAction(new Runnable()
      {
        public void run()
        {
          _sampleDocument.setText(template.substring(0, e.getLocation() + 1) +"<<<ERROR\n" + e.getMessage());
        }
      });
    }
  }

  private static void addSampleCommitLogEntry(CommitLogBuilder sampleCommitLogBuilder, String className,
                                        Change.Type changeType, String vcsRootName)
  {
    final File file = new File("c:/sandbox/" + vcsRootName + "/commitlog/samplecommit/" + className + ".java");
    final CommitLogEntry logEntry = new CommitLogEntry(file, new FilePathImpl(file, false), vcsRootName,
                                                       "commitlog.samplecommit", null,
                                                       changeType);
    if (changeType == Change.Type.NEW) {
      logEntry.setNewVersion("1.0");
    } else {
      logEntry.setOldVersion("1.2.3.4");
      if (changeType != Change.Type.DELETED)
        logEntry.setNewVersion("1.2.3.5");
    }
    sampleCommitLogBuilder.addCommitLogEntry(logEntry);
  }

  private static void initEditor(Editor editor)
  {
    EditorSettings editorsettings = editor.getSettings();
    editorsettings.setFoldingOutlineShown(false);
    editorsettings.setLineMarkerAreaShown(false);
    editorsettings.setLineNumbersShown(false);
    editorsettings.setRightMarginShown(false);
    editor.getComponent().setPreferredSize(new Dimension(512, 384));
  }

  public void dispose()
  {
    _templateDocument.removeDocumentListener(_templateDocumentListener);
    EditorFactory.getInstance().releaseEditor(_templateEditor);
    EditorFactory.getInstance().releaseEditor(_sampleEditor);
  }

  public boolean isModified()
  {
    return _modified;
  }

  public void save()
  {
    _projectComponent.setTextualCommitLogTemplate(_templateDocument.getText());
    _modified = false;
  }

  public void load()
  {
    ApplicationManager.getApplication().runWriteAction(new Runnable()
    {
      public void run()
      {
        _templateDocument.setText(_projectComponent.getTextualCommitLogTemplate());
        _modified = false;
      }
    });
  }
}
