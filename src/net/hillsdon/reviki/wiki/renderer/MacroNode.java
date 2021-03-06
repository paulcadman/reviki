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
package net.hillsdon.reviki.wiki.renderer;

import java.util.List;
import java.util.regex.Matcher;

import net.hillsdon.fij.text.Escape;
import net.hillsdon.reviki.vc.PageInfo;
import net.hillsdon.reviki.web.urls.URLOutputFilter;
import net.hillsdon.reviki.wiki.renderer.creole.AbstractRegexNode;
import net.hillsdon.reviki.wiki.renderer.creole.HtmlEscapeResultNode;
import net.hillsdon.reviki.wiki.renderer.creole.RenderNode;
import net.hillsdon.reviki.wiki.renderer.macro.Macro;
import net.hillsdon.reviki.wiki.renderer.result.CompositeResultNode;
import net.hillsdon.reviki.wiki.renderer.result.LiteralResultNode;
import net.hillsdon.reviki.wiki.renderer.result.ResultNode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Supplier;

/**
 * Macro
 *
 * @author mth
 */
public class MacroNode extends AbstractRegexNode {

  private static final Log LOG = LogFactory.getLog(MacroNode.class);

  private final Supplier<List<Macro>> _macros;

  public MacroNode(final Supplier<List<Macro>> macros, final boolean block) {
    super(block ? "(?s)(?:^|\\n)<<([-\\p{Digit}\\p{L}]+?):(.+?)>>(^|\\n)"
                : "(?s)<<([-\\p{Digit}\\p{L}]+?):(.+?)>>");
    _macros = macros;
  }

  private String getMacroName(final Matcher matcher) {
    return matcher.group(1).trim();
  }

  public ResultNode handle(final PageInfo page, final Matcher matcher, final RenderNode parent, final URLOutputFilter urlOutputFilter) {
    // We need to move to a push system for updating macros to avoid this.
    final String macroName = getMacroName(matcher);
    Macro macro = null;
    List<Macro> macros = _macros.get();
    for (Macro candidate : macros) {
      if (candidate.getName().equals(macroName)) {
        macro = candidate;
        break;
      }
    }
    if (macro == null) {
      return new LiteralResultNode("<pre>" + Escape.html(matcher.group()) + "</pre>");
    }

    try {
      String content = macro.handle(page, matcher.group(2));
      switch (macro.getResultFormat()) {
        case XHTML:
          return new LiteralResultNode(content);
        case WIKI:
          // Use the parent as renderer if possible as that has the appropriate child nodes.
          RenderNode renderer = parent != null ? parent : this;
          return new CompositeResultNode(renderer.render(page, content, this, urlOutputFilter));
        default:
          return new HtmlEscapeResultNode(content);
      }
    }
    catch (Exception e) {
      LOG.error("Error handling macro on: " + page.getPath(), e);
      return new LiteralResultNode(String.format("<p>Error evaluating macro '%s': %s</p>", Escape.html(macro.getName()), Escape.html(e.getMessage())));
    }
  }

}
