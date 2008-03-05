package net.hillsdon.svnwiki.webtests;

import java.io.IOException;

import net.hillsdon.svnwiki.text.WikiWordUtils;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlLink;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.xml.XmlPage;

public class TestSearch extends WebTestSupport {

  public void testLinkToOpenSearchAvailableFromRegularPages() throws Exception {
    HtmlPage results = getWebPage("pages/FrontPage");
    HtmlLink link = (HtmlLink) results.getByXPath("/html/head/link[@rel='search']").iterator().next();
    // Session crap on the end.
    assertTrue(link.getHrefAttribute().startsWith("/svnwiki/pages/FindPage/opensearch.xml"));
    XmlPage xml = (XmlPage) results.getWebClient().getPage(results.getFullyQualifiedUrl(link.getHrefAttribute()));
  }
  
  public void testSearchOffersToCreateWikiPageThatDoesntExistWhenWeSearchForAWikiWord() throws Exception {
    String name = uniqueWikiPageName("ThisDoesNotExist");
    HtmlPage results = search(getWebPage(""), name);
    assertTrue(results.asText().contains("Create new page " + name));
    results.getAnchorByHref("/svnwiki/pages/" + name);
  }
  
  /**
   * Search by WikiWord and Wiki Word.
   */
  public void testNewPageCanBeFoundBySearchIndex() throws Exception {
    String name = uniqueWikiPageName("SearchIndexTest");
    HtmlPage page = editWikiPage(name, "Should be found by search", "", true);
    assertSearchFindsPageUsingQuery(page, name, "found by search");
    assertSearchFindsPageUsingQuery(page, name, WikiWordUtils.pathToTitle(name));
    HtmlPage searchResult = search(page, name);
    // Goes directly to the page.
    assertEquals(page.getWebResponse().getUrl(), searchResult.getWebResponse().getUrl());
  }

  private HtmlPage search(final HtmlPage page, final String query) throws IOException {
    HtmlForm searchForm = page.getFormByName("searchForm");
    searchForm.getInputByName("query").setValueAttribute(query);
    HtmlPage results = (HtmlPage) searchForm.getInputByValue("Go").click();
    return results;
  }

  private void assertSearchFindsPageUsingQuery(final HtmlPage page, final String name, final String query) throws IOException {
    HtmlPage results = search(page, query);
    results.getAnchorByHref("/svnwiki/pages/" + name);
  }
  
}
