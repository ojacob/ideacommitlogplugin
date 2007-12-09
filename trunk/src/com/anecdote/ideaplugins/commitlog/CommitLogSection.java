/*
 * Copyright (c) 2007, Anecdote Software. All Rights Reserved.
 */

package com.anecdote.ideaplugins.commitlog;

public class CommitLogSection
{
  private final String _text;
  private final int _usedNodes;

  public CommitLogSection(String text, int usedNodes)
  {
    _text = text;
    _usedNodes = usedNodes;
  }

  public int getUsedNodes()
  {
    return _usedNodes;
  }

  public String getText()
  {
    return _text;
  }
}
