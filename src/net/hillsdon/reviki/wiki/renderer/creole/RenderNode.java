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

import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;

import net.hillsdon.reviki.vc.PageInfo;
import net.hillsdon.reviki.web.urls.URLOutputFilter;
import net.hillsdon.reviki.web.urls.UnknownWikiException;
import net.hillsdon.reviki.wiki.renderer.result.ResultNode;

public interface RenderNode {

  /**
   * @return Child nodes as set in {@link #addChildren(RenderNode...)},
   *         the default is the empty list.
   */
  List<RenderNode> getChildren();

  /**
   * @param nodes Child nodes, matches will be attempted in the order given,
   *              giving a priority to earlier rules in case of equal match
   *              indices.
   *
   * @return this, for conviniene.
   */
  RenderNode addChildren(RenderNode... nodes);

  /**
   * Render starting from this node.
   * @param page PageInfo containing the original page content and attributes.
   * @param text The part of the content relevant to the rendering.
   * @param parent TODO
   * @return Rendered HTML.
   */
  List<ResultNode> render(PageInfo page, String text, RenderNode parent, URLOutputFilter urlOutputFilter);

  /**
   * Test for a match in the given text.
   *
   * @param text The text.
   * @return A matcher if a match was found, null otherwise.
   */
  Matcher find(String text);

  /**
   * @param page PageInfo with the original content and attributes.
   * @param matcher A matcher that found a match using our find method and we were deemed the best.
   * @param parent TODO
   * @return Replacement text for the match (this method should recurse to complete rendering of the match).
   */
  ResultNode handle(PageInfo page, Matcher matcher, RenderNode parent, URLOutputFilter urlOutputFilter) throws URISyntaxException, UnknownWikiException;

}
