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
package net.hillsdon.reviki.web.redirect;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.hillsdon.reviki.configuration.WikiConfiguration;
import net.hillsdon.reviki.vc.PageReference;
import net.hillsdon.reviki.web.common.View;
import net.hillsdon.reviki.web.urls.ApplicationUrls;
import net.hillsdon.reviki.web.urls.URLOutputFilter;
import net.hillsdon.reviki.web.urls.WikiUrls;
import net.hillsdon.reviki.web.urls.impl.WikiUrlsImpl;

/**
 * Communicates a redirect to a wiki page.
 * 
 * @author mth
 */
public class RedirectToPageView implements View {

  private final PageReference _page;
  private final WikiUrls _wikiUrls;
  private final String _appended;

  public RedirectToPageView(final WikiUrls wikiUrls, final PageReference page) {
    this(wikiUrls, page, "");
  }
  
  public RedirectToPageView(final WikiUrls wikiUrls, final PageReference page, final String appended) {
    _wikiUrls = wikiUrls;
    _page = page;
    _appended = appended;
  }

  public PageReference getPage() {
    return _page;
  }

  public void render(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
    response.sendRedirect(response.encodeRedirectURL(_wikiUrls.page(null, _page.getPath()) + _appended));
  }

  public static View create(final PageReference page, final ApplicationUrls applicationUrls, final WikiConfiguration perWikiConfiguration) {
    return new RedirectToPageView(new WikiUrlsImpl(applicationUrls, perWikiConfiguration), page);
  }
  
}
