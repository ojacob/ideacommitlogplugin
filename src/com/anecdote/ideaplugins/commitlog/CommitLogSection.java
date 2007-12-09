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
