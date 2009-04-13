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

import javax.swing.*;
import java.awt.*;

public class CommitLogConfigurationPanel extends JPanel
{
  private CommitLogProjectComponent _projectComponent;
  private CommitLogConfigurationPage _commitLogTemplatePage;
  private CommitLogConfigurationPage _commitCommentTemplatePage;
  private JTabbedPane _tabbedPane;

  public CommitLogConfigurationPanel(CommitLogProjectComponent projectComponent)
  {
    super(new BorderLayout());
    setPreferredSize(new Dimension(400, 700));
    _projectComponent = projectComponent;
    JTabbedPane tabbedPane = new JTabbedPane();
    _commitLogTemplatePage = new CommitLogConfigurationPage(new CommitLogTemplate()
    {
      public String getTemplateText()
      {
        return _projectComponent.getTextualCommitLogTemplate();
      }

      public void setTemplateText(String text)
      {
        _projectComponent.setTextualCommitLogTemplate(text);
      }

      public void reset()
      {
        _projectComponent.resetCommitLogTemplate();
      }

      public String getDefaultTemplateText()
      {
        return CommitLogProjectComponent.DEFAULT_COMMIT_LOG_TEMPLATE;
      }
    }, _projectComponent);
    _commitCommentTemplatePage = new CommitLogConfigurationPage(new CommitLogTemplate()
    {
      public String getTemplateText()
      {
        return _projectComponent.getTextualCommitCommentTemplate();
      }

      public void setTemplateText(String text)
      {
        _projectComponent.setTextualCommitCommentTemplate(text);
      }

      public void reset()
      {
        _projectComponent.resetCommitCommentTemplate();
      }

      public String getDefaultTemplateText()
      {
        return CommitLogProjectComponent.DEFAULT_COMMIT_COMMENT_TEMPLATE;
      }
    }, _projectComponent);
    _tabbedPane = tabbedPane;
    _tabbedPane.addTab("Commit Log Template", _commitLogTemplatePage);
    tabbedPane.addTab("Commit Comment Template", _commitCommentTemplatePage);
    add(tabbedPane, BorderLayout.CENTER);
    add(new JLabel("Version " + CommitLogProjectComponent.VERSION +
                   " : Copyright 2007 - 2009 Anecdote Software.  All Rights Reserved."),
        BorderLayout.SOUTH);
  }

  public boolean isModified()
  {
    return _commitCommentTemplatePage.isModified() || _commitLogTemplatePage.isModified();
  }

  public void save()
  {
    _commitCommentTemplatePage.save();
    _commitLogTemplatePage.save();
  }

  public void load()
  {
    _commitCommentTemplatePage.load();
    _commitLogTemplatePage.load();
  }


  public static void main(String[] args)
  {
    JFrame frame = new JFrame("Test Frame");
    CommitLogConfigurationPanel component = new CommitLogConfigurationPanel(null);
    frame.getContentPane().add(component, BorderLayout.CENTER);
    frame.pack();
    frame.setVisible(true);
  }

  public void setSelectedTab(int index)
  {
    _tabbedPane.setSelectedIndex(index);
  }
}
