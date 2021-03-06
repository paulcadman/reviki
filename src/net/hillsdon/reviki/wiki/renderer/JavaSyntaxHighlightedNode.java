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

import java.io.IOException;
import java.util.regex.Matcher;

import net.hillsdon.fij.text.Escape;
import net.hillsdon.reviki.vc.PageInfo;
import net.hillsdon.reviki.web.urls.URLOutputFilter;
import net.hillsdon.reviki.wiki.renderer.creole.AbstractRegexNode;
import net.hillsdon.reviki.wiki.renderer.creole.RenderNode;
import net.hillsdon.reviki.wiki.renderer.result.LiteralResultNode;
import net.hillsdon.reviki.wiki.renderer.result.ResultNode;

import com.uwyn.jhighlight.renderer.XhtmlRendererFactory;

/**
 * Syntax formatting for java as in vqwiki.
 *
 * A general syntax needs some thought.
 *
 * @author mth
 */
public class JavaSyntaxHighlightedNode extends AbstractRegexNode {

  public JavaSyntaxHighlightedNode(final boolean block) {
    super(
        block ? "(?s)(?:^|\\n)\\[<java>\\](.*?)(^|\\n)\\[</java>\\]"
              : "(?s)\\[<java>\\](.*?)\\[</java>\\]");
  }

  public ResultNode handle(final PageInfo page, final Matcher matcher, RenderNode parent, final URLOutputFilter urlOutputFilter) {
    String content = matcher.group(1).trim();
    try {
      return new LiteralResultNode(XhtmlRendererFactory.getRenderer(XhtmlRendererFactory.JAVA).highlight("", content, "UTF-8", true));
    }
    catch (IOException e) {
      return new LiteralResultNode("<pre>" + Escape.html(content) + "</pre>");
    }
  }

}
