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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.util.*;

@SuppressWarnings(
  {"AssignmentToForLoopParameter", "CallToPrintStackTrace", "SSBasedInspection", "CallToPrintStackTrace",
   "StringContatenationInLoop"})
class CommitLogBuilder
{
  private static final String TIME_PLACEHOLDER = "TIME";
  private static final String DATE_PLACEHOLDER = "DATE";
  private static final String DATE_TIME_PLACEHOLDER = "DATE_TIME";
  private static final String FILE_COUNT_PLACEHOLDER = "FILE_COUNT";
  private static final String ROOT_COUNT_PLACEHOLDER = "ROOT_COUNT";
  private static final String ROOT_LIST_PLACEHOLDER = "ROOT_LIST";
  private static final String COMMIT_MESSAGE_PLACEHOLDER = "COMMIT_MESSAGE";

  private static final String ROOTS_SECTION_START_PLACEHOLDER = "ROOTS_SECTION";
  private static final String ROOTS_SECTION_END_PLACEHOLDER = "/ROOTS_SECTION";
  private static final String ROOT_ENTRY_START_PLACEHOLDER = "ROOT_ENTRY";
  private static final String ROOT_ENTRY_END_PLACEHOLDER = "/ROOT_ENTRY";

  private static final String MODIFIED_FILES_SECTION_START_PLACEHOLDER = "MODIFIED_FILES";
  private static final String ADDED_FILES_SECTION_START_PLACEHOLDER = "ADDED_FILES";
  private static final String DELETED_FILES_SECTION_START_PLACEHOLDER = "DELETED_FILES";
  private static final String ALL_FILES_SECTION_START_PLACEHOLDER = "ALL_FILES";
  private static final String MODIFIED_FILES_SECTION_END_PLACEHOLDER = "/MODIFIED_FILES";
  private static final String ADDED_FILES_SECTION_END_PLACEHOLDER = "/ADDED_FILES";
  private static final String DELETED_FILES_SECTION_END_PLACEHOLDER = "/DELETED_FILES";
  private static final String ALL_FILES_SECTION_END_PLACEHOLDER = "/ALL_FILES";

  private static final String FILE_ENTRY_START_PLACEHOLDER = "FILE_ENTRY";
  private static final String FILE_ENTRY_END_PLACEHOLDER = "/FILE_ENTRY";
  private static final String FILE_NAME_PLACEHOLDER = "FILE_NAME";
  private static final String FILE_PATH_PLACEHOLDER = "FILE_PATH";
  private static final String FILE_ACTION_PLACEHOLDER = "FILE_ACTION";
  private static final String ROOT_NAME_PLACEHOLDER = "ROOT_NAME";
  private static final String PACKAGE_NAME_PLACEHOLDER = "PACKAGE_NAME";
  private static final String PACKAGE_PATH_PLACEHOLDER = "PACKAGE_PATH";
  private static final String PATH_FROM_ROOT_PLACEHOLDER = "PATH_FROM_ROOT";
  private static final String OLD_REVISION_NUMBER_PLACEHOLDER = "OLD_REVISION_NUMBER";
  private static final String NEW_REVISION_NUMBER_PLACEHOLDER = "NEW_REVISION_NUMBER";

  private int _fileCount;
  private final Map<String, Map<Change.Type, Collection<CommitLogEntry>>> _commitLogEntriesByRoot =
    new TreeMap<String, Map<Change.Type, Collection<CommitLogEntry>>>();
  private Map<Change.Type, Collection<CommitLogEntry>> _commitLogEntriesByType =
    new EnumMap<Change.Type, Collection<CommitLogEntry>>(Change.Type.class);
  private String _commitMessage;
  private final String _commitLogTemplate;

  CommitLogBuilder(String commitLogTemplate, String commitMessage)
  {
    _commitLogTemplate = commitLogTemplate;
    _commitMessage = commitMessage;
  }

  public void addCommitLogEntry(CommitLogEntry commitLogEntry)
  {
    getCommitLogEntries(commitLogEntry.getVcsRootName(), commitLogEntry.getChangeType()).add(commitLogEntry);
    getCommitLogEntries(null, commitLogEntry.getChangeType()).add(commitLogEntry);
    _fileCount++;
  }

  public void removeUncommittedEntries()
  {
    removeUncommittedEntriesByRoot();
    removeUncommittedEntriesByType(_commitLogEntriesByType);
  }

  private void removeUncommittedEntriesByRoot()
  {
    for (Iterator<Map.Entry<String, Map<Change.Type, Collection<CommitLogEntry>>>> entriesByRootIterator =
      _commitLogEntriesByRoot.entrySet().iterator(); entriesByRootIterator.hasNext();) {
      Map.Entry<String, Map<Change.Type, Collection<CommitLogEntry>>> rootEntry = entriesByRootIterator.next();
      Map<Change.Type, Collection<CommitLogEntry>> entriesForRootByType = rootEntry.getValue();
      _fileCount -= removeUncommittedEntriesByType(entriesForRootByType);
      if (entriesForRootByType.isEmpty()) {
        entriesByRootIterator.remove();
      }
    }
  }

  private static int removeUncommittedEntriesByType(Map<Change.Type, Collection<CommitLogEntry>> entriesForRootByType)
  {
    int result = 0;
    for (Iterator<Map.Entry<Change.Type, Collection<CommitLogEntry>>> entriesByTypeIterator = entriesForRootByType
      .entrySet().iterator(); entriesByTypeIterator.hasNext();) {
      Map.Entry<Change.Type, Collection<CommitLogEntry>> entry = entriesByTypeIterator.next();
      Collection<CommitLogEntry> commitLogEntries = entry.getValue();
      for (Iterator<CommitLogEntry> commitLogEntryIterator = commitLogEntries.iterator();
           commitLogEntryIterator.hasNext();) {
        CommitLogEntry commitLogEntry = commitLogEntryIterator.next();
        if (commitLogEntry.getOldVersion() == null ?
            commitLogEntry.getNewVersion() == null :
            commitLogEntry.getOldVersion().equals(commitLogEntry.getNewVersion())) {
          CommitLogProjectComponent.log(
            "Removing Commit log entry for " + commitLogEntry.getFilePath() + " : file not committed");
          commitLogEntryIterator.remove();
          result++;
        }

//        if (changeListManager.getChange(commitLogEntry.getFilePath()) != null) {
//          // still exists, remove this as it wasn't done
//          commitLogEntryIterator.remove();
//          result++;
//        }
      }
      if (commitLogEntries.isEmpty()) {
        entriesByTypeIterator.remove();
      }
    }
    return result;
  }

  private Collection<CommitLogEntry> getCommitLogEntries(String root, Change.Type type)
  {
    Map<Change.Type, Collection<CommitLogEntry>> byRoot = getCommitLogEntriesByType(root);
    Collection<CommitLogEntry> result = byRoot.get(type);
    if (result == null) {
      result = new HashSet<CommitLogEntry>();
      byRoot.put(type, result);
    }
    return result;
  }

  protected Map<Change.Type, Collection<CommitLogEntry>> getCommitLogEntriesByType(String root)
  {
    Map<Change.Type, Collection<CommitLogEntry>> byRoot = root != null ? _commitLogEntriesByRoot.get(root)
                                                                       : _commitLogEntriesByType;
    if (byRoot == null) {
      byRoot = new EnumMap<Change.Type, Collection<CommitLogEntry>>(Change.Type.class);
      _commitLogEntriesByRoot.put(root, byRoot);
    }
    return byRoot;
  }

  @SuppressWarnings({"AssignmentToForLoopParameter"})
  protected String buildCommitLog(Date date) throws CommitLogTemplateParser.TextTemplateParserException
  {
    CommitLogProjectComponent.log("CommitLogBuilder::buildCommitLog() Entered");
    final String templateText = _commitLogTemplate;
    final CommitLogTemplateParser parser = new CommitLogTemplateParser();
    final List<CommitLogTemplateParser.TextTemplateNode> textTemplateNodes = parser.parseTextTemplate(templateText);
    if (textTemplateNodes.isEmpty()) {
      CommitLogProjectComponent.log("ERROR : Parsed template is empty!");
    }
    final StringBuilder result = new StringBuilder(500);
    for (int i = 0; i < textTemplateNodes.size(); i++) {
      final CommitLogTemplateParser.TextTemplateNode textTemplateNode = textTemplateNodes.get(i);
      String text = textTemplateNode.getText();
      if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.BLOCK_PLACEHOLDER_NODE) {
        if (text.equals(ROOTS_SECTION_START_PLACEHOLDER)) {
          i++;
          CommitLogSection logSection;
          final List<CommitLogTemplateParser.TextTemplateNode> followingNodes = getFollowingNodes(textTemplateNodes, i);
          logSection = buildCommitLogRootsSection(followingNodes, new Date());
          i += logSection.getUsedNodes() - 1;
          text = logSection.getText();
        } else if (isFileSectionStartPlaceholder(text)) {
          i++;
          final List<CommitLogTemplateParser.TextTemplateNode> followingNodes = getFollowingNodes(textTemplateNodes, i);
          CommitLogSection logSection = buildCommitLogFilesSection(followingNodes, null, text);
          i += logSection.getUsedNodes() - 1;
          text = logSection.getText();
        }
      } else if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.VALUE_PLACEHOLDER_NODE) {
        text = processCommonPlaceholders(text, date);
      }
      result.append(text);
    }
    return result.toString();
  }

  private static boolean isFilesSectionEndPlaceholder(String text)
  {
    return DELETED_FILES_SECTION_END_PLACEHOLDER.equals(text) || MODIFIED_FILES_SECTION_END_PLACEHOLDER.equals(
      text) || ADDED_FILES_SECTION_END_PLACEHOLDER.equals(text) || ALL_FILES_SECTION_END_PLACEHOLDER.equals(
      text);
  }

  private static boolean isFileSectionStartPlaceholder(String text)
  {
    return DELETED_FILES_SECTION_START_PLACEHOLDER.equals(text) || MODIFIED_FILES_SECTION_START_PLACEHOLDER.equals(
      text) || ADDED_FILES_SECTION_START_PLACEHOLDER.equals(text) || ALL_FILES_SECTION_START_PLACEHOLDER.equals(
      text);
  }

  private String processCommonPlaceholders(String nodeText, Date date)
  {
    if (nodeText.equals(TIME_PLACEHOLDER)) {
      nodeText = DateFormat.getTimeInstance().format(date);
    } else if (nodeText.equals(DATE_PLACEHOLDER)) {
      nodeText = DateFormat.getDateInstance().format(date);
    } else if (nodeText.equals(DATE_TIME_PLACEHOLDER)) {
      nodeText = DateFormat.getDateTimeInstance().format(date);
    } else if (nodeText.equals(FILE_COUNT_PLACEHOLDER)) {
      nodeText = String.valueOf(_fileCount);
    } else if (nodeText.equals(ROOT_COUNT_PLACEHOLDER)) {
      nodeText = String.valueOf(_commitLogEntriesByRoot.size());
    } else if (nodeText.equals(ROOT_LIST_PLACEHOLDER)) {
      nodeText = toString(_commitLogEntriesByRoot.keySet());
    } else if (nodeText.equals(COMMIT_MESSAGE_PLACEHOLDER)) {
      nodeText = _commitMessage;
    } else {
      nodeText = "Illegal Placeholder : " + CommitLogTemplateParser.VALUE_PLACEHOLDER_SYMBOL + nodeText +
                 CommitLogTemplateParser.VALUE_PLACEHOLDER_SYMBOL;
    }
    return nodeText;
  }

  private CommitLogSection buildCommitLogFilesSection(List<CommitLogTemplateParser.TextTemplateNode> nodes,
                                                      @Nullable String rootName, String sectionPlaceholder
  )
  {
    CommitLogSection logSection;
    if (sectionPlaceholder.equals(ALL_FILES_SECTION_START_PLACEHOLDER)) {
      logSection = buildCommitLogFilesSection(nodes, rootName);
    } else {
      Change.Type type = null;
      if (sectionPlaceholder.equals(ADDED_FILES_SECTION_START_PLACEHOLDER)) {
        type = Change.Type.NEW;
      } else if (sectionPlaceholder.equals(DELETED_FILES_SECTION_START_PLACEHOLDER)) {
        type = Change.Type.DELETED;
      } else if (sectionPlaceholder.equals(MODIFIED_FILES_SECTION_START_PLACEHOLDER)) {
        type = Change.Type.MODIFICATION;
      }
      logSection = type != null ? buildCommitLogFilesSection(nodes, rootName, type) :
                   new CommitLogSection("Unknown placeholder in template : $" + sectionPlaceholder + '$', 1);
    }
    return logSection;
  }

  private CommitLogSection buildCommitLogRootsSection(List<CommitLogTemplateParser.TextTemplateNode> nodes, Date date)
  {
    int usedNodes = 0;
    final StringBuilder result = new StringBuilder(500);
    for (int i = 0; i < nodes.size(); i++) {
      final CommitLogTemplateParser.TextTemplateNode textTemplateNode = nodes.get(i);
      String text = textTemplateNode.getText();
      if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.BLOCK_PLACEHOLDER_NODE) {
        String nodeText = text;
        if (nodeText.equals(ROOT_ENTRY_START_PLACEHOLDER)) {
          i++;
          final CommitLogSection logSection = buildCommitLogRootEntries(getFollowingNodes(nodes, i), date);
          text = logSection.getText();
          i += logSection.getUsedNodes() - 1;
        } else if (text.equals(ROOTS_SECTION_END_PLACEHOLDER)) {
          usedNodes = i + 1;
          break;
        }
      } else if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.VALUE_PLACEHOLDER_NODE) {
        text = processCommonPlaceholders(text, date);
      }
      //noinspection GetterCallInLoop
      if (!_commitLogEntriesByRoot.isEmpty()) {
        result.append(text);
      }
    }
    return new CommitLogSection(result.toString(), usedNodes);
  }

  private CommitLogSection buildCommitLogRootEntries(List<CommitLogTemplateParser.TextTemplateNode> nodes, Date date)
  {
    final StringBuilder result = new StringBuilder(500);
    int usedNodes = 0;
    if (_commitLogEntriesByRoot.isEmpty()) {
      usedNodes = appendCommitLogRootEntries(result, nodes, date, null, null);
      return new CommitLogSection("", usedNodes);
    } else {
      for (Map.Entry<String, Map<Change.Type, Collection<CommitLogEntry>>> entry : _commitLogEntriesByRoot.entrySet()) {
        String rootName = entry.getKey();
        Map<Change.Type, Collection<CommitLogEntry>> logEntriesByType = entry.getValue();
        usedNodes = appendCommitLogRootEntries(result, nodes, date, rootName, logEntriesByType);
      }
      return new CommitLogSection(result.toString(), usedNodes);
    }
  }

  private int appendCommitLogRootEntries(StringBuilder buffer, List<CommitLogTemplateParser.TextTemplateNode> nodes,
                                         Date date,
                                         @Nullable String rootName,
                                         @Nullable Map<Change.Type, Collection<CommitLogEntry>> logEntriesByType
  )
  {
    int usedNodes = 0;
    for (int i = 0; i < nodes.size(); i++) {
      final CommitLogTemplateParser.TextTemplateNode textTemplateNode = nodes.get(i);
      String text = textTemplateNode.getText();
      if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.BLOCK_PLACEHOLDER_NODE) {
        String nodeText = text;
        if (isFileSectionStartPlaceholder(nodeText)) {
          i++;
          final List<CommitLogTemplateParser.TextTemplateNode> followingNodes = getFollowingNodes(nodes, i);
          CommitLogSection logSection = buildCommitLogFilesSection(followingNodes, rootName, nodeText);
          i += logSection.getUsedNodes() - 1;
          text = logSection.getText();
        } else if (text.equals(ROOT_ENTRY_END_PLACEHOLDER)) {
          usedNodes = i + 1;
          break;
        } else {
          text = "Illegal section placeholder " + text + " : expecting " +
                 CommitLogTemplateParser.BLOCK_PLACEHOLDER_OPEN_SYMBOL +
                 ROOT_ENTRY_END_PLACEHOLDER + CommitLogTemplateParser.BLOCK_PLACEHOLDER_CLOSE_SYMBOL;
        }
//        if (text.equals(ROOTS_SECTION_END_PLACEHOLDER)) {
        //they've forgotten the ENTRY_END, pretend ENTRY_END
//          usedNodes = i; // allow section end to be seen by calling method
//          break;
//        }
      } else if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.VALUE_PLACEHOLDER_NODE) {
        String nodeText = text;
        if (nodeText.equals(ROOT_NAME_PLACEHOLDER)) {
          text = rootName;
        } else if (nodeText.equals(FILE_COUNT_PLACEHOLDER)) {
          int fileCount = 0;
          if (logEntriesByType != null) {
            for (Map.Entry<Change.Type, Collection<CommitLogEntry>> entries : logEntriesByType.entrySet()) {
              fileCount += entries.getValue().size();
            }
          }
          text = String.valueOf(fileCount);
        } else {
          text = processCommonPlaceholders(nodeText, date);
        }
      }
      buffer.append(text);
    }
    return usedNodes;
  }

  @NotNull
  private CommitLogSection buildCommitLogFilesSection(@NotNull List<CommitLogTemplateParser.TextTemplateNode> nodes,
                                                      @Nullable String rootName)
  {
    final CommitLogSection deleted = buildCommitLogFilesSection(nodes, rootName, Change.Type.DELETED);
    final CommitLogSection modified = buildCommitLogFilesSection(nodes, rootName, Change.Type.MODIFICATION);
    final CommitLogSection created = buildCommitLogFilesSection(nodes, rootName, Change.Type.NEW);
    final int usedNodes = deleted.getUsedNodes();
    // all used nodes should be same
    assert (usedNodes == modified.getUsedNodes() && usedNodes == created.getUsedNodes());
    return new CommitLogSection(deleted.getText() + modified.getText() + created.getText(), usedNodes);
  }

  @NotNull
  @SuppressWarnings({"AssignmentToForLoopParameter"})
  private CommitLogSection buildCommitLogFilesSection(@NotNull List<CommitLogTemplateParser.TextTemplateNode> nodes,
                                                      @Nullable String rootName, Change.Type type)
  {
    int usedNodes = 0;
    @Nullable final Collection<CommitLogEntry> entries = getCommitLogEntries(rootName, type);
    final boolean hasEntries = entries != null && !entries.isEmpty();
    final StringBuilder result = hasEntries ? new StringBuilder(500) : new StringBuilder(0);
    for (int i = 0; i < nodes.size(); i++) {
      final CommitLogTemplateParser.TextTemplateNode textTemplateNode = nodes.get(i);
      String text = textTemplateNode.getText();
      if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.BLOCK_PLACEHOLDER_NODE) {
        if (text.equals(FILE_ENTRY_START_PLACEHOLDER)) {
          i++;
          final CommitLogSection logSection;
          List<CommitLogTemplateParser.TextTemplateNode> followingNodes = getFollowingNodes(nodes, i);
          logSection = hasEntries ?
                       buildCommitLogFileEntries(followingNodes, type, entries) :
                       buildCommitLogFileEntry(followingNodes, type, null);
          i += logSection.getUsedNodes() - 1;
          text = logSection.getText();
        } else if (isFilesSectionEndPlaceholder(text)) {
          usedNodes = i + 1;
          break;
        }
      } else if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.VALUE_PLACEHOLDER_NODE) {
        if (text.equals(FILE_COUNT_PLACEHOLDER)) {
          if (hasEntries) {
            text = String.valueOf(entries.size());
          }
        } else {
          text = "Illegal Placeholder : " + CommitLogTemplateParser.VALUE_PLACEHOLDER_SYMBOL + text +
                 CommitLogTemplateParser.VALUE_PLACEHOLDER_SYMBOL;
        }
      }
      if (hasEntries) {
        result.append(text);
      }
    }
    return new CommitLogSection(result.toString(), usedNodes);
  }

  @NotNull
  private static CommitLogSection buildCommitLogFileEntries(
    @NotNull List<CommitLogTemplateParser.TextTemplateNode> followingNodes,
    Change.Type type, Collection<CommitLogEntry> entries)
  {
    int usedNodes = 0;
    final StringBuilder result = new StringBuilder(500);
    for (final CommitLogEntry entry : entries) {
      CommitLogSection logSection = buildCommitLogFileEntry(followingNodes, type, entry);
      result.append(logSection.getText());
      usedNodes = logSection.getUsedNodes();
    }
    return new CommitLogSection(result.toString(), usedNodes);
  }

  @SuppressWarnings({"GetterCallInLoop"})
  private static CommitLogSection buildCommitLogFileEntry(List<CommitLogTemplateParser.TextTemplateNode> followingNodes,
                                                          Change.Type type,
                                                          @Nullable CommitLogEntry entry)
  {
    int usedNodes = 0;
    final StringBuilder result = new StringBuilder(500);
    @Nullable final FilePath filePath = entry != null ? entry.getFilePath() : null;
    for (int i = 0; i < followingNodes.size(); i++) {
      final CommitLogTemplateParser.TextTemplateNode textTemplateNode = followingNodes.get(i);
      String text = textTemplateNode.getText();
      if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.BLOCK_PLACEHOLDER_NODE) {
        if (text.equals(FILE_ENTRY_END_PLACEHOLDER)) {
          usedNodes = i + 1;
          break;
        } else {
          text = "Illegal section placeholder " + text + " : expecting one of " +
                 CommitLogTemplateParser.BLOCK_PLACEHOLDER_OPEN_SYMBOL +
                 DELETED_FILES_SECTION_END_PLACEHOLDER + CommitLogTemplateParser.BLOCK_PLACEHOLDER_CLOSE_SYMBOL + ',' +
                 CommitLogTemplateParser.BLOCK_PLACEHOLDER_OPEN_SYMBOL +
                 ADDED_FILES_SECTION_END_PLACEHOLDER + CommitLogTemplateParser.BLOCK_PLACEHOLDER_CLOSE_SYMBOL + ',' +
                 CommitLogTemplateParser.BLOCK_PLACEHOLDER_OPEN_SYMBOL +
                 MODIFIED_FILES_SECTION_END_PLACEHOLDER + CommitLogTemplateParser.BLOCK_PLACEHOLDER_CLOSE_SYMBOL +
                 " or " +
                 CommitLogTemplateParser.BLOCK_PLACEHOLDER_OPEN_SYMBOL +
                 ALL_FILES_SECTION_END_PLACEHOLDER + CommitLogTemplateParser.BLOCK_PLACEHOLDER_CLOSE_SYMBOL;
        }
//        else if (isFilesSectionEndPlaceholder(text)) {
//          // they've forgotten the ENTRY_END, pretend ENTRY_END
//          usedNodes = i; // allow section end to be seen by calling method
//          break;
//        }
      } else if (textTemplateNode.getType() == CommitLogTemplateParser.TextTemplateNodeType.VALUE_PLACEHOLDER_NODE) {
        if (entry != null) {
          if (text.equals(FILE_NAME_PLACEHOLDER)) {
            //noinspection GetterCallInLoop
            text = filePath != null ? filePath.getName() : "<no file>";
          } else if (text.equals(FILE_PATH_PLACEHOLDER)) {
            text = filePath != null ? filePath.getPath() : "<no file>";
          } else if (text.equals(FILE_ACTION_PLACEHOLDER)) {
            if (type == Change.Type.DELETED) {
              text = "Removed";
            } else if (type == Change.Type.MODIFICATION) {
              text = "Modified";
            } else if (type == Change.Type.NEW) {
              text = "Added";
            }
          } else if (text.equals(ROOT_NAME_PLACEHOLDER)) {
            text = entry.getVcsRootName();
          } else if (text.equals(PACKAGE_NAME_PLACEHOLDER)) {
            text = entry.getPackageName();
          } else if (text.equals(PACKAGE_PATH_PLACEHOLDER) || text.equals(PATH_FROM_ROOT_PLACEHOLDER)) {
            text = entry.getPathFromRoot();
          } else if (text.equals(OLD_REVISION_NUMBER_PLACEHOLDER)) {
            if (entry.getOldVersion() == null || type == Change.Type.NEW) {
              text = "Added";
            } else {
              text = entry.getOldVersion();
            }
          } else if (text.equals(NEW_REVISION_NUMBER_PLACEHOLDER)) {
            if (entry.getNewVersion() == null || type == Change.Type.DELETED) {
              text = "Removed";
            } else {
              text = entry.getNewVersion();
            }
          } else {
            text = "Illegal Placeholder : " + CommitLogTemplateParser.VALUE_PLACEHOLDER_SYMBOL + text +
                   CommitLogTemplateParser.VALUE_PLACEHOLDER_SYMBOL;
          }
        }
      }
      result.append(text);
    }
    return new CommitLogSection(result.toString(), usedNodes);
  }

  private static List<CommitLogTemplateParser.TextTemplateNode> getFollowingNodes(
    @NotNull List<CommitLogTemplateParser.TextTemplateNode> textTemplateNodes, int start)
  {
    return textTemplateNodes.subList(start, textTemplateNodes.size());
  }

  private static String toString(@NotNull Collection collection)
  {
    final StringBuilder stringBuilder = new StringBuilder(100);
    for (final Iterator iterator = collection.iterator(); iterator.hasNext();) {
      stringBuilder.append(iterator.next());
      if (iterator.hasNext()) {
        stringBuilder.append(", ");
      }
    }
    return stringBuilder.toString();
  }
}