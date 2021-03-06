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
package net.hillsdon.reviki.webtests;

import java.util.List;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomText;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;

public class TestHistory extends WebTestSupport {

  @SuppressWarnings("unchecked")
  public void test() throws Exception {
    String pageName = uniqueWikiPageName("HistoryTest");
    HtmlPage page = editWikiPage(pageName, "Initial content", "", "", true);
    page = editWikiPage(pageName, "Altered content", "", "s/Initial/Altered", false);
    HtmlPage history = (HtmlPage) ((HtmlAnchor) page.getByXPath("//a[@name='history']").iterator().next()).click();
    List<HtmlTableRow> historyRows = (List<HtmlTableRow>) history.getByXPath("//tr[td]");
    assertEquals(3, historyRows.size());
    HtmlTableRow altered = historyRows.get(1);
    // First column is date/time.
    verifyRow(altered, "s/Initial/Altered");
    HtmlTableRow initial = historyRows.get(2);
    verifyRow(initial, "None");

    final List<HtmlSubmitInput> compareButtons = (List<HtmlSubmitInput>) history.getByXPath("//input[@type='submit' and @value='Compare']/.");
    assertEquals(1, compareButtons.size());
    HtmlPage diff = (HtmlPage) compareButtons.get(0).click();
    assertEquals("Altered", ((DomNode) diff.getByXPath("//ins").iterator().next()).asText());
    assertEquals("Initial", ((DomNode) diff.getByXPath("//del").iterator().next()).asText());
    List<HtmlDivision> divs = (List<HtmlDivision>) diff.getByXPath("//div[@id='flash']/.");
    assertEquals(0, divs.size());

    // Check for the warning when viewing differences backwards
    final List<HtmlRadioButtonInput> radioButtons = (List<HtmlRadioButtonInput>) history.getByXPath("//input[@type='radio']/.");
    assertEquals(4, radioButtons.size());
    radioButtons.get(0).click();
    radioButtons.get(3).click();
    diff = (HtmlPage) compareButtons.get(0).click();
    divs = (List<HtmlDivision>) diff.getByXPath("//div[@id='flash']/.");
    assertEquals(1, divs.size());

  }

  public void testCompare() throws Exception {
    final String page1 = uniqueWikiPageName("HistoryCompare1.999@'.999Test");
    final String page2 = uniqueWikiPageName("HistoryCompare2.999@'.999Test");
    editWikiPage(page1, "1", "", "", true);
    editWikiPage(page1, "2", "", "", false);
    editWikiPage(page1, "3", "", "", false);
    HtmlPage page = renamePage(page1, page2);
    editWikiPage(page2, "4", "", "", false);
    page = (HtmlPage) page.getAnchorByName("history").click();
    HtmlForm form = page.getFormByName("compareForm");
    List<HtmlInput> fromList = form.getInputsByName("diff");
    List<HtmlInput> toList = form.getInputsByName("revision");
    final int last = fromList.size() - 1;
    assertEquals(last, toList.size() - 1);
    // The second newest revision is checked for "from"
    assertTrue(fromList.get(1).isChecked());
    // The newest revision is checked for "to"
    assertTrue(toList.get(0).isChecked());
    // Choose the oldest for "from"
    fromList.get(last).click();
    toList.get(0).click();
    HtmlPage diffPage = (HtmlPage) ((HtmlSubmitInput) form.getInputByName("compare")).click();
    final HtmlDivision diffRendering = (HtmlDivision) diffPage.getByXPath("id('wiki-rendering')").get(0);
    assertEquals("1", ((DomText) diffRendering.getByXPath("del/text()").get(0)).asText().trim());
    assertEquals("4", ((DomText) diffRendering.getByXPath("ins/text()").get(0)).asText().trim());
    // Choose "from" and "to" to be the oldest revisions so we can check title
    toList.get(last).click();
    diffPage = (HtmlPage) ((HtmlSubmitInput) form.getInputByName("compare")).click();
    diffPage.getTitleText().contains(page2);
  }

  private void verifyRow(final HtmlTableRow altered, final String content) {
    assertEquals(getUsername(), altered.getCell(2).asText());
    assertEquals(content, altered.getCell(3).asText());
  }

}
