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

import java.util.*;

@SuppressWarnings({"SSBasedInspection", "CallToPrintStackTrace"})
public class CommitLogTemplateParser
{
  public static final String VALUE_PLACEHOLDER_SYMBOL = "$";
  public static final String BLOCK_PLACEHOLDER_OPEN_SYMBOL = "[";
  public static final String BLOCK_PLACEHOLDER_CLOSE_SYMBOL = "]";
  public static final String ESCAPE_SYMBOL = "\\";

  public List<TextTemplateNode> parseTextTemplate(String textTemplate) throws TextTemplateParserException
  {
    try {
      StringTokenizer tokens = new StringTokenizer(textTemplate, ESCAPE_SYMBOL + VALUE_PLACEHOLDER_SYMBOL +
                                                                 BLOCK_PLACEHOLDER_OPEN_SYMBOL +
                                                                 BLOCK_PLACEHOLDER_CLOSE_SYMBOL, true);
      List<TextTemplateNode> result = new LinkedList<TextTemplateNode>();
      boolean inPlaceholder = false;
      boolean inBlockPlaceholder = false;
      int nextLocation = 0;
      while (tokens.hasMoreTokens()) {
        int tokenLocation = nextLocation;
        String token = tokens.nextToken();
        nextLocation += token.length();
        if (VALUE_PLACEHOLDER_SYMBOL.equals(token)) {
          if (inBlockPlaceholder) {
            throwParserException("Block placeholders may not contain '" + VALUE_PLACEHOLDER_SYMBOL + '\'', tokenLocation);
          }
          inPlaceholder = !inPlaceholder;
        } else if (BLOCK_PLACEHOLDER_OPEN_SYMBOL.equals(token)) {
          if (inPlaceholder) {
            throwParserException("Placeholders may not contain '" + BLOCK_PLACEHOLDER_OPEN_SYMBOL + '\'',
                                 tokenLocation);
          } else {
            inPlaceholder = true;
            inBlockPlaceholder = true;
          }
        } else if (BLOCK_PLACEHOLDER_CLOSE_SYMBOL.equals(token)) {
          if (inPlaceholder) {
            inPlaceholder = false;
            inBlockPlaceholder = false;
          } else {
            throwParserException("Template may not contain unescaped '" +
                                 BLOCK_PLACEHOLDER_CLOSE_SYMBOL + "' - use '" + ESCAPE_SYMBOL +
                                                                                              BLOCK_PLACEHOLDER_CLOSE_SYMBOL + "' instead", tokenLocation);
          }
        } else {
          if (ESCAPE_SYMBOL.equals(token)) {
            if (tokens.hasMoreTokens()) {
              tokenLocation = nextLocation;
              token = tokens.nextToken();
              nextLocation += token.length();
              if (!VALUE_PLACEHOLDER_SYMBOL.equals(token) && !ESCAPE_SYMBOL.equals(token) &&
                  !BLOCK_PLACEHOLDER_CLOSE_SYMBOL.equals(token) && !BLOCK_PLACEHOLDER_OPEN_SYMBOL.equals(token)) {
                throwParserException('\'' + ESCAPE_SYMBOL + "' may only precede '" + VALUE_PLACEHOLDER_SYMBOL
                                     + "', '" + BLOCK_PLACEHOLDER_OPEN_SYMBOL + "', '" +
                                                                                       BLOCK_PLACEHOLDER_CLOSE_SYMBOL + "' or '" + ESCAPE_SYMBOL +
                                                                                                 '\'', tokenLocation);
              }
            } else {
              throwParserException('\'' + ESCAPE_SYMBOL + "' must be followed by '" + VALUE_PLACEHOLDER_SYMBOL
                                   + "', '" + BLOCK_PLACEHOLDER_OPEN_SYMBOL + "', '" +
                                                                                     BLOCK_PLACEHOLDER_CLOSE_SYMBOL + "' or '" + ESCAPE_SYMBOL +
                                                                                               '\'', tokenLocation);
            }
          }
          if (inPlaceholder) {
            if (inBlockPlaceholder) {
              if (tokens.hasMoreTokens()) {
                if (token.contains("\n"))
                  throwParserException("Block Placeholders may not contain linefeeds",
                                       tokenLocation + token.indexOf('\n') - 1);
                result.add(createTextTemplateNode(TextTemplateNodeType.BLOCK_PLACEHOLDER_NODE, token, tokenLocation));
              } else {
                throwParserException("Opening " + BLOCK_PLACEHOLDER_OPEN_SYMBOL +
                                     " detected with no closing " + BLOCK_PLACEHOLDER_CLOSE_SYMBOL,
                                     tokenLocation + token.length() - 1);
              }
            } else {
              if (tokens.hasMoreTokens()) {
                if (token.contains("\n"))
                  throwParserException("Value Placeholders may not contain linefeeds",
                                       tokenLocation + token.indexOf('\n') - 1);
                result.add(createTextTemplateNode(TextTemplateNodeType.VALUE_PLACEHOLDER_NODE, token, tokenLocation));
              } else {
                throwParserException("Opening " + VALUE_PLACEHOLDER_SYMBOL + " detected with no closing " +
                                     VALUE_PLACEHOLDER_SYMBOL, tokenLocation + token.length() - 1);
              }
            }
          } else {
            result.add(createTextTemplateNode(TextTemplateNodeType.TEXT_NODE, token, tokenLocation));
          }
        }
      }
      return result;
    } catch (TextTemplateParserException e) {
      throw new TextTemplateParserException(
        e.getMessage(), e, e.getLocation());
    }
  }

  private static void throwParserException(String message, int tokenLocation) throws TextTemplateParserException
  {
    throw new TextTemplateParserException("Illegal text template - error at index " + tokenLocation + " : " + message,
                                          tokenLocation);
  }

  public static void main(String[] args)
  {
    CommitLogTemplateParser p = new CommitLogTemplateParser();
    p.test("Low accuracy on Interface $DOMAIN-ELEMENT.bestname$ of $TARGET.name$");
    p.test("Low accuracy on Interface $DOMAIN-ELEMENT.bestname$ of $TARGET.name$.");
    p.test("$DOMAIN-ELEMENT.bestname Low accuracy on Interface of $TARGET.name.");
    p.test("$DOMAIN-ELEMENT.bestname$ Low accuracy on Interface of $TARGET.name$.");
    p.test("$DOMAIN-ELEMENT.bestname$ Low accuracy on Interface of $TARGET.name.");
    p.test("Low accuracy on Interface the bestname of $DOMAIN-ELEMENT$ of $TARGET.name");
    p.test("Low accuracy on Interface the <B>bestname of $DOMAIN-ELEMENT$ of the name of $TARGET$</B>");
    p.test("Low accuracy on Interface the bestname of $DOMAIN-ELEMENT$ of the name of $TARGET.$");
    p.test("Price is $PRICE$\\$");
    p.test("Price is $PRICE$\\$ down \\\\ from $OLD_PRICE$\\$");
  }

  private void test(String text)
  {
    System.out.println("Testing input : " + text);
    try {
      Iterator result = parseTextTemplate(text).iterator();
      System.out.println("Results : ");
      while (result.hasNext()) {
        TextTemplateNode node = (TextTemplateNode)result.next();
        System.out.println("Node of type " + node.getType() + " with text : " + node.getText() + "#END#");
      }
    } catch (TextTemplateParserException e) {
      e.printStackTrace(System.out);
    }
  }

  protected CommitLogTemplateParser.TextTemplateNode createTextTemplateNode(final TextTemplateNodeType type, final String text,
                                                                   final int location)
  {
    return new CommitLogTemplateParser.TextTemplateNode()
    {
      public TextTemplateNodeType getType()
      {
        return type;
      }

      public String getText()
      {
        return text;
      }

      @Override
      public String toString()
      {
        return text;
      }

      public int getLocation()
      {
        return location;
      }
    };
  }

  enum TextTemplateNodeType
  {
    TEXT_NODE, VALUE_PLACEHOLDER_NODE, BLOCK_PLACEHOLDER_NODE
  }

  interface TextTemplateNode
  {
    /**
     * Returns the type of the node - either TEXT_NODE, PLACEHOLDER_NODE or BLOCK_PLACEHOLDER_NODE. TEXT_NODE denotes
     * that the node represents a static part of the template, whereas PLACEHOLDER_NODE or BLOCK_PLACEHOLDER_NODE
     * denotes that the node represents a dynamic part of the template.
     *
     * @return the type of the node.
     */
    TextTemplateNodeType getType();

    /**
     * Returns the contents of the node.  If the node is of type TEXT_NODE, the plain static text will be returned.
     * However if the node is of type PLACEHOLDER_NODE or BLOCK_PLACEHOLDER_NODE then the information held within the
     * placholder will be returned.
     *
     * @return the contents of the node.
     */
    String getText();

    /**
     * @return the location of the node in the text
     */
    int getLocation();
  }

  public static class TextTemplateParserException extends Exception
  {
    private final int _location;

    public TextTemplateParserException(Throwable cause, int location)
    {
      super(cause);
      _location = location;
    }

    public TextTemplateParserException(String message, int location)
    {
      super(message);
      _location = location;
    }

    public TextTemplateParserException(String message, Throwable cause, int location)
    {
      super(message, cause);
      _location = location;
    }

    public int getLocation()
    {
      return _location;
    }
  }
}
