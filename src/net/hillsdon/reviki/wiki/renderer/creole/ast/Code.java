package net.hillsdon.reviki.wiki.renderer.creole.ast;

import java.io.IOException;

import net.hillsdon.fij.text.Escape;

import com.uwyn.jhighlight.renderer.Renderer;

public class Code extends TaggedNode {
  public Code(final String contents) {
    super("pre", new Raw(Escape.html(contents)));
  }

  public Code(final String contents, final Renderer highlighter) throws IOException {
    super("pre", new Raw(highlighter.highlight("", contents, "UTF-8", true).replace("&nbsp;", " ").replace("<br />", "")));
  }
}
