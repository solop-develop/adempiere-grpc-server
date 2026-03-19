/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.spin.hr.util;

import org.adempiere.exceptions.AdempiereException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Payroll Expression Evaluator — parses and evaluates mathematical/conditional
 * expressions for embedded payroll calculation in HR_Attribute.
 *
 * Supported operations:
 *   Arithmetic  : +, -, *, /, %, () , unary minus
 *   Numeric     : ROUND(n,d), ABS(n), FLOOR(n), CEIL(n), MIN(a,b), MAX(a,b)
 *   Conditional : IF(cond, then, else), COALESCE(a,b,...), NVL(val, default)
 *   String      : UPPER(s), LOWER(s), TRIM(s), CONCAT(a,b,...), LENGTH(s)
 *   Date        : TODAY(), DATEDIFF(d1, d2)
 *   Conversion  : TONUMBER(s), TOSTRING(v)
 *   Variables   : UPPERCASE identifiers resolved from context map
 *
 * @author Solop SP014
 */
public class PayrollExpressionEvaluator {

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Evaluates an expression using the provided scriptCtx as variable source.
     *
     * @param expression expression string
     * @param context    script context (keys are variable names)
     * @return evaluation result (Number, String, Boolean, Timestamp, or null)
     */
    public static Object evaluate(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().isEmpty())
            return null;
        Tokenizer tokenizer = new Tokenizer(expression.trim());
        List<Token> tokens = tokenizer.tokenize();
        Parser parser = new Parser(tokens, context);
        return parser.parseExpression();
    }

    /**
     * Validates expression syntax without executing it (uses an empty context).
     *
     * @param expression expression string
     * @return null if valid, error message otherwise
     */
    public static String validateSyntax(String expression) {
        try {
            evaluate(expression, java.util.Collections.emptyMap());
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // -----------------------------------------------------------------------
    // Token types
    // -----------------------------------------------------------------------

    private enum TokenType {
        NUMBER, STRING, IDENTIFIER, LPAREN, RPAREN, COMMA,
        PLUS, MINUS, STAR, SLASH, PERCENT,
        EQ, NEQ, LT, LTE, GT, GTE,
        AND, OR, NOT,
        EOF
    }

    private static class Token {
        final TokenType type;
        final String value;

        Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return type + "(" + value + ")";
        }
    }

    // -----------------------------------------------------------------------
    // Tokenizer
    // -----------------------------------------------------------------------

    private static class Tokenizer {
        private final String src;
        private int pos;

        Tokenizer(String src) {
            this.src = src;
            this.pos = 0;
        }

        List<Token> tokenize() {
            List<Token> tokens = new ArrayList<>();
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (Character.isWhitespace(c)) {
                    pos++;
                } else if (c == '(') {
                    tokens.add(new Token(TokenType.LPAREN, "("));
                    pos++;
                } else if (c == ')') {
                    tokens.add(new Token(TokenType.RPAREN, ")"));
                    pos++;
                } else if (c == ',') {
                    tokens.add(new Token(TokenType.COMMA, ","));
                    pos++;
                } else if (c == '+') {
                    tokens.add(new Token(TokenType.PLUS, "+"));
                    pos++;
                } else if (c == '-') {
                    tokens.add(new Token(TokenType.MINUS, "-"));
                    pos++;
                } else if (c == '*') {
                    tokens.add(new Token(TokenType.STAR, "*"));
                    pos++;
                } else if (c == '/') {
                    tokens.add(new Token(TokenType.SLASH, "/"));
                    pos++;
                } else if (c == '%') {
                    tokens.add(new Token(TokenType.PERCENT, "%"));
                    pos++;
                } else if (c == '!' && peek() == '=') {
                    tokens.add(new Token(TokenType.NEQ, "!="));
                    pos += 2;
                } else if (c == '<' && peek() == '=') {
                    tokens.add(new Token(TokenType.LTE, "<="));
                    pos += 2;
                } else if (c == '>' && peek() == '=') {
                    tokens.add(new Token(TokenType.GTE, ">="));
                    pos += 2;
                } else if (c == '<') {
                    tokens.add(new Token(TokenType.LT, "<"));
                    pos++;
                } else if (c == '>') {
                    tokens.add(new Token(TokenType.GT, ">"));
                    pos++;
                } else if (c == '=' && peek() == '=') {
                    tokens.add(new Token(TokenType.EQ, "=="));
                    pos += 2;
                } else if (c == '=') {
                    tokens.add(new Token(TokenType.EQ, "="));
                    pos++;
                } else if (c == '\'' || c == '"') {
                    tokens.add(readString(c));
                } else if (Character.isDigit(c) || (c == '.' && Character.isDigit(peek()))) {
                    tokens.add(readNumber());
                } else if (Character.isLetter(c) || c == '_') {
                    tokens.add(readIdentifier());
                } else {
                    throw new AdempiereException("Unexpected character: '" + c + "' at position " + pos);
                }
            }
            tokens.add(new Token(TokenType.EOF, ""));
            return tokens;
        }

        private char peek() {
            return (pos + 1 < src.length()) ? src.charAt(pos + 1) : 0;
        }

        private Token readString(char quote) {
            pos++; // skip opening quote
            StringBuilder sb = new StringBuilder();
            while (pos < src.length() && src.charAt(pos) != quote) {
                if (src.charAt(pos) == '\\' && pos + 1 < src.length()) {
                    pos++;
                    char esc = src.charAt(pos);
                    switch (esc) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        default:  sb.append(esc);
                    }
                } else {
                    sb.append(src.charAt(pos));
                }
                pos++;
            }
            pos++; // skip closing quote
            return new Token(TokenType.STRING, sb.toString());
        }

        private Token readNumber() {
            int start = pos;
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.'))
                pos++;
            return new Token(TokenType.NUMBER, src.substring(start, pos));
        }

        private Token readIdentifier() {
            int start = pos;
            while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_'))
                pos++;
            String word = src.substring(start, pos);
            // Logical keywords
            if ("AND".equalsIgnoreCase(word)) return new Token(TokenType.AND, word);
            if ("OR".equalsIgnoreCase(word))  return new Token(TokenType.OR,  word);
            if ("NOT".equalsIgnoreCase(word)) return new Token(TokenType.NOT, word);
            return new Token(TokenType.IDENTIFIER, word);
        }
    }

    // -----------------------------------------------------------------------
    // Recursive-descent parser / evaluator
    // -----------------------------------------------------------------------

    private static class Parser {
        private final List<Token> tokens;
        private final Map<String, Object> context;
        private int pos;

        Parser(List<Token> tokens, Map<String, Object> context) {
            this.tokens  = tokens;
            this.context = context;
            this.pos     = 0;
        }

        private Token current() { return tokens.get(pos); }
        private Token consume() { return tokens.get(pos++); }

        private Token expect(TokenType type) {
            Token t = consume();
            if (t.type != type)
                throw new AdempiereException("Expected " + type + " but got " + t);
            return t;
        }

        private boolean match(TokenType type) {
            if (current().type == type) { pos++; return true; }
            return false;
        }

        // Expression → OR-expr
        Object parseExpression() {
            return parseOr();
        }

        // OR → AND (OR AND)*
        private Object parseOr() {
            Object left = parseAnd();
            while (current().type == TokenType.OR) {
                consume();
                Object right = parseAnd();
                left = isTruthy(left) || isTruthy(right);
            }
            return left;
        }

        // AND → comparison (AND comparison)*
        private Object parseAnd() {
            Object left = parseComparison();
            while (current().type == TokenType.AND) {
                consume();
                Object right = parseComparison();
                left = isTruthy(left) && isTruthy(right);
            }
            return left;
        }

        // comparison → additive ((==|!=|<|<=|>|>=) additive)?
        private Object parseComparison() {
            Object left = parseAdditive();
            TokenType op = current().type;
            if (op == TokenType.EQ || op == TokenType.NEQ
                    || op == TokenType.LT || op == TokenType.LTE
                    || op == TokenType.GT || op == TokenType.GTE) {
                consume();
                Object right = parseAdditive();
                return compare(left, right, op);
            }
            return left;
        }

        // additive → multiplicative ((+|-) multiplicative)*
        private Object parseAdditive() {
            Object left = parseMultiplicative();
            while (current().type == TokenType.PLUS || current().type == TokenType.MINUS) {
                TokenType op = consume().type;
                Object right = parseMultiplicative();
                if (op == TokenType.PLUS) {
                    if (left instanceof String || right instanceof String)
                        left = String.valueOf(left) + String.valueOf(right);
                    else
                        left = toDecimal(left).add(toDecimal(right));
                } else {
                    left = toDecimal(left).subtract(toDecimal(right));
                }
            }
            return left;
        }

        // multiplicative → unary ((*|/|%) unary)*
        private Object parseMultiplicative() {
            Object left = parseUnary();
            while (current().type == TokenType.STAR
                    || current().type == TokenType.SLASH
                    || current().type == TokenType.PERCENT) {
                TokenType op = consume().type;
                Object right = parseUnary();
                BigDecimal l = toDecimal(left);
                BigDecimal r = toDecimal(right);
                if (op == TokenType.STAR)
                    left = l.multiply(r);
                else if (op == TokenType.SLASH)
                    left = l.divide(r, 10, RoundingMode.HALF_UP);
                else
                    left = l.remainder(r);
            }
            return left;
        }

        // unary → (-|NOT)? primary
        private Object parseUnary() {
            if (current().type == TokenType.MINUS) {
                consume();
                Object val = parsePrimary();
                return toDecimal(val).negate();
            }
            if (current().type == TokenType.NOT) {
                consume();
                Object val = parsePrimary();
                return !isTruthy(val);
            }
            return parsePrimary();
        }

        // primary → NUMBER | STRING | IDENTIFIER | function-call | '(' expression ')'
        private Object parsePrimary() {
            Token t = current();

            if (t.type == TokenType.NUMBER) {
                consume();
                return new BigDecimal(t.value);
            }

            if (t.type == TokenType.STRING) {
                consume();
                return t.value;
            }

            if (t.type == TokenType.LPAREN) {
                consume();
                Object val = parseExpression();
                expect(TokenType.RPAREN);
                return val;
            }

            if (t.type == TokenType.IDENTIFIER) {
                consume();
                String name = t.value.toUpperCase();
                String originalName = t.value;

                // built-in functions (always uppercase) or process methods via reflection
                if (current().type == TokenType.LPAREN) {
                    return callFunction(name, originalName);
                }

                // variable look-up: try exact name first, then original case
                if (context.containsKey(name))
                    return context.get(name);
                if (context.containsKey(t.value))
                    return context.get(t.value);

                // boolean literals
                if ("TRUE".equals(name))  return Boolean.TRUE;
                if ("FALSE".equals(name)) return Boolean.FALSE;
                if ("NULL".equals(name))  return null;

                // Named constant: COMMISSION_AMT → getCommissionAmt() (usa dateFrom/dateTo del período)
                if ("COMMISSION_AMT".equals(name)) {
                    Object proc = context.get("engineHelper");
                    if (proc != null) {
                        try {
                            Method m = proc.getClass().getMethod("getCommissionAmt");
                            return m.invoke(proc);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            return null;
                        } catch (NoSuchMethodException | IllegalAccessException ignored) { }
                    }
                    return null;
                }

                // Named constant: EMPLOYEE_MONTHLY_SALARY → getMonthlySalary()
                if ("EMPLOYEE_MONTHLY_SALARY".equals(name)) {
                    Object ruleCtxObj = context.get("ruleContext");
                    if (ruleCtxObj != null) {
                        try {
                            Method getHelper = ruleCtxObj.getClass().getMethod("getEngineHelper");
                            Object helper = getHelper.invoke(ruleCtxObj);
                            if (helper != null) {
                                Method m = helper.getClass().getMethod("getMonthlySalary");
                                return m.invoke(helper);
                            }
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            return null;
                        } catch (NoSuchMethodException | IllegalAccessException ignored) { }
                    }
                    Object proc = context.get("engineHelper");
                    if (proc != null) {
                        try {
                            Method m = proc.getClass().getMethod("getMonthlySalary");
                            return m.invoke(proc);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            return null;
                        } catch (NoSuchMethodException | IllegalAccessException ignored) { }
                    }
                    return null;
                }

                // Named constant: EMPLOYEE_DAILY_SALARY → getDailySalary()
                if ("EMPLOYEE_DAILY_SALARY".equals(name)) {
                    Object ruleCtxObj = context.get("ruleContext");
                    if (ruleCtxObj != null) {
                        try {
                            Method getHelper = ruleCtxObj.getClass().getMethod("getEngineHelper");
                            Object helper = getHelper.invoke(ruleCtxObj);
                            if (helper != null) {
                                Method m = helper.getClass().getMethod("getDailySalary");
                                return m.invoke(helper);
                            }
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            return null;
                        } catch (NoSuchMethodException | IllegalAccessException ignored) { }
                    }
                    Object proc = context.get("engineHelper");
                    if (proc != null) {
                        try {
                            Method m = proc.getClass().getMethod("getDailySalary");
                            return m.invoke(proc);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            return null;
                        } catch (NoSuchMethodException | IllegalAccessException ignored) { }
                    }
                    return null;
                }

                // Check for _LAST suffix (case-insensitive): SALARIO_LAST → getLastConceptValue("SALARIO")
                if (name.endsWith("_LAST")) {
                    String baseName = t.value.substring(0, t.value.length() - 5); // remove "_LAST", preserve original case
                    Object procLast = context.get("engineHelper");
                    if (procLast != null) {
                        try {
                            Method m = procLast.getClass().getMethod("getLastConceptValue", String.class);
                            return m.invoke(procLast, baseName);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            return null;
                        } catch (NoSuchMethodException | IllegalAccessException ignored) { }
                    }
                    return null;
                }

                // _TYPE_SUM: TIPO_A_TYPE_SUM → getConceptType("TIPO_A")
                if (name.endsWith("_TYPE_SUM")) {
                    String baseName = t.value.substring(0, t.value.length() - 9);
                    Object proc = context.get("engineHelper");
                    if (proc != null) {
                        try {
                            Method m = proc.getClass().getMethod("getConceptType", String.class);
                            return m.invoke(proc, baseName);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            return null;
                        } catch (NoSuchMethodException | IllegalAccessException ignored) { }
                    }
                    return null;
                }

                // _GROUP_SUM: CAT_BASICA_GROUP_SUM → getConceptGroup("CAT_BASICA")
                if (name.endsWith("_GROUP_SUM")) {
                    String baseName = t.value.substring(0, t.value.length() - 10);
                    Object proc = context.get("engineHelper");
                    if (proc != null) {
                        try {
                            Method m = proc.getClass().getMethod("getConceptGroup", String.class);
                            return m.invoke(proc, baseName);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            return null;
                        } catch (NoSuchMethodException | IllegalAccessException ignored) { }
                    }
                    return null;
                }

                // _CATEGORY_SUM: CAT_BASICA_CATEGORY_SUM → getConceptCategory("CAT_BASICA")
                if (name.endsWith("_CATEGORY_SUM")) {
                    String baseName = t.value.substring(0, t.value.length() - 13);
                    Object proc = context.get("engineHelper");
                    if (proc != null) {
                        try {
                            Method m = proc.getClass().getMethod("getConceptCategory", String.class);
                            return m.invoke(proc, baseName);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            return null;
                        } catch (NoSuchMethodException | IllegalAccessException ignored) { }
                    }
                    return null;
                }

                // _ATTR: ANTIGUEDAD_ATTR → getAttribute("ANTIGUEDAD")
                if (name.endsWith("_ATTR")) {
                    String baseName = t.value.substring(0, t.value.length() - 5);
                    Object proc = context.get("engineHelper");
                    if (proc != null) {
                        try {
                            Method m = proc.getClass().getMethod("getAttribute", String.class);
                            return m.invoke(proc, baseName);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            return null;
                        } catch (NoSuchMethodException | IllegalAccessException ignored) { }
                    }
                    return null;
                }

                // _INCIDENCE_SUM: ASISTENCIA_INCIDENCE_SUM → getIncidenceSum("ASISTENCIA", _From, _To)
                if (name.endsWith("_INCIDENCE_SUM")) {
                    String baseName = t.value.substring(0, t.value.length() - 14);
                    Object proc = context.get("engineHelper");
                    if (proc != null) {
                        try {
                            Timestamp from = (Timestamp) context.get("_From");
                            Timestamp to   = (Timestamp) context.get("_To");
                            Method m = proc.getClass().getMethod("getIncidenceSum", String.class, Timestamp.class, Timestamp.class);
                            return m.invoke(proc, baseName, from, to);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            return null;
                        } catch (NoSuchMethodException | IllegalAccessException ignored) { }
                    }
                    return null;
                }

                // Fallback: resolve as payroll concept.
                // Prefer ruleContext.getEngineHelper().getConceptValue() when running in
                // ParallelEngine: it reads from ParallelEngine.movements (already computed).
                // process.getConceptValue() reads from MHRProcess.movements which is empty
                // during parallel runs and would always return null/zero.
                Object ruleCtxObj = context.get("ruleContext");
                if (ruleCtxObj != null) {
                    try {
                        Method getHelper = ruleCtxObj.getClass().getMethod("getEngineHelper");
                        Object helper = getHelper.invoke(ruleCtxObj);
                        if (helper != null) {
                            Method m = helper.getClass().getMethod("getConceptValue", String.class);
                            Object val = m.invoke(helper, t.value);
                            if (val != null) return val;
                        }
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // concept not found → fall through
                    } catch (NoSuchMethodException | IllegalAccessException ignored) { }
                }

                // Legacy fallback: process.getConceptValue() (MHRProcess non-parallel path)
                Object proc = context.get("engineHelper");
                if (proc != null) {
                    try {
                        Method m = proc.getClass().getMethod("getConceptValue", String.class);
                        Object val = m.invoke(proc, t.value);
                        if (val != null) return val;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // concept not found or error → fall through to null
                    } catch (NoSuchMethodException ignored) { }
                    catch (IllegalAccessException ignored) { }
                }

                return null; // undefined variable → null
            }

            throw new AdempiereException("Unexpected token: " + t);
        }

        // -----------------------------------------------------------------------
        // Function dispatch
        // -----------------------------------------------------------------------

        private Object callFunction(String name, String originalName) {
            expect(TokenType.LPAREN);
            List<Object> args = new ArrayList<>();
            if (current().type != TokenType.RPAREN) {
                args.add(parseExpression());
                while (match(TokenType.COMMA))
                    args.add(parseExpression());
            }
            expect(TokenType.RPAREN);

            switch (name) {
                // Numeric
                case "ROUND": {
                    require(name, args, 2);
                    int scale = toDecimal(args.get(1)).intValue();
                    return toDecimal(args.get(0)).setScale(scale, RoundingMode.HALF_UP);
                }
                case "ABS":   { require(name, args, 1); return toDecimal(args.get(0)).abs(); }
                case "FLOOR": { require(name, args, 1); return toDecimal(args.get(0)).setScale(0, RoundingMode.FLOOR); }
                case "CEIL":  { require(name, args, 1); return toDecimal(args.get(0)).setScale(0, RoundingMode.CEILING); }
                case "MIN": {
                    require(name, args, 2);
                    BigDecimal a = toDecimal(args.get(0)), b = toDecimal(args.get(1));
                    return a.compareTo(b) <= 0 ? a : b;
                }
                case "MAX": {
                    require(name, args, 2);
                    BigDecimal a = toDecimal(args.get(0)), b = toDecimal(args.get(1));
                    return a.compareTo(b) >= 0 ? a : b;
                }

                // Conditional
                case "IF": {
                    require(name, args, 3);
                    return isTruthy(args.get(0)) ? args.get(1) : args.get(2);
                }
                case "COALESCE": {
                    for (Object arg : args)
                        if (arg != null) return arg;
                    return null;
                }
                case "NVL": {
                    require(name, args, 2);
                    return args.get(0) != null ? args.get(0) : args.get(1);
                }

                // String
                case "UPPER":  { require(name, args, 1); return args.get(0) == null ? null : args.get(0).toString().toUpperCase(); }
                case "LOWER":  { require(name, args, 1); return args.get(0) == null ? null : args.get(0).toString().toLowerCase(); }
                case "TRIM":   { require(name, args, 1); return args.get(0) == null ? null : args.get(0).toString().trim(); }
                case "LENGTH": { require(name, args, 1); return args.get(0) == null ? BigDecimal.ZERO : new BigDecimal(args.get(0).toString().length()); }
                case "CONCAT": {
                    StringBuilder sb = new StringBuilder();
                    for (Object arg : args) if (arg != null) sb.append(arg);
                    return sb.toString();
                }

                // Date
                case "TODAY": {
                    return new Timestamp(System.currentTimeMillis());
                }
                case "DATEDIFF": {
                    require(name, args, 2);
                    LocalDate d1 = toLocalDate(args.get(0));
                    LocalDate d2 = toLocalDate(args.get(1));
                    return new BigDecimal(ChronoUnit.DAYS.between(d1, d2));
                }

                // Conversion
                case "TONUMBER": {
                    require(name, args, 1);
                    if (args.get(0) == null) return BigDecimal.ZERO;
                    try { return new BigDecimal(args.get(0).toString().trim()); }
                    catch (NumberFormatException e) { return BigDecimal.ZERO; }
                }
                case "TOSTRING": {
                    require(name, args, 1);
                    return args.get(0) == null ? null : args.get(0).toString();
                }

                case "LAST": {
                    if (args.size() != 2)
                        throw new AdempiereException("LAST requires exactly 2 arguments: LAST(\"concept\", \"payroll\")");
                    Object proc = context.get("engineHelper");
                    if (proc == null) return null;
                    String conceptCode = args.get(0) == null ? null : args.get(0).toString();
                    if (conceptCode == null) return null;
                    String payrollCode = args.get(1) == null ? null : args.get(1).toString();
                    try {
                        Method m = proc.getClass().getMethod("getLastConceptValue", String.class, String.class);
                        return m.invoke(proc, conceptCode, payrollCode);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        return null;
                    } catch (Exception e) {
                        throw new AdempiereException("Error in LAST: " + e.getMessage());
                    }
                }

                case "COMMISSION": {
                    require(name, args, 1);
                    Object proc = context.get("engineHelper");
                    if (proc == null) return null;
                    String docBasisType = args.get(0) == null ? null : args.get(0).toString();
                    try {
                        Method m = proc.getClass().getMethod("getCommissionAmt", String.class);
                        return m.invoke(proc, docBasisType);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        return null;
                    } catch (NoSuchMethodException | IllegalAccessException ignored) { }
                    return null;
                }
                case "COMMISSION_RANGE": {
                    require(name, args, 3);
                    Object proc = context.get("engineHelper");
                    if (proc == null) return null;
                    Timestamp from = args.get(0) == null ? null : (args.get(0) instanceof Timestamp ? (Timestamp) args.get(0) : Timestamp.valueOf(args.get(0).toString()));
                    Timestamp to   = args.get(1) == null ? null : (args.get(1) instanceof Timestamp ? (Timestamp) args.get(1) : Timestamp.valueOf(args.get(1).toString()));
                    String docBasisType = args.get(2) == null ? null : args.get(2).toString();
                    try {
                        Method m = proc.getClass().getMethod("getCommissionAmt", Timestamp.class, Timestamp.class, String.class);
                        return m.invoke(proc, from, to, docBasisType);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        return null;
                    } catch (NoSuchMethodException | IllegalAccessException ignored) { }
                    return null;
                }

                default:
                    // Fallback: invoke method on process object via reflection
                    Object proc = context.get("engineHelper");
                    if (proc != null)
                        return invokeOnProcess(proc, originalName, args);
                    throw new AdempiereException("Unknown function: " + name);
            }
        }

        private Object invokeOnProcess(Object proc, String methodName, List<Object> args) {
            Method match = null;
            for (Method m : proc.getClass().getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == args.size()) {
                    match = m;
                    break;
                }
            }
            if (match == null)
                throw new AdempiereException("Unknown function: " + methodName
                        + " (no method with " + args.size() + " arg(s) on process)");
            try {
                Object[] coerced = new Object[args.size()];
                Class<?>[] types = match.getParameterTypes();
                for (int i = 0; i < args.size(); i++)
                    coerced[i] = coerceArg(args.get(i), types[i]);
                Object result = match.invoke(proc, coerced);
                if (result instanceof Double || result instanceof Float)
                    return new BigDecimal(result.toString());
                if (result instanceof Integer || result instanceof Long)
                    return new BigDecimal(result.toString());
                return result;
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                throw new AdempiereException("Error invoking " + methodName + ": "
                        + (cause != null ? cause.getMessage() : e.getMessage()));
            } catch (Exception e) {
                throw new AdempiereException("Error invoking " + methodName + ": " + e.getMessage());
            }
        }

        private Object coerceArg(Object arg, Class<?> type) {
            if (arg == null) return null;
            if (type.isInstance(arg)) return arg;
            if ((type == double.class  || type == Double.class)  && arg instanceof Number) return ((Number) arg).doubleValue();
            if ((type == int.class     || type == Integer.class) && arg instanceof Number) return ((Number) arg).intValue();
            if ((type == long.class    || type == Long.class)    && arg instanceof Number) return ((Number) arg).longValue();
            if (type == BigDecimal.class && arg instanceof Number) return new BigDecimal(arg.toString());
            if (type == String.class) return arg.toString();
            return arg;
        }

        // -----------------------------------------------------------------------
        // Helper methods
        // -----------------------------------------------------------------------

        private void require(String fn, List<Object> args, int count) {
            if (args.size() != count)
                throw new AdempiereException(fn + " requires exactly " + count + " argument(s), got " + args.size());
        }

        private BigDecimal toDecimal(Object val) {
            if (val == null) return BigDecimal.ZERO;
            if (val instanceof BigDecimal) return (BigDecimal) val;
            if (val instanceof Number)    return new BigDecimal(val.toString());
            if (val instanceof Boolean)   return ((Boolean) val) ? BigDecimal.ONE : BigDecimal.ZERO;
            try { return new BigDecimal(val.toString().trim()); }
            catch (NumberFormatException e) {
                throw new AdempiereException("Cannot convert to number: " + val);
            }
        }

        private boolean isTruthy(Object val) {
            if (val == null)            return false;
            if (val instanceof Boolean) return (Boolean) val;
            if (val instanceof Number)  return ((Number) val).doubleValue() != 0;
            if (val instanceof String)  return !((String) val).isEmpty();
            return true;
        }

        private boolean compare(Object left, Object right, TokenType op) {
            // null comparisons
            if (left == null && right == null) return op == TokenType.EQ;
            if (left == null || right == null) return op == TokenType.NEQ;

            // Try numeric comparison first
            try {
                BigDecimal l = toDecimal(left), r = toDecimal(right);
                int cmp = l.compareTo(r);
                switch (op) {
                    case EQ:  return cmp == 0;
                    case NEQ: return cmp != 0;
                    case LT:  return cmp <  0;
                    case LTE: return cmp <= 0;
                    case GT:  return cmp >  0;
                    case GTE: return cmp >= 0;
                    default:  return false;
                }
            } catch (AdempiereException ignored) {}

            // String comparison
            int cmp = left.toString().compareTo(right.toString());
            switch (op) {
                case EQ:  return cmp == 0;
                case NEQ: return cmp != 0;
                case LT:  return cmp <  0;
                case LTE: return cmp <= 0;
                case GT:  return cmp >  0;
                case GTE: return cmp >= 0;
                default:  return false;
            }
        }

        private LocalDate toLocalDate(Object val) {
            if (val instanceof Timestamp)
                return ((Timestamp) val).toLocalDateTime().toLocalDate();
            if (val instanceof java.util.Date)
                return ((java.util.Date) val).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            throw new AdempiereException("Cannot convert to date: " + val);
        }
    }
}
