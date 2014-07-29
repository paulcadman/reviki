package net.hillsdon.reviki.wiki.renderer;

import java.util.List;

import com.google.common.base.Supplier;

import net.hillsdon.fij.text.Escape;
import net.hillsdon.reviki.vc.PageInfo;
import net.hillsdon.reviki.vc.PageStore;
import net.hillsdon.reviki.wiki.MarkupRenderer;
import net.hillsdon.reviki.wiki.renderer.creole.CreoleRenderer;
import net.hillsdon.reviki.wiki.renderer.creole.LinkParts;
import net.hillsdon.reviki.wiki.renderer.creole.LinkPartsHandler;
import net.hillsdon.reviki.wiki.renderer.creole.ast.*;
import net.hillsdon.reviki.wiki.renderer.macro.Macro;

public class HtmlRenderer extends MarkupRenderer<String> {
  /**
   * Most elements have a consistent CSS class. Links and images are an
   * exception (as can be seen in their implementation), as their HTML is
   * generated by a link handler.
   */
  public static final String CSS_CLASS_ATTR = "class='wiki-content'";

  private final PageStore _pageStore;

  private final LinkPartsHandler _linkHandler;

  private final LinkPartsHandler _imageHandler;

  private final Supplier<List<Macro>> _macros;

  public HtmlRenderer(PageStore pageStore, LinkPartsHandler linkHandler, LinkPartsHandler imageHandler, Supplier<List<Macro>> macros) {
    _pageStore = pageStore;
    _linkHandler = linkHandler;
    _imageHandler = imageHandler;
    _macros = macros;

    renderer = new HtmlVisitor();
  }

  public ASTNode render(final PageInfo page) {
    return CreoleRenderer.render(_pageStore, page, _linkHandler, _imageHandler, _macros);
  }

  private final class HtmlVisitor extends ASTRenderer<String> {
    /**
     * This directive controls the vertical alignment of table cells.
     */
    private static final String TABLE_ALIGNMENT_DIRECTIVE = "table-alignment";

    public HtmlVisitor() {
      super("");
    }

    /**
     * Render a node with a tag.
     */
    public String renderTagged(String tag, ASTNode node) {
      // Render the children
      String inner = visitASTNode(node);

      // Render the tag
      if (inner.equals("")) {
        return "<" + tag + " " + CSS_CLASS_ATTR + " />";
      }
      else {
        return "<" + tag + " " + CSS_CLASS_ATTR + ">" + inner + "</" + tag + ">";
      }
    }

    @Override
    public String visitASTNode(ASTNode node) {
      String out = "";

      for (ASTNode child : node.getChildren()) {
        out += visit(child);
      }

      return out;
    }

    @Override
    public String visitBold(Bold node) {
      return renderTagged("strong", node);
    }

    @Override
    public String visitCode(Code node) {
      return renderTagged("pre", node);
    }

    @Override
    public String visitHeading(Heading node) {
      return renderTagged("h" + node.getLevel(), node);
    }

    @Override
    public String visitHorizontalRule(HorizontalRule node) {
      return renderTagged("hr", node);
    }

    @Override
    public String visitImage(Image node) {
      LinkPartsHandler handler = node.getHandler();
      PageInfo page = node.getPage();
      LinkParts parts = node.getParts();

      try {
        return handler.handle(page, Escape.html(parts.getText()), parts, urlOutputFilter());
      }
      catch (Exception e) {
        return Escape.html(parts.getText());
      }
    }

    @Override
    public String visitInlineCode(InlineCode node) {
      return renderTagged("code", node);
    }

    @Override
    public String visitItalic(Italic node) {
      return renderTagged("em", node);
    }

    @Override
    public String visitLinebreak(Linebreak node) {
      return renderTagged("br", node);
    }

    @Override
    public String visitLink(Link node) {
      LinkPartsHandler handler = node.getHandler();
      PageInfo page = node.getPage();
      LinkParts parts = node.getParts();

      try {
        return handler.handle(page, Escape.html(parts.getText()), parts, urlOutputFilter());
      }
      catch (Exception e) {
        // Special case: render mailto: as a link if it didn't get interwiki'd
        String target = node.getTarget();
        String title = node.getTitle();
        if (target.startsWith("mailto:")) {
          return String.format("<a href='%s'>%s</a>", target, Escape.html(title));
        }
        else {
          return Escape.html(parts.getText());
        }
      }
    }

    @Override
    public String visitListItem(ListItem node) {
      return renderTagged("li", node);
    }

    @Override
    public String visitMacroNode(MacroNode node) {
      return renderTagged(node.isBlock() ? "pre" : "code", node);
    }

    @Override
    public String visitOrderedList(OrderedList node) {
      return renderTagged("ol", node);
    }

    @Override
    public String visitParagraph(Paragraph node) {
      return renderTagged("p", node);
    }

    @Override
    public String visitStrikethrough(Strikethrough node) {
      return renderTagged("strike", node);
    }

    @Override
    public String visitTable(Table node) {
      return renderTagged("table", node);
    }

    @Override
    public String visitTableCell(TableCell node) {
      if (!isEnabled(TABLE_ALIGNMENT_DIRECTIVE)) {
        return renderTagged("td", node);
      }

      try {
        String out = "<td " + CSS_CLASS_ATTR;
        out += " style='vertical-align:" + unsafeGetArgs(TABLE_ALIGNMENT_DIRECTIVE).get(0) + "'>";
        out += visitASTNode(node);
        out += "</td>";
        return out;
      }
      catch (Exception e) {
        System.err.println("Error when handling directive " + TABLE_ALIGNMENT_DIRECTIVE);
        return renderTagged("td", node);
      }
    }

    @Override
    public String visitTableHeaderCell(TableHeaderCell node) {
      if (!isEnabled(TABLE_ALIGNMENT_DIRECTIVE)) {
        return renderTagged("th", node);
      }

      try {
        String out = "<th " + CSS_CLASS_ATTR;
        out += " style='vertical-align:" + unsafeGetArgs(TABLE_ALIGNMENT_DIRECTIVE).get(0) + "'>";
        out += visitASTNode(node);
        out += "</th>";
        return out;
      }
      catch (Exception e) {
        System.err.println("Error when handling directive " + TABLE_ALIGNMENT_DIRECTIVE);
        return renderTagged("th", node);
      }
    }

    @Override
    public String visitTableRow(TableRow node) {
      return renderTagged("tr", node);
    }

    @Override
    public String visitTextNode(TextNode node) {
      String text = node.getText();
      return node.isEscaped() ? Escape.html(text) : text;
    }

    @Override
    public String visitUnorderedList(UnorderedList node) {
      return renderTagged("ul", node);
    }
  }
}
