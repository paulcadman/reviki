/**
 * Copyright 2008 Matthew Hillsdon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hillsdon.reviki.wiki.renderer.creole;

import static java.util.Arrays.asList;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.hillsdon.reviki.vc.PageInfo;
import net.hillsdon.reviki.web.urls.URLOutputFilter;
import net.hillsdon.reviki.web.urls.UnknownWikiException;
import net.hillsdon.reviki.wiki.renderer.result.ResultNode;

public abstract class AbstractRegexNode implements RenderNode {

  private final List<RenderNode> _children = new ArrayList<RenderNode>();
  private final Pattern _matchRe;
  private RenderNode _fallback = null;

  public AbstractRegexNode(final String matchRe) {
    _matchRe = Pattern.compile(matchRe);
  }

  public List<RenderNode> getChildren() {
    return _children;
  }

  public void setFallback(final RenderNode fallback) {
    _fallback = fallback;
  }

  public AbstractRegexNode addChildren(final RenderNode... rules) {
    _children.addAll(asList(rules));
    return this;
  }

  public List<ResultNode> render(final PageInfo page, /* mutable */ String text, final RenderNode parent, final URLOutputFilter urlOutputFilter) {
    final List<ResultNode> result = new ArrayList<ResultNode>();
    while (text != null && text.length() > 0) {
      RenderNode earliestRule = null;
      Matcher earliestMatch = null;
      int earliestIndex = Integer.MAX_VALUE;
      for (RenderNode child : _children) {
        Matcher matcher = child.find(text);
        if (matcher != null && matcher.group(0).length() > 0) {
          if (matcher.start() < earliestIndex) {
            earliestIndex = matcher.start();
            earliestMatch = matcher;
            earliestRule = child;
          }
        }
      }
      if (earliestRule != null) {
        String beforeMatch = text.substring(0, earliestMatch.start());
        String afterMatch = text.substring(earliestMatch.end());
        fallback(page, result, beforeMatch, urlOutputFilter);
        try {
          ResultNode resultNode = earliestRule.handle(page, earliestMatch, this, urlOutputFilter);
          result.add(resultNode);
          text = afterMatch;
        }
        catch (URISyntaxException e) {
          fallback(page, result, text.substring(earliestMatch.start(), earliestMatch.end()), urlOutputFilter);
          text = afterMatch;
        }
        catch (UnknownWikiException e) {
          fallback(page, result, text.substring(earliestMatch.start(), earliestMatch.end()), urlOutputFilter);
          text = afterMatch;
        }
      }
      else {
        fallback(page, result, text, urlOutputFilter);
        text = "";
      }
    }
    return result;
  }

  private void fallback(final PageInfo page, final List<ResultNode> result, final String text, final URLOutputFilter urlOutputFilter) {
    if (_fallback != null) {
      result.addAll(_fallback.render(page, text, this, urlOutputFilter));
    }
    else {
      result.add(new HtmlEscapeResultNode(text));
    }
  }

  public Matcher find(final String text) {
    Matcher matcher = _matchRe.matcher(text);
    return matcher.find() && confirmMatchFind(matcher) ? matcher : null;
  }

  protected boolean confirmMatchFind(final Matcher matcher) {
    return true;
  }

}