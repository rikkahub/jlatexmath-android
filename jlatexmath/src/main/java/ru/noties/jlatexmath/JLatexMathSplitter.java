package ru.noties.jlatexmath;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Splits a LaTeX expression into multiple drawables that each fit within a maximum pixel width.
 *
 * <p>Useful for embedding LaTeX in Compose's AnnotatedString as InlineContent so that long
 * formulas can wrap across lines. Split points are placed before top-level binary operators
 * and relation symbols so the operator appears at the start of the new line, following
 * standard mathematical typography convention.</p>
 *
 * <pre>{@code
 * // Compose usage example:
 * val parts = JLatexMathSplitter.split(latex, maxWidthPx, textSize, color)
 * val annotatedString = buildAnnotatedString {
 *     parts.forEachIndexed { i, drawable ->
 *         appendInlineContent("latex_$i")
 *     }
 * }
 * val inlineContent = parts.mapIndexed { i, drawable ->
 *     "latex_$i" to InlineTextContent(
 *         Placeholder(drawable.intrinsicWidth.sp, drawable.intrinsicHeight.sp, PlaceholderVerticalAlign.Bottom)
 *     ) { AndroidView(factory = { ImageView(it).apply { setImageDrawable(drawable) } }) }
 * }.toMap()
 * }</pre>
 */
public final class JLatexMathSplitter {

    /**
     * Splits {@code latex} into segments where each rendered segment width ≤ {@code maxWidth}.
     *
     * <p>Split points are placed before top-level binary operators ({@code +}, {@code -},
     * {@code \times}, …) and relation symbols ({@code =}, {@code \leq}, {@code \to}, …).
     * Content inside braces {@code {}}, parentheses {@code ()}, brackets {@code []}, and
     * {@code \left…\right} / {@code \begin…\end} blocks is never split.</p>
     *
     * <p>If a single indivisible segment already exceeds {@code maxWidth} (e.g. a very wide
     * fraction), it is returned as-is rather than truncated.</p>
     *
     * @param latex    LaTeX source string
     * @param maxWidth maximum rendered width per segment in pixels
     * @param textSize text size passed to {@link JLatexMathDrawable.Builder#textSize}
     * @param color    foreground color (ARGB)
     * @return one or more drawables; never empty
     */
    @NonNull
    public static List<JLatexMathDrawable> split(
            @NonNull String latex,
            float maxWidth,
            float textSize,
            @ColorInt int color
    ) {
        List<Integer> splitPositions = findTopLevelSplitPositions(latex);

        if (splitPositions.isEmpty()) {
            return singletonList(latex, textSize, color);
        }

        // Boundaries at which a new segment may start; last boundary is end-of-string.
        List<Integer> boundaries = new ArrayList<>(splitPositions.size() + 1);
        boundaries.addAll(splitPositions);
        boundaries.add(latex.length());

        List<JLatexMathDrawable> result = new ArrayList<>();
        int segmentStart = 0;
        int lastGoodEnd = -1;  // last boundary where [segmentStart..lastGoodEnd] fits

        for (int boundary : boundaries) {
            if (boundary <= segmentStart) continue;

            String candidate = latex.substring(segmentStart, boundary).trim();
            if (candidate.isEmpty()) continue;

            float width = measureWidth(candidate, textSize);

            if (width <= maxWidth) {
                lastGoodEnd = boundary;
            } else {
                if (lastGoodEnd > segmentStart) {
                    // Flush the last segment that fit
                    result.add(buildDrawable(latex.substring(segmentStart, lastGoodEnd).trim(), textSize, color));
                    segmentStart = lastGoodEnd;
                    lastGoodEnd = -1;

                    // Re-evaluate: does [segmentStart..boundary] fit from the new start?
                    String newCandidate = latex.substring(segmentStart, boundary).trim();
                    if (!newCandidate.isEmpty() && measureWidth(newCandidate, textSize) <= maxWidth) {
                        lastGoodEnd = boundary;
                    }
                } else {
                    // No valid split before this point — add the oversized segment as-is
                    result.add(buildDrawable(candidate, textSize, color));
                    segmentStart = boundary;
                    lastGoodEnd = -1;
                }
            }
        }

        // Flush any remaining content
        if (segmentStart < latex.length()) {
            String remaining = latex.substring(segmentStart).trim();
            if (!remaining.isEmpty()) {
                result.add(buildDrawable(remaining, textSize, color));
            }
        }

        return result.isEmpty() ? singletonList(latex, textSize, color) : result;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the character positions (in {@code latex}) where a new segment may start.
     * These are positions of top-level binary operators and relation symbols.
     * Content nested inside {}, (), [], \left…\right, or \begin…\end is excluded.
     */
    @NonNull
    private static List<Integer> findTopLevelSplitPositions(@NonNull String latex) {
        List<Integer> positions = new ArrayList<>();
        int depth = 0;
        int i = 0;

        while (i < latex.length()) {
            char c = latex.charAt(i);

            if (c == '{' || c == '(' || c == '[') {
                depth++;
            } else if ((c == '}' || c == ')' || c == ']') && depth > 0) {
                depth--;
            } else if (c == '\\') {
                int cmdEnd = findCommandEnd(latex, i);
                String cmd = latex.substring(i, cmdEnd);

                if ("\\left".equals(cmd) || "\\begin".equals(cmd)) {
                    depth++;
                } else if ("\\right".equals(cmd) || "\\end".equals(cmd)) {
                    if (depth > 0) depth--;
                } else if (depth == 0 && SPLIT_COMMANDS.contains(cmd)) {
                    positions.add(i);
                }

                i = cmdEnd;
                continue;
            } else if (depth == 0 && isSplitChar(c)) {
                positions.add(i);
            }

            i++;
        }

        return positions;
    }

    private static boolean isSplitChar(char c) {
        return c == '+' || c == '-' || c == '=' || c == '<' || c == '>';
    }

    /** Returns the index one past the end of the LaTeX command starting at {@code start} (the '\'). */
    private static int findCommandEnd(@NonNull String latex, int start) {
        int i = start + 1;
        if (i >= latex.length()) return i;
        if (!Character.isLetter(latex.charAt(i))) return i + 1; // single-char commands like \{
        while (i < latex.length() && Character.isLetter(latex.charAt(i))) i++;
        return i;
    }

    private static float measureWidth(@NonNull String latex, float textSize) {
        try {
            return JLatexMathDrawable.builder(latex)
                    .textSize(textSize)
                    .build()
                    .getIntrinsicWidth();
        } catch (Exception e) {
            return Float.MAX_VALUE;
        }
    }

    @NonNull
    private static JLatexMathDrawable buildDrawable(@NonNull String latex, float textSize, @ColorInt int color) {
        return JLatexMathDrawable.builder(latex)
                .textSize(textSize)
                .color(color)
                .build();
    }

    @NonNull
    private static List<JLatexMathDrawable> singletonList(@NonNull String latex, float textSize, @ColorInt int color) {
        List<JLatexMathDrawable> list = new ArrayList<>(1);
        list.add(buildDrawable(latex, textSize, color));
        return list;
    }

    // -------------------------------------------------------------------------
    // Split-point command table
    // -------------------------------------------------------------------------

    private static final Set<String> SPLIT_COMMANDS = new HashSet<>(Arrays.asList(
            // Relations
            "\\leq", "\\le", "\\geq", "\\ge",
            "\\neq", "\\ne", "\\approx", "\\equiv",
            "\\sim", "\\simeq", "\\cong",
            "\\subset", "\\supset", "\\subseteq", "\\supseteq",
            "\\sqsubset", "\\sqsupset", "\\sqsubseteq", "\\sqsupseteq",
            "\\in", "\\notin", "\\ni",
            "\\to", "\\gets", "\\rightarrow", "\\leftarrow",
            "\\Rightarrow", "\\Leftarrow", "\\Leftrightarrow", "\\leftrightarrow",
            "\\longrightarrow", "\\longleftarrow",
            "\\Longrightarrow", "\\Longleftarrow", "\\Longleftrightarrow",
            "\\implies", "\\iff",
            "\\propto", "\\perp", "\\parallel",
            "\\vdash", "\\dashv", "\\models",
            "\\asymp", "\\bowtie", "\\smile", "\\frown",
            // Binary operators
            "\\pm", "\\mp",
            "\\times", "\\div", "\\cdot",
            "\\cup", "\\cap", "\\sqcup", "\\sqcap",
            "\\oplus", "\\ominus", "\\otimes", "\\oslash", "\\odot",
            "\\wedge", "\\vee",
            "\\setminus", "\\circ", "\\bullet",
            "\\star", "\\ast", "\\dagger", "\\ddagger",
            "\\amalg", "\\uplus"
    ));

    private JLatexMathSplitter() {
    }
}
