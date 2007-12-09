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

import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

class CommitLogEntry
{
  private final File _file;
  private final FilePath _filePath;
  private final Change.Type _changeType;
  private final String _vcsRootName;
  private final String _packageName;
  private final AbstractVcs _vcs;
  private String _oldVersion;
  private String _newVersion;

  CommitLogEntry(File file, FilePath filePath, String vcsRootName, String packageName, AbstractVcs vcs,
                 Change.Type changeType)
  {
    _file = file;
    _vcsRootName = vcsRootName;
    _packageName = packageName;
    _vcs = vcs;
    _filePath = filePath;
    _changeType = changeType;
  }

  public String getVcsRootName()
  {
    return _vcsRootName;
  }

  public File getFile()
  {
    return _file;
  }

  FilePath getFilePath()
  {
    return _filePath;
  }

  AbstractVcs getVcs()
  {
    return _vcs;
  }

  String getNewVersion()
  {
    return _newVersion;
  }

  void setNewVersion(String newVersion)
  {
    _newVersion = newVersion;
  }

  String getOldVersion()
  {
    return _oldVersion;
  }

  void setOldVersion(String oldVersion)
  {
    _oldVersion = oldVersion;
  }

  Change.Type getChangeType()
  {
    return _changeType;
  }

  public String getPackageName()
  {
    return _packageName;
  }

  @NotNull
  @Override
  public String toString()
  {
    return _filePath + " : " + _oldVersion + " -> " + _newVersion;
  }

  @Override
  public boolean equals(@Nullable Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final CommitLogEntry that = (CommitLogEntry)obj;

    return _file.equals(that._file);
  }

  @Override
  public int hashCode()
  {
    return _file.hashCode();
  }
}
