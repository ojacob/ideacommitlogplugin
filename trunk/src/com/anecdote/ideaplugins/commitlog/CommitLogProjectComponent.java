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

import com.intellij.openapi.components.*;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.io.*;

@State(name = CommitLogProjectComponent.COMPONENT_NAME,
       storages = {@Storage(id = "COMMIT_LOG_PLUGIN", file = "$PROJECT_FILE$")})
public class CommitLogProjectComponent extends CheckinHandlerFactory
  implements ProjectComponent, Configurable, PersistentStateComponent<CommitLogProjectComponent>
{
  private ProjectLevelVcsManager _vcsManager;
  private final Project _project;
  private CommitLogWindow _commitLogWindow;
  private String _textualCommitLogTemplate;
  static final String DEFAULT_COMMIT_LOG_TEMPLATE_RESOURCE = "/resources/DefaultCommitLogTemplate.txt";
  public static final String DEFAULT_COMMIT_LOG_TEMPLATE = readResourceAsString(DEFAULT_COMMIT_LOG_TEMPLATE_RESOURCE);
  private static final String COMMIT_LOG_ICON_NAME = "/resources/commitlog.png";
  private static final Icon COMMIT_LOG_ICON = IconLoader.getIcon(COMMIT_LOG_ICON_NAME);
  public static final String COMPONENT_NAME = "CommitLogProjectComponent";
  private CommitLogConfigurationPanel _configurationPanel;
  private boolean _generateTextualCommitLog = true;
  public static final String VERSION = "1.0.2";

  public CommitLogProjectComponent()
  {
    _project = null;
  }

  public CommitLogProjectComponent(Project project)
  {
    _project = project;
  }

  public void initComponent()
  {
    _vcsManager = ProjectLevelVcsManager.getInstance(_project);
    _vcsManager.registerCheckinHandlerFactory(this);
  }

  public void disposeComponent()
  {
    _vcsManager.unregisterCheckinHandlerFactory(this);
  }

  @NotNull
  public String getComponentName()
  {
    return COMPONENT_NAME;
  }

  public void projectOpened()
  {
    // called when project is opened
  }

  public void projectClosed()
  {
    // called when project is being closed
  }

  @Override
  @NotNull
  public CheckinHandler createHandler(CheckinProjectPanel panel)
  {
    return new CommitLogCheckinHandler(this, panel);
  }

  public Project getProject()
  {
    return _project;
  }

  public CommitLogWindow getCommitLogWindow()
  {
    if (_commitLogWindow == null) {
      _commitLogWindow = new CommitLogWindow(_project);
    }
    return _commitLogWindow;
  }

  public void setTextualCommitLogTemplate(String text)
  {
    _textualCommitLogTemplate = text;
  }

  public String getTextualCommitLogTemplate()
  {
    if (_textualCommitLogTemplate == null) {
      resetCommitLogTemplate();
    }
    return _textualCommitLogTemplate;
  }

  public void resetCommitLogTemplate()
  {
    _textualCommitLogTemplate = DEFAULT_COMMIT_LOG_TEMPLATE;
  }

  public static String readResourceAsString(String resourceName)
  {
    final StringBuilder stringBuilder = new StringBuilder(500);
    final InputStream templateStream = CommitLogProjectComponent.class.getResourceAsStream(
      resourceName);
    if (templateStream == null) {
      return "Error : Could not find resource " + resourceName;
    }
    try {
      final DataInputStream dataInputStream = new DataInputStream(templateStream);
      try {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(dataInputStream));
        try {
          String line;
          //noinspection NestedAssignment
          while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append('\n');
          }
        } finally {
          bufferedReader.close();
        }
      } catch (IOException ex) {
        ex.getMessage();
      } finally {
        try {
          dataInputStream.close();
        } catch (IOException ex) {
          // ignore
        }
      }
    } finally {
      try {
        templateStream.close();
      } catch (IOException e) {
        // ignore
      }
    }
    final String string = stringBuilder.toString();
    return string;
  }

  @Nls
  public String getDisplayName()
  {
    return "Commit Log";
  }

  @Nullable
  public Icon getIcon()
  {
    return COMMIT_LOG_ICON;
  }

  @Nullable
  @NonNls
  public String getHelpTopic()
  {
    return null;
  }

  public JComponent createComponent()
  {
    if (_configurationPanel == null) {
      _configurationPanel = new CommitLogConfigurationPanel(this);
    }
    return _configurationPanel;
  }

  public boolean isModified()
  {
    return _configurationPanel.isModified();
  }

  public void apply() throws ConfigurationException
  {
    _configurationPanel.save();
  }

  public void reset()
  {
    _configurationPanel.load();
  }

  public void disposeUIResources()
  {
    _configurationPanel = null;
  }

  public CommitLogProjectComponent getState()
  {
    return this;
  }

  public void loadState(CommitLogProjectComponent state)
  {
    XmlSerializerUtil.copyBean(state, this);
  }

  public void setGenerateTextualCommitLog(boolean generateTextualCommitLog)
  {
    _generateTextualCommitLog = generateTextualCommitLog;
  }

  public boolean isGenerateTextualCommitLog()
  {
    return _generateTextualCommitLog;
  }

  @SuppressWarnings({"SSBasedInspection"})
  public static void log(String s)
  {
    System.out.println(s);
  }
}
