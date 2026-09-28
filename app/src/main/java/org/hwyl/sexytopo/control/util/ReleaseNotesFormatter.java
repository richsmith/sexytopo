package org.hwyl.sexytopo.control.util;

import org.apache.commons.text.StringEscapeUtils;

/**
 * Turns the release notes markdown (docs/releases.md) into an HTML page. Only the subset of
 * markdown that the release notes actually use is supported: "# " headings, "- " list items and
 * blank lines. Anything else is shown as a plain paragraph.
 */
public class ReleaseNotesFormatter {

    private static final String STYLE =
            "body { font-family: sans-serif; line-height: 1.5; margin: 0 auto; padding: 1rem;"
                    + " max-width: min(70ch, 90%); background-color: #ffffff; color: #000000; }"
                    + " h2 { font-size: 1.2rem; margin: 1.5rem 0 0.5rem 0; }"
                    + " ul { padding-left: 1.2rem; margin: 0; }"
                    + " li { margin: 0.3rem 0; }"
                    + " @media (prefers-color-scheme: dark) {"
                    + " body { background-color: #1e1e1e; color: #e0e0e0; } }";

    public static String toHtml(String markdown) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<style>")
                .append(STYLE)
                .append("</style></head><body>\n");

        boolean inList = false;
        for (String rawLine : markdown.split("\\r?\\n")) {
            String line = rawLine.trim();
            boolean isItem = line.startsWith("- ");

            if (inList && !isItem) {
                html.append("</ul>\n");
                inList = false;
            }

            if (line.isEmpty()) {
                continue;
            } else if (line.startsWith("#")) {
                html.append("<h2>").append(escape(line.replaceFirst("^#+", ""))).append("</h2>\n");
            } else if (isItem) {
                if (!inList) {
                    html.append("<ul>\n");
                    inList = true;
                }
                html.append("<li>").append(escape(line.substring(2))).append("</li>\n");
            } else {
                html.append("<p>").append(escape(line)).append("</p>\n");
            }
        }

        if (inList) {
            html.append("</ul>\n");
        }

        html.append("</body></html>\n");
        return html.toString();
    }

    private static String escape(String text) {
        return StringEscapeUtils.escapeHtml4(text.trim());
    }
}
