/*
 * Copyright (c) 2007, Anecdote Software. All Rights Reserved.
 */

package com.anecdote.ideaplugins.commitlog;

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
