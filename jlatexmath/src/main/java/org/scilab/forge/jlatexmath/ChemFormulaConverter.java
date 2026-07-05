/* ChemFormulaConverter.java
 * =========================================================================
 * This file is part of the JLaTeXMath Library - http://forge.scilab.org/jlatexmath
 */

package org.scilab.forge.jlatexmath;

final class ChemFormulaConverter {

    private ChemFormulaConverter() { }

    static String convert(String input) {
        if (input == null || input.length() == 0) {
            return "";
        }
        input = input.replace('\n', ' ')
                .replace('\u2212', '-')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2010', '-')
                .replace("\u2026", "...");

        StringBuilder out = new StringBuilder();
        int pos = 0;
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (Character.isWhitespace(ch)) {
                appendSpace(out);
                pos = skipWhiteSpace(input, pos);
                continue;
            }

            Arrow arrow = matchArrow(input, pos);
            if (arrow != null) {
                pos = appendArrow(input, pos + arrow.token.length(), arrow, out);
                continue;
            }

            String operator = matchOperator(input, pos);
            if (operator != null) {
                appendBinary(out, operator);
                pos += operator.length();
                continue;
            }

            String upDown = matchUpDown(input, pos);
            if (upDown != null) {
                appendSpace(out);
                out.append(upDown);
                out.append("{}\\ ");
                pos += input.charAt(pos) == '(' ? 3 : 1;
                continue;
            }

            if (ch == '=') {
                appendRelation(out, "=");
                pos++;
                continue;
            }

            if (ch == '+' || ch == '-') {
                appendBinary(out, Character.toString(ch));
                pos++;
                continue;
            }

            int end = readFormulaTokenEnd(input, pos);
            if (end == pos) {
                out.append(escape(ch));
                pos++;
            } else {
                out.append(convertFormulaToken(input.substring(pos, end)));
                pos = end;
            }
        }

        return out.toString();
    }

    private static int appendArrow(String input, int pos, Arrow arrow, StringBuilder out) {
        LabelResult above = readOptionalLabel(input, pos);
        if (above == null) {
            appendRelation(out, arrow.latex);
            return pos;
        }

        LabelResult below = readOptionalLabel(input, above.next);
        String labeled;
        if (arrow.xCommand != null) {
            labeled = "\\" + arrow.xCommand;
            if (below != null) {
                labeled += "[" + convertLabel(below.label) + "]";
                pos = below.next;
            } else {
                pos = above.next;
            }
            labeled += "{" + convertLabel(above.label) + "}";
        } else {
            labeled = "\\overset{" + convertLabel(above.label) + "}{" + arrow.latex + "}";
            if (below != null) {
                labeled = "\\underset{" + convertLabel(below.label) + "}{" + labeled + "}";
                pos = below.next;
            } else {
                pos = above.next;
            }
        }
        appendRelation(out, labeled);
        return pos;
    }

    private static String convertFormulaToken(String token) {
        if (isTextGroup(token)) {
            return "\\text{" + token.substring(1, token.length() - 1) + "}";
        }
        if (isStateOfAggregation(token)) {
            return "\\mathrm{" + token + "}";
        }

        StringBuilder out = new StringBuilder();
        StringBuilder roman = new StringBuilder();
        boolean maySubscript = false;

        int pos = 0;
        AmountResult amount = readAmount(token, pos);
        if (amount != null) {
            out.append(convertAmount(amount.value)).append("\\,");
            pos = amount.next;
        }

        while (pos < token.length()) {
            char ch = token.charAt(pos);
            if (Character.isLetter(ch)) {
                roman.append(ch);
                pos++;
                maySubscript = true;
                continue;
            }

            flushRoman(out, roman);

            if (Character.isDigit(ch)) {
                int end = readDigits(token, pos);
                String digits = token.substring(pos, end);
                if (maySubscript) {
                    out.append("_{").append(digits).append('}');
                } else {
                    out.append(digits);
                }
                pos = end;
                maySubscript = true;
            } else if (ch == '^' || ch == '_') {
                ScriptResult script = readScript(token, pos + 1);
                out.append(ch).append('{').append(convertScript(script.value)).append('}');
                pos = script.next;
                maySubscript = true;
            } else if (ch == '+' || ch == '-') {
                int end = readSigns(token, pos);
                out.append("^{").append(formatCharge(token.substring(pos, end))).append('}');
                pos = end;
                maySubscript = true;
            } else if (ch == '.') {
                int end = readDots(token, pos);
                out.append(end - pos > 1 ? "\\ldots " : "\\cdot ");
                pos = end;
                maySubscript = false;
            } else if (ch == '$') {
                GroupResult math = readDelimited(token, pos, '$', '$');
                out.append(math.value);
                pos = math.next;
                maySubscript = true;
            } else if (ch == '\\') {
                CommandResult command = readCommandWithOptionalGroup(token, pos);
                out.append(convertCommand(command.name, command.group, token.substring(pos, command.next)));
                pos = command.next;
                maySubscript = command.group != null || command.name.length() > 0;
            } else if (ch == '(' || ch == '[') {
                GroupResult group = readBalanced(token, pos, ch, ch == '(' ? ')' : ']');
                out.append(convertBracketGroup(ch, group.value));
                pos = group.next;
                maySubscript = true;
            } else {
                out.append(escape(ch));
                pos++;
                maySubscript = isClosingGroup(ch);
            }
        }

        flushRoman(out, roman);
        return out.toString();
    }

    private static String convertLabel(String label) {
        String converted = convert(label);
        return converted.length() == 0 ? "{}" : converted;
    }

    private static String convertScript(String script) {
        if (isSingleVariable(script) || isGreekCommand(script)) {
            return script;
        }

        StringBuilder out = new StringBuilder();
        StringBuilder roman = new StringBuilder();
        for (int i = 0; i < script.length(); i++) {
            char ch = script.charAt(i);
            if (Character.isLetter(ch)) {
                roman.append(ch);
            } else if (ch == '$') {
                flushRoman(out, roman);
                GroupResult math = readDelimited(script, i, '$', '$');
                out.append(math.value);
                i = math.next - 1;
            } else if (ch == '\\') {
                flushRoman(out, roman);
                CommandResult command = readCommandWithOptionalGroup(script, i);
                out.append(convertCommand(command.name, command.group, script.substring(i, command.next)));
                i = command.next - 1;
            } else {
                flushRoman(out, roman);
                out.append(escape(ch));
            }
        }
        flushRoman(out, roman);
        return out.toString();
    }

    private static int readFormulaTokenEnd(String input, int start) {
        int pos = start;
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (Character.isWhitespace(ch)
                    || ch == '='
                    || matchArrow(input, pos) != null
                    || matchOperator(input, pos) != null
                    || matchUpDown(input, pos) != null) {
                break;
            }
            if (ch == '{') {
                pos = readBalanced(input, pos, '{', '}').next;
                continue;
            }
            if (ch == '[') {
                pos = readBalanced(input, pos, '[', ']').next;
                continue;
            }
            if (ch == '(') {
                pos = readBalanced(input, pos, '(', ')').next;
                continue;
            }
            if (ch == '$') {
                pos = readDelimited(input, pos, '$', '$').next;
                continue;
            }
            if (ch == '\\') {
                pos = readCommandWithOptionalGroup(input, pos).next;
                continue;
            }
            if (ch == '+' || ch == '-') {
                int end = readSigns(input, pos);
                if (isTokenBoundary(input, end)) {
                    pos = end;
                }
                break;
            }
            pos++;
        }
        return pos;
    }

    private static boolean isTokenBoundary(String input, int pos) {
        return pos >= input.length()
                || Character.isWhitespace(input.charAt(pos))
                || input.charAt(pos) == '='
                || isStateOfAggregationAt(input, pos)
                || matchArrow(input, pos) != null;
    }

    private static Arrow matchArrow(String input, int pos) {
        if (startsWith(input, pos, "<-->")) {
            return new Arrow("<-->", "\\longleftrightarrow");
        }
        if (startsWith(input, pos, "<=>>")) {
            return new Arrow("<=>>", "\\longrightleftharpoons");
        }
        if (startsWith(input, pos, "<<=>")) {
            return new Arrow("<<=>", "\\longleftrightharpoons");
        }
        if (startsWith(input, pos, "<=>")) {
            return new Arrow("<=>", "\\longrightleftharpoons");
        }
        if (startsWith(input, pos, "<->")) {
            return new Arrow("<->", "\\longleftrightarrow");
        }
        if (startsWith(input, pos, "->")) {
            return new Arrow("->", "\\longrightarrow", "xrightarrow");
        }
        if (startsWith(input, pos, "<-")) {
            return new Arrow("<-", "\\longleftarrow", "xleftarrow");
        }
        if (startsWith(input, pos, "=>")) {
            return new Arrow("=>", "\\Longrightarrow");
        }
        if (startsWith(input, pos, "<=")) {
            return new Arrow("<=", "\\Longleftarrow");
        }
        if (startsWith(input, pos, "\u2192") || startsWith(input, pos, "\u27f6")) {
            return new Arrow(input.substring(pos, pos + 1), "\\longrightarrow", "xrightarrow");
        }
        if (startsWith(input, pos, "\u21cc")) {
            return new Arrow("\u21cc", "\\longrightleftharpoons");
        }
        return null;
    }

    private static String matchOperator(String input, int pos) {
        if (startsWith(input, pos, "\\pm")) {
            return "\\pm";
        }
        if (startsWith(input, pos, "$\\pm$")) {
            return "\\pm";
        }
        if (startsWith(input, pos, "+-") || startsWith(input, pos, "+/-")) {
            return "\\pm";
        }
        if (startsWith(input, pos, "\\approx")) {
            return "\\approx";
        }
        if (startsWith(input, pos, "$\\approx$")) {
            return "\\approx";
        }
        if (startsWith(input, pos, "<<")) {
            return "\\ll";
        }
        if (startsWith(input, pos, ">>")) {
            return "\\gg";
        }
        return null;
    }

    private static String matchUpDown(String input, int pos) {
        if (startsWith(input, pos, "(v)")) {
            return "\\downarrow";
        }
        if (startsWith(input, pos, "(^)")) {
            return "\\uparrow";
        }
        if (startsWith(input, pos, "v") && isTokenBoundary(input, pos + 1)) {
            return "\\downarrow";
        }
        if (startsWith(input, pos, "^") && isTokenBoundary(input, pos + 1)) {
            return "\\uparrow";
        }
        return null;
    }

    private static boolean startsWith(String input, int pos, String token) {
        return pos + token.length() <= input.length() && input.startsWith(token, pos);
    }

    private static int skipWhiteSpace(String input, int pos) {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private static int readDigits(String input, int pos) {
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private static int readSigns(String input, int pos) {
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (ch != '+' && ch != '-') {
                break;
            }
            pos++;
        }
        return pos;
    }

    private static int readDots(String input, int pos) {
        while (pos < input.length() && input.charAt(pos) == '.') {
            pos++;
        }
        return pos;
    }

    private static CommandResult readCommandWithOptionalGroup(String input, int pos) {
        int start = pos;
        pos++;
        while (pos < input.length() && Character.isLetter(input.charAt(pos))) {
            pos++;
        }

        String name = input.substring(start + 1, pos);
        if (pos < input.length() && input.charAt(pos) == '{') {
            GroupResult group = readBalanced(input, pos, '{', '}');
            return new CommandResult(name, group.value, group.next);
        }
        return new CommandResult(name, null, pos);
    }

    private static ScriptResult readScript(String input, int pos) {
        if (pos >= input.length()) {
            return new ScriptResult("", pos);
        }

        if (input.charAt(pos) == '{') {
            int depth = 1;
            int start = pos + 1;
            pos++;
            while (pos < input.length() && depth > 0) {
                char ch = input.charAt(pos);
                if (ch == '{') {
                    depth++;
                } else if (ch == '}') {
                    depth--;
                }
                pos++;
            }
            int end = depth == 0 ? pos - 1 : input.length();
            return new ScriptResult(input.substring(start, end), pos);
        }

        if (input.charAt(pos) == '\\') {
            CommandResult command = readCommandWithOptionalGroup(input, pos);
            return new ScriptResult(input.substring(pos, command.next), command.next);
        }

        int start = pos;
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (!Character.isLetterOrDigit(ch) && ch != '+' && ch != '-') {
                break;
            }
            pos++;
        }
        if (pos == start) {
            pos++;
        }
        return new ScriptResult(input.substring(start, pos), pos);
    }

    private static GroupResult readBalanced(String input, int pos, char open, char close) {
        int depth = 1;
        int start = pos + 1;
        pos++;
        while (pos < input.length() && depth > 0) {
            char ch = input.charAt(pos);
            if (ch == open) {
                depth++;
            } else if (ch == close) {
                depth--;
            }
            pos++;
        }

        int end = depth == 0 ? pos - 1 : input.length();
        return new GroupResult(input.substring(start, end), pos);
    }

    private static GroupResult readDelimited(String input, int pos, char open, char close) {
        int start = pos + 1;
        pos++;
        while (pos < input.length() && input.charAt(pos) != close) {
            pos++;
        }
        int end = pos;
        if (pos < input.length()) {
            pos++;
        }
        return new GroupResult(input.substring(start, end), pos);
    }

    private static LabelResult readOptionalLabel(String input, int pos) {
        pos = skipWhiteSpace(input, pos);
        if (pos >= input.length() || input.charAt(pos) != '[') {
            return null;
        }

        int depth = 1;
        int start = pos + 1;
        pos++;
        while (pos < input.length() && depth > 0) {
            char ch = input.charAt(pos);
            if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
            }
            pos++;
        }

        int end = depth == 0 ? pos - 1 : input.length();
        return new LabelResult(input.substring(start, end), pos);
    }

    private static void flushRoman(StringBuilder out, StringBuilder roman) {
        if (roman.length() == 0) {
            return;
        }
        out.append("\\mathrm{").append(roman).append('}');
        roman.setLength(0);
    }

    private static String convertCommand(String name, String group, String fallback) {
        if ("bond".equals(name) && group != null) {
            return convertBond(group);
        }
        if ("ce".equals(name) && group != null) {
            return convert(group);
        }
        if ("ca".equals(name)) {
            return "\\sim ";
        }
        if (group != null) {
            return "\\" + name + "{" + group + "}";
        }
        return fallback;
    }

    private static String convertBond(String bond) {
        if ("-".equals(bond) || "1".equals(bond)) {
            return "{-}";
        }
        if ("=".equals(bond) || "2".equals(bond)) {
            return "{=}";
        }
        if ("#".equals(bond) || "3".equals(bond)) {
            return "{\\equiv}";
        }
        if ("...".equals(bond)) {
            return "{\\cdot\\cdot\\cdot}";
        }
        if ("....".equals(bond)) {
            return "{\\cdot\\cdot\\cdot\\cdot}";
        }
        if ("->".equals(bond)) {
            return "{\\rightarrow}";
        }
        if ("<-".equals(bond)) {
            return "{\\leftarrow}";
        }
        if ("~".equals(bond)) {
            return "{\\cdots}";
        }
        return "{-}";
    }

    private static String convertBracketGroup(char open, String value) {
        char close = open == '(' ? ')' : ']';
        if (isStateOfAggregation(open + value + close)) {
            return "\\mathrm{" + open + value + close + "}";
        }
        return escape(open) + convert(value) + escape(close);
    }

    private static AmountResult readAmount(String token, int pos) {
        if (pos >= token.length()) {
            return null;
        }

        int start = pos;
        boolean parenthesizedFraction = false;
        if (token.charAt(pos) == '(') {
            int close = token.indexOf(')', pos);
            if (close > pos && isFraction(token.substring(pos + 1, close))) {
                pos = close + 1;
                parenthesizedFraction = true;
            }
        } else {
            if (token.charAt(pos) == '+' || token.charAt(pos) == '-') {
                pos++;
            }
            int before = pos;
            while (pos < token.length() && Character.isDigit(token.charAt(pos))) {
                pos++;
            }
            if (pos < token.length() && (token.charAt(pos) == '.' || token.charAt(pos) == ',')) {
                pos++;
                while (pos < token.length() && Character.isDigit(token.charAt(pos))) {
                    pos++;
                }
            } else if (pos < token.length() && token.charAt(pos) == '/') {
                pos++;
                while (pos < token.length() && Character.isDigit(token.charAt(pos))) {
                    pos++;
                }
            } else if (pos < token.length() && pos == before && Character.isLowerCase(token.charAt(pos))) {
                pos++;
            }
        }

        if (pos == start || pos >= token.length()) {
            return null;
        }
        if (!parenthesizedFraction && !isFormulaStart(token.charAt(pos))) {
            return null;
        }

        return new AmountResult(token.substring(start, pos), pos);
    }

    private static String convertAmount(String amount) {
        if (amount.startsWith("(") && amount.endsWith(")") && isFraction(amount.substring(1, amount.length() - 1))) {
            amount = amount.substring(1, amount.length() - 1);
        }
        if (isFraction(amount)) {
            int slash = amount.indexOf('/');
            return "\\frac{" + amount.substring(0, slash) + "}{" + amount.substring(slash + 1) + "}";
        }
        if (amount.length() == 1 && Character.isLowerCase(amount.charAt(0))) {
            return Character.toString(amount.charAt(0));
        }
        return amount;
    }

    private static boolean isFormulaStart(char ch) {
        return Character.isUpperCase(ch) || ch == '\\' || ch == '(' || ch == '[' || ch == '{';
    }

    private static boolean isFraction(String value) {
        int slash = value.indexOf('/');
        if (slash <= 0 || slash >= value.length() - 1) {
            return false;
        }
        return isDigits(value.substring(0, slash)) && isDigits(value.substring(slash + 1));
    }

    private static boolean isDigits(String value) {
        if (value.length() == 0) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTextGroup(String token) {
        return token.length() >= 2 && token.charAt(0) == '{' && token.charAt(token.length() - 1) == '}';
    }

    private static boolean isStateOfAggregation(String token) {
        if (token.length() < 3 || token.charAt(0) != '(' || token.charAt(token.length() - 1) != ')') {
            return false;
        }
        String value = token.substring(1, token.length() - 1);
        if (value.length() > 3 || value.length() == 0) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isLowerCase(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isStateOfAggregationAt(String input, int pos) {
        if (pos >= input.length() || input.charAt(pos) != '(') {
            return false;
        }
        GroupResult group = readBalanced(input, pos, '(', ')');
        return isStateOfAggregation("(" + group.value + ")");
    }

    private static boolean isSingleVariable(String script) {
        return script.length() == 1 && Character.isLowerCase(script.charAt(0));
    }

    private static boolean isGreekCommand(String script) {
        return script.startsWith("\\") && script.length() > 1;
    }

    private static void appendSpace(StringBuilder out) {
        if (out.length() != 0 && !endsWithSpace(out)) {
            out.append("\\ ");
        }
    }

    private static boolean endsWithSpace(StringBuilder out) {
        int len = out.length();
        return len >= 2 && out.charAt(len - 2) == '\\' && out.charAt(len - 1) == ' ';
    }

    private static void appendBinary(StringBuilder out, String op) {
        appendSpace(out);
        out.append("\\mathbin{").append(op).append('}');
        out.append("\\ ");
    }

    private static void appendRelation(StringBuilder out, String relation) {
        appendSpace(out);
        out.append(relation);
        out.append("\\ ");
    }

    private static String formatCharge(String signs) {
        if (signs.length() <= 1) {
            return signs;
        }

        char sign = signs.charAt(0);
        for (int i = 1; i < signs.length(); i++) {
            if (signs.charAt(i) != sign) {
                return signs;
            }
        }
        return signs.length() + Character.toString(sign);
    }

    private static boolean isClosingGroup(char ch) {
        return ch == ')' || ch == ']' || ch == '}';
    }

    private static String escape(char ch) {
        switch (ch) {
        case '{':
            return "\\{";
        case '}':
            return "\\}";
        case '%':
            return "\\%";
        case '&':
            return "\\&";
        case '#':
            return "\\#";
        case '_':
            return "\\_";
        case '[':
            return "[";
        case ']':
            return "]";
        case '(':
            return "(";
        case ')':
            return ")";
        default:
            return Character.toString(ch);
        }
    }

    private static final class Arrow {
        final String token;
        final String latex;
        final String xCommand;

        Arrow(String token, String latex) {
            this(token, latex, null);
        }

        Arrow(String token, String latex, String xCommand) {
            this.token = token;
            this.latex = latex;
            this.xCommand = xCommand;
        }
    }

    private static final class ScriptResult {
        final String value;
        final int next;

        ScriptResult(String value, int next) {
            this.value = value;
            this.next = next;
        }
    }

    private static final class GroupResult {
        final String value;
        final int next;

        GroupResult(String value, int next) {
            this.value = value;
            this.next = next;
        }
    }

    private static final class CommandResult {
        final String name;
        final String group;
        final int next;

        CommandResult(String name, String group, int next) {
            this.name = name;
            this.group = group;
            this.next = next;
        }
    }

    private static final class AmountResult {
        final String value;
        final int next;

        AmountResult(String value, int next) {
            this.value = value;
            this.next = next;
        }
    }

    private static final class LabelResult {
        final String label;
        final int next;

        LabelResult(String label, int next) {
            this.label = label;
            this.next = next;
        }
    }
}
