package org.spin.sp014.notification.support;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.compiere.util.Util;

import java.util.Arrays;
import java.util.List;

/**
 * Utility to render Markdown text as HTML.
 * <p>
 * The text is always passed through commonmark-java: plain text becomes a
 * paragraph, Markdown is converted to its HTML representation, and any existing
 * inline/block HTML is preserved. This avoids unreliable "is this Markdown?"
 * detection, since any plain text is also valid Markdown.
 * <p>
 * GitHub Flavored Markdown is supported: tables, strikethrough, task lists and
 * bare-URL autolinking.
 */
public final class MarkdownUtil {

	private static final List<Extension> EXTENSIONS = Arrays.asList(
			TablesExtension.create(),
			StrikethroughExtension.create(),
			TaskListItemsExtension.create(),
			AutolinkExtension.create());
	private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
	private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).build();

	private MarkdownUtil() {
	}

	/**
	 * Convert the given Markdown (or plain text) to HTML.
	 *
	 * @param text source text, may be {@code null}
	 * @return rendered HTML, or an empty string when {@code text} is null/blank
	 */
	public static String toHtml(String text) {
		if (Util.isEmpty(text, true)) {
			return "";
		}
		return RENDERER.render(PARSER.parse(text));
	}
}
