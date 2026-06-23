package org.spin.sp014.notification.support;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tables.TablesExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.options.MutableDataSet;
import org.compiere.util.Util;

import java.util.Arrays;

/**
 * Utility to render Markdown text as HTML.
 * <p>
 * The text is always passed through flexmark: plain text becomes a paragraph,
 * Markdown is converted to its HTML representation, and any existing inline/block
 * HTML is preserved. This avoids unreliable "is this Markdown?" detection, since
 * any plain text is also valid Markdown.
 * <p>
 * GitHub Flavored Markdown is supported: tables, strikethrough, task lists and
 * bare-URL autolinking.
 */
public final class MarkdownUtil {

	private static final MutableDataSet OPTIONS = new MutableDataSet();
	static {
		OPTIONS.setFrom(ParserEmulationProfile.GITHUB_DOC);
		OPTIONS.set(Parser.EXTENSIONS, Arrays.asList(
				TablesExtension.create(),
				StrikethroughExtension.create(),
				TaskListExtension.create(),
				AutolinkExtension.create()));
	}
	private static final Parser PARSER = Parser.builder(OPTIONS).build();
	private static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();

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
