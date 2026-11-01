package dev.mekanismponder.client;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Keeps logical text intact; Minecraft applies Arabic shaping once at display time. */
public final class GuideTextLayout {
    // The native Ponder window is 180 pixels wide. Near-scene placement is kept inside
    // the screen so this full width remains available even at the 320-pixel minimum.
    public static final int CAPTION_WIDTH = 180;
    public static final int CAPTION_RIGHT_MARGIN = 8;
    public static final int MAX_PAGE_LINES = 3;

    private GuideTextLayout() {}

    public static List<String> captionPages(String text) {
        // Newlines are authored display-unit boundaries. Font width, punctuation,
        // abbreviations, and sentence heuristics never create additional pages.
        return List.copyOf(Arrays.asList(text.split("\n", -1)));
    }

    public static float constrainNearSceneX(float x, int uiWidth) {
        return Math.min(x, uiWidth - CAPTION_WIDTH - CAPTION_RIGHT_MARGIN);
    }

    /** Mirrors the width expression in TextWindowElement.render after x placement. */
    public static int nativeCaptionWidth(int uiWidth, float x) {
        return (int) Math.min(uiWidth - x, (float) CAPTION_WIDTH);
    }

    /** Adds natural line breaks inside one authored bubble without changing its text or page boundary. */
    public static String captionDisplayText(Font font, String authoredUnit) {
        String[] lines = authoredUnit.split("\n", -1);
        var display = new StringBuilder(authoredUnit.length());
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) display.append('\n');
            display.append(reflowInsideWindow(font, lines[index], CAPTION_WIDTH));
        }
        return display.toString();
    }

    private static String reflowInsideWindow(Font font, String text, int width) {
        if (text.isEmpty()) return text;
        var lineBreaks = new ArrayList<Integer>();
        int lineStart = 0;
        while (lineStart < text.length()) {
            BreakIterator boundaries = BreakIterator.getCharacterInstance(Locale.ROOT);
            boundaries.setText(text);
            int next = boundaries.following(lineStart);
            int fitEnd = lineStart;
            int lastWhitespaceEnd = -1;
            while (next != BreakIterator.DONE) {
                int previous = fitEnd;
                String candidate = text.substring(lineStart, next);
                boolean whitespaceChunk = containsWhitespace(text, previous, next);
                if (measuredWidth(font, candidate) > width) break;
                fitEnd = next;
                if (whitespaceChunk && hasNonWhitespace(text, lineStart, previous)) lastWhitespaceEnd = next;
                next = boundaries.next();
            }

            if (next == BreakIterator.DONE) break;
            // A word that fits must stay on this line even if its following space
            // overflows. Preserve that space at the start of the next visual line.
            boolean overflowingWhitespace = containsWhitespace(text, fitEnd, next);
            int breakAt = overflowingWhitespace ? fitEnd
                    : lastWhitespaceEnd > lineStart ? lastWhitespaceEnd : fitEnd;
            if (breakAt <= lineStart) {
                // An indivisible grapheme may itself exceed the window. Keep it in this bubble;
                // the authoring validation reports the overflow instead of dropping or paging it.
                breakAt = next;
            }
            if (breakAt >= text.length()) break;
            lineBreaks.add(breakAt);
            lineStart = breakAt;
        }

        if (lineBreaks.isEmpty()) return text;
        var display = new StringBuilder(text.length() + lineBreaks.size());
        int start = 0;
        for (int lineBreak : lineBreaks) {
            display.append(text, start, lineBreak).append('\n');
            start = lineBreak;
        }
        return display.append(text, start, text.length()).toString();
    }

    private static boolean containsWhitespace(String text, int start, int end) {
        for (int offset = start; offset < end;) {
            int codePoint = text.codePointAt(offset);
            if (isWhitespace(codePoint)) return true;
            offset += Character.charCount(codePoint);
        }
        return false;
    }

    private static boolean hasNonWhitespace(String text, int start, int end) {
        for (int offset = start; offset < end;) {
            int codePoint = text.codePointAt(offset);
            if (!isWhitespace(codePoint)) return true;
            offset += Character.charCount(codePoint);
        }
        return false;
    }

    /** Normalizes caption units for search while removing only paired display continuations. */
    public static String searchableCaption(String text) {
        String[] units = text.split("\n", -1);
        int[] omittedStart = new int[units.length];
        int[] omittedEnd = new int[units.length];
        Arrays.fill(omittedStart, -1);
        Arrays.fill(omittedEnd, -1);
        for (int index = 1; index < units.length; index++) {
            int previous = lastNonWhitespaceOffset(units[index - 1]);
            int current = firstNonWhitespaceOffset(units[index]);
            if (previous >= 0 && current >= 0
                    && units[index - 1].codePointAt(previous) == '…'
                    && units[index].codePointAt(current) == '…') {
                omittedEnd[index - 1] = previous;
                omittedStart[index] = current;
            }
        }

        var normalized = new StringBuilder(text.length());
        for (int unitIndex = 0; unitIndex < units.length; unitIndex++) {
            String unit = units[unitIndex];
            for (int offset = 0; offset < unit.length();) {
                int codePoint = unit.codePointAt(offset);
                if (offset != omittedStart[unitIndex] && offset != omittedEnd[unitIndex]
                        && !isWhitespace(codePoint)) normalized.appendCodePoint(codePoint);
                offset += Character.charCount(codePoint);
            }
        }
        return normalized.toString();
    }

    private static int firstNonWhitespaceOffset(String text) {
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            if (!isWhitespace(codePoint)) return offset;
            offset += Character.charCount(codePoint);
        }
        return -1;
    }

    private static int lastNonWhitespaceOffset(String text) {
        for (int offset = text.length(); offset > 0;) {
            int codePoint = text.codePointBefore(offset);
            offset -= Character.charCount(codePoint);
            if (!isWhitespace(codePoint)) return offset;
        }
        return -1;
    }

    private static boolean isWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    public static List<String> wrap(Font font, String text, int width) {
        var lines = new ArrayList<String>();
        String remaining = text.strip();
        while (!remaining.isEmpty()) {
            BreakIterator boundaries = BreakIterator.getCharacterInstance(Locale.ROOT);
            boundaries.setText(remaining);
            int end = 0;
            for (int next = boundaries.next(); next != BreakIterator.DONE; next = boundaries.next()) {
                String candidate = remaining.substring(0, next);
                if (candidate.indexOf('\n') >= 0 || measuredWidth(font, candidate) > width) break;
                end = next;
            }
            if (end == 0) end = remaining.offsetByCodePoints(0, 1);
            if (end < remaining.length()) {
                int space = remaining.lastIndexOf(' ', end);
                if (space > 0) end = space;
            }
            lines.add(remaining.substring(0, end).strip());
            remaining = remaining.substring(end).stripLeading();
        }
        return List.copyOf(lines);
    }

    public static int measuredWidth(Font font, String logicalText) {
        // Ponder wraps raw strings, while Minecraft renders visual-order glyphs.
        // Bound both, so neither the native wrapping nor Arabic shaping can overflow.
        return Math.max(font.width(logicalText), font.width(Component.literal(logicalText).getVisualOrderText()));
    }
}
