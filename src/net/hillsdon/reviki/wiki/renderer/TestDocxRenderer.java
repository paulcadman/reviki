package net.hillsdon.reviki.wiki.renderer;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBElement;

import net.hillsdon.reviki.vc.impl.SimplePageStore;
import net.hillsdon.reviki.web.common.ViewTypeConstants;
import net.hillsdon.reviki.web.urls.InternalLinker;
import net.hillsdon.reviki.web.urls.URLOutputFilter;
import net.hillsdon.reviki.web.urls.impl.ExampleDotComWikiUrls;
import net.hillsdon.reviki.wiki.renderer.DocxRenderer.DocxVisitor;
import net.hillsdon.reviki.wiki.renderer.creole.ast.*;
import net.hillsdon.reviki.wiki.renderer.macro.Macro;

import org.apache.commons.io.IOUtils;
import org.docx4j.wml.*;

import com.google.common.base.Optional;
import com.google.common.base.Suppliers;

import junit.framework.TestCase;

/**
 * Because the docx renderer is very side-effectful, this only attempts to test
 * the more clean parts of it. Hopefully that's enough.
 */
public class TestDocxRenderer extends TestCase {
  private DocxRenderer _renderer;

  private DocxVisitor _visitor;

  private ObjectFactory _factory;

  public void setUp() {
    _visitor = new DocxVisitor(null, null, URLOutputFilter.NULL);
    _factory = new ObjectFactory();

    SvnWikiRenderer svnrenderer = new SvnWikiRenderer(new FakeConfiguration(), new SimplePageStore(), new InternalLinker(new ExampleDotComWikiUrls()), Suppliers.ofInstance(Collections.<Macro> emptyList()));
    _renderer = (DocxRenderer) svnrenderer.getRenderers().getRenderer(ViewTypeConstants.CTYPE_DOCX);
  }

  /** Test that we can construct styles. */
  public void testConstructStyle() {
    String name = "Hello World";
    String basedOn = "Test Style";
    String type = "paragraph";
    Optional<JcEnumeration> justification = Optional.of(JcEnumeration.RIGHT);

    Optional<PPrBase.Spacing> spacing = Optional.of(_factory.createPPrBaseSpacing());
    spacing.get().setAfter(BigInteger.valueOf(140));
    spacing.get().setLine(BigInteger.valueOf(288));
    spacing.get().setLineRule(STLineSpacingRule.AUTO);
    spacing.get().setBefore(BigInteger.ZERO);

    boolean bold = true;

    Style style = DocxVisitor.constructStyle(name, basedOn, type, justification, spacing, bold);
    Style style2 = DocxVisitor.constructStyle(name, basedOn, type, justification, spacing, false);

    assertTrue(style.getName().getVal().equals(name));
    assertTrue(style.getStyleId().equals(DocxVisitor.styleNameToId(name)));
    assertTrue(style.getBasedOn().getVal().equals(DocxVisitor.styleNameToId(basedOn)));
    assertTrue(style.getType().equals(type));
    assertTrue(style.getPPr().getJc().getVal().equals(justification.get()));
    assertTrue(style.getPPr().getSpacing().equals(spacing.get()));
    assertEquals(style.getRPr().getB().isVal(), bold);

    // RPr is only set if there is something (a bold, in the case of these
    // styles) to go in it.
    assertNull(style2.getRPr());
  }

  /** Test that we can get the true name (id) of a style. */
  public void testStyleNameToId() {
    assertTrue(DocxVisitor.styleNameToId("hello world").equals("helloworld"));
    assertTrue(DocxVisitor.styleNameToId(" foo").equals("foo"));
    assertTrue(DocxVisitor.styleNameToId("bar ").equals("bar"));
    assertTrue(DocxVisitor.styleNameToId(" foo bar baz ").equals("foobarbaz"));
  }

  /** Test that we can enter and exit regular contexts. */
  public void testContexts() {
    assertEquals(1, _visitor._contexts.size());
    assertEquals(1, _visitor._blockContexts.size());

    P paragraph = _factory.createP();
    _visitor.enterContext(paragraph, false);

    assertEquals(2, _visitor._contexts.size());
    assertEquals(1, _visitor._blockContexts.size());

    Body body = _factory.createBody();
    _visitor.enterContext(body, true);

    assertEquals(3, _visitor._contexts.size());
    assertEquals(2, _visitor._blockContexts.size());

    _visitor.exitContext(true);

    assertEquals(2, _visitor._contexts.size());
    assertEquals(1, _visitor._blockContexts.size());

    _visitor.exitContext(false);

    assertEquals(1, _visitor._contexts.size());
    assertEquals(1, _visitor._blockContexts.size());
  }

  /** Test we can enter and exit list contexts. */
  public void testListContexts() {
    BigInteger id = DocxVisitor.UNORDERED_LIST_ID;

    assertEquals(0, _visitor._numberings.size());

    _visitor.enterListContext(id);

    assertEquals(1, _visitor._numberings.size());

    PPrBase.NumPr numpr = _visitor._numberings.peek();
    assertTrue(numpr.getNumId().getVal().equals(id));
    assertTrue(numpr.getIlvl().getVal().equals(BigInteger.ZERO));

    _visitor.enterListContext(id);

    assertEquals(2, _visitor._numberings.size());

    numpr = _visitor._numberings.peek();
    assertTrue(numpr.getNumId().getVal().equals(id));
    assertTrue(numpr.getIlvl().getVal().equals(BigInteger.ONE));

    _visitor.exitListContext();

    assertEquals(1, _visitor._numberings.size());

    _visitor.exitListContext();

    assertEquals(0, _visitor._numberings.size());
  }

  /** Test that we can apply styles to paragraphs. */
  public void testParaStyle() {
    P paragraph = _factory.createP();
    String style1 = "foo";
    String style2 = "foo";

    DocxVisitor.paraStyle(paragraph, style1);
    assertTrue(paragraph.getPPr().getPStyle().getVal().equals(style1));

    DocxVisitor.paraStyle(paragraph, style2);
    assertTrue(paragraph.getPPr().getPStyle().getVal().equals(style2));
  }

  /** Get the text of a run. */
  @SuppressWarnings("unchecked")
  protected String getRunText(R run) {
    return ((JAXBElement<Text>) run.getContent().get(0)).getValue().getValue();
  }

  /** Test that we can set the text of a run. */
  public void testRunText() {
    R run = _factory.createR();
    String text = "hello world";

    DocxVisitor.runText(run, text);

    assertTrue(getRunText(run).equals(text));
  }

  /** Test that we can construct a run and push it to a context. */
  public void testConstructRun() {
    P paragraph = _factory.createP();
    _visitor.enterContext(paragraph, false);

    _visitor._bold.setVal(Boolean.TRUE);
    _visitor._italic.setVal(Boolean.TRUE);

    R run = _visitor.constructRun(false);

    assertTrue(paragraph.getContent().contains(run));
    assertNull(run.getRPr());

    run = _visitor.constructRun(true);

    assertTrue(run.getRPr().getB().isVal());
    assertTrue(run.getRPr().getI().isVal());
    assertFalse(run.getRPr().getStrike().isVal());

    _visitor._italic.setVal(Boolean.FALSE);
    assertTrue(run.getRPr().getI().isVal());
  }

  /** Test that we can set the font of a run. */
  public void testRPrFont() {
    RPr rpr = _factory.createRPr();
    String font = "A Font";

    DocxVisitor.runFont(rpr, font);

    assertTrue(rpr.getRFonts().getAscii().equals(font));
    assertTrue(rpr.getRFonts().getHAnsi().equals(font));
  }

  /** Test that we can commit things to the context. */
  public void testContextCommit() {
    P paragraph = _factory.createP();
    R run = _factory.createR();

    _visitor.commitBlock(paragraph);
    _visitor._contexts.push(paragraph);
    _visitor.commitInline(run);

    assertTrue(_visitor._blockContexts.peek().getContent().contains(paragraph));
    assertTrue(paragraph.getContent().contains(run));
    assertFalse(_visitor._blockContexts.peek().getContent().contains(run));
  }

  /** Test that we can align table cells. */
  public void testTableCellValign() {
    Tc tablecell = _factory.createTc();

    DocxVisitor.applyValign(tablecell, "top");
    assertTrue(tablecell.getTcPr().getVAlign().getVal().equals(STVerticalJc.TOP));

    DocxVisitor.applyValign(tablecell, "middle");
    assertTrue(tablecell.getTcPr().getVAlign().getVal().equals(STVerticalJc.CENTER));

    DocxVisitor.applyValign(tablecell, "center");
    assertTrue(tablecell.getTcPr().getVAlign().getVal().equals(STVerticalJc.CENTER));

    DocxVisitor.applyValign(tablecell, "centre");
    assertTrue(tablecell.getTcPr().getVAlign().getVal().equals(STVerticalJc.CENTER));

    DocxVisitor.applyValign(tablecell, "bottom");
    assertTrue(tablecell.getTcPr().getVAlign().getVal().equals(STVerticalJc.BOTTOM));
  }

  /** Sanity check: check that we actually get output. */
  public void testSanity() {
    Page page = new Page(new ArrayList<ASTNode>());
    InputStream is = _renderer.render(page, URLOutputFilter.NULL);

    assertNotNull(is);

    try {
      byte[] rendered = IOUtils.toByteArray(is);
      assertFalse(0 == rendered.length);
    }
    catch (IOException e) {
      assertFalse("Failed to extract byte array", true);
    }
  }

  /**
   * Test the rendering of a four-element table, including styles and cell
   * contents.
   */
  public void testTables() {
    List<ASTNode> foo = Arrays.asList(new ASTNode[] { new Plaintext("foo") });
    List<ASTNode> bar = Arrays.asList(new ASTNode[] { new Plaintext("bar") });
    List<ASTNode> baz = Arrays.asList(new ASTNode[] { new Plaintext("baz") });
    List<ASTNode> bat = Arrays.asList(new ASTNode[] { new Plaintext("bat") });
    TableHeaderCell th1 = new TableHeaderCell(new Inline(foo));
    TableHeaderCell th2 = new TableHeaderCell(new Inline(bar));
    TableCell td1 = new TableCell(new Inline(baz));
    TableCell td2 = new TableCell(new Inline(bat));
    TableRow tr1 = new TableRow(Arrays.asList(new ASTNode[] { th1, th2 }));
    TableRow tr2 = new TableRow(Arrays.asList(new ASTNode[] { td1, td2 }));
    Table table = new Table(Arrays.asList(new ASTNode[] { tr1, tr2 }));

    Body body = _factory.createBody();
    _visitor.enterContext(body, true);

    _visitor.visit(table);

    assertEquals(1, body.getContent().size());

    Tbl tbl = (Tbl) body.getContent().get(0);

    assertEquals(2, tbl.getContent().size());
    assertNotNull(tbl.getTblPr().getTblW());
    assertFalse(tbl.getTblPr().getTblW().getW().equals(BigInteger.ZERO));

    Tr tblTr1 = (Tr) tbl.getContent().get(0);
    Tr tblTr2 = (Tr) tbl.getContent().get(1);

    Tc tblTr1Tc1 = (Tc) tblTr1.getContent().get(0);
    Tc tblTr1Tc2 = (Tc) tblTr1.getContent().get(1);
    Tc tblTr2Tc1 = (Tc) tblTr2.getContent().get(0);
    Tc tblTr2Tc2 = (Tc) tblTr2.getContent().get(1);

    assertEquals(1, tblTr1Tc1.getContent().size());
    assertEquals(1, tblTr1Tc2.getContent().size());
    assertEquals(1, tblTr2Tc1.getContent().size());
    assertEquals(1, tblTr2Tc2.getContent().size());

    P p1 = (P) tblTr1Tc1.getContent().get(0);
    P p2 = (P) tblTr1Tc2.getContent().get(0);
    P p3 = (P) tblTr2Tc1.getContent().get(0);
    P p4 = (P) tblTr2Tc2.getContent().get(0);

    assertNotNull(p1.getPPr());
    assertNotNull(p2.getPPr());
    assertNotNull(p3.getPPr());
    assertNotNull(p4.getPPr());

    String headerStyle = DocxVisitor.styleNameToId(DocxVisitor.TABLE_HEADER_STYLE);
    String contentStyle = DocxVisitor.styleNameToId(DocxVisitor.TABLE_CONTENTS_STYLE);

    assertTrue(p1.getPPr().getPStyle().getVal().equals(headerStyle));
    assertTrue(p2.getPPr().getPStyle().getVal().equals(headerStyle));
    assertTrue(p3.getPPr().getPStyle().getVal().equals(contentStyle));
    assertTrue(p4.getPPr().getPStyle().getVal().equals(contentStyle));

    assertEquals(1, p1.getContent().size());
    assertEquals(1, p2.getContent().size());
    assertEquals(1, p3.getContent().size());
    assertEquals(1, p4.getContent().size());

    R r1 = (R) p1.getContent().get(0);
    R r2 = (R) p2.getContent().get(0);
    R r3 = (R) p3.getContent().get(0);
    R r4 = (R) p4.getContent().get(0);

    assertTrue(getRunText(r1).equals("foo"));
    assertTrue(getRunText(r2).equals("bar"));
    assertTrue(getRunText(r3).equals("baz"));
    assertTrue(getRunText(r4).equals("bat"));
  }

  /** Test that the style a heading gets is dependent upon the level. */
  public void testHeadingStyles() {
    Heading h1 = new Heading(1, new Inline(Arrays.asList(new ASTNode[] { new Plaintext("foo") })));
    Heading h2 = new Heading(2, new Inline(Arrays.asList(new ASTNode[] { new Plaintext("bar") })));
    Heading h3 = new Heading(3, new Inline(Arrays.asList(new ASTNode[] { new Plaintext("baz") })));
    Heading h4 = new Heading(4, new Inline(Arrays.asList(new ASTNode[] { new Plaintext("bat") })));
    Heading h5 = new Heading(5, new Inline(Arrays.asList(new ASTNode[] { new Plaintext("qux") })));
    Heading h6 = new Heading(6, new Inline(Arrays.asList(new ASTNode[] { new Plaintext("qix") })));

    Body body = _factory.createBody();
    _visitor.enterContext(body, true);

    _visitor.visitHeading(h1);
    _visitor.visitHeading(h2);
    _visitor.visitHeading(h3);
    _visitor.visitHeading(h4);
    _visitor.visitHeading(h5);
    _visitor.visitHeading(h6);

    assertEquals(6, body.getContent().size());

    for (int i = 0; i < 6; i++) {
      P header = (P) body.getContent().get(i);
      String style;

      switch (i) {
        case 0:
          style = DocxVisitor.HEADING1_STYLE;
          break;
        case 1:
          style = DocxVisitor.HEADING2_STYLE;
          break;
        case 2:
          style = DocxVisitor.HEADING3_STYLE;
          break;
        case 3:
          style = DocxVisitor.HEADING4_STYLE;
          break;
        case 4:
          style = DocxVisitor.HEADING5_STYLE;
          break;
        default:
          style = DocxVisitor.HEADING6_STYLE;
      }

      assertTrue(header.getPPr().getPStyle().getVal().equals(DocxVisitor.styleNameToId(style)));
    }
  }

  /** Test paragraphs contain different runs for different formatting */
  public void testFormatting() {
    Inline inlinetxt = new Inline(Arrays.asList(new ASTNode[] { new Plaintext("foo") }));
    Inline inlinetxt2 = new Inline(Arrays.asList(new ASTNode[] { new Bold(inlinetxt) }));
    ASTNode[] inline = new ASTNode[] { new Bold(inlinetxt), new Italic(inlinetxt), new Strikethrough(inlinetxt), new Strikethrough(inlinetxt2) };
    Paragraph paragraph = new Paragraph(new Inline(Arrays.asList(inline)));

    Body body = _factory.createBody();
    _visitor.enterContext(body, true);

    _visitor.visitParagraph(paragraph);

    assertEquals(1, body.getContent().size());

    P p = (P) body.getContent().get(0);
    assertEquals(4, p.getContent().size());
  }
}
