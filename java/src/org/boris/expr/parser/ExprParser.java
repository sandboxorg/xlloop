/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Peter Smith
 *******************************************************************************/
package org.boris.expr.parser;

import java.io.IOException;
import java.util.ArrayList;

import org.boris.expr.Expr;
import org.boris.expr.ExprAddition;
import org.boris.expr.ExprArray;
import org.boris.expr.ExprDivision;
import org.boris.expr.ExprDouble;
import org.boris.expr.ExprEqual;
import org.boris.expr.ExprException;
import org.boris.expr.ExprExpression;
import org.boris.expr.ExprFunction;
import org.boris.expr.ExprGreaterThan;
import org.boris.expr.ExprGreaterThanOrEqualTo;
import org.boris.expr.ExprInteger;
import org.boris.expr.ExprLessThan;
import org.boris.expr.ExprLessThanOrEqualTo;
import org.boris.expr.ExprMissing;
import org.boris.expr.ExprMultiplication;
import org.boris.expr.ExprNotEqual;
import org.boris.expr.ExprPower;
import org.boris.expr.ExprString;
import org.boris.expr.ExprStringConcat;
import org.boris.expr.ExprSubtraction;
import org.boris.expr.ExprVariable;
import org.boris.expr.IBinaryOperator;
import org.boris.expr.IEvaluationCallback;

public class ExprParser
{
    private Expr current;
    private IParserVisitor visitor;

    public static Expr parse(String text, IEvaluationCallback callback)
            throws IOException, ExprException {
        ExprParser p = new ExprParser();
        p.parse(new ExprLexer(text), callback);
        return p.get();
    }

    public void setParserVisitor(IParserVisitor visitor) {
        this.visitor = visitor;
    }

    public void parse(ExprLexer lexer, IEvaluationCallback callback)
            throws IOException, ExprException {
        ExprToken e = null;
        while ((e = lexer.next()) != null) {
            parseToken(lexer, callback, e);
        }
    }

    private void parseToken(ExprLexer lexer, IEvaluationCallback callback,
            ExprToken token) throws ExprException, IOException {
        switch (token.type) {
        case Plus:
        case Minus:
        case Multiply:
        case Divide:
        case Power:
        case StringConcat:
        case LessThan:
        case LessThanOrEqualTo:
        case GreaterThan:
        case GreaterThanOrEqualTo:
        case Equal:
        case NotEqual:
            parseOperator(token);
            break;
        case Decimal:
        case Integer:
        case String:
        case Variable:
            parseValue(token, callback);
            break;
        case OpenBracket:
            parseExpression(lexer, callback);
            break;
        case Function:
            parseFunction(token, lexer, callback);
            break;
        case OpenBrace:
            parseArray(lexer, callback);
            break;
        default:
            throw new ExprException("Unexpected " + token.type + " found");
        }
    }

    private void parseFunction(ExprToken token, ExprLexer lexer,
            IEvaluationCallback callback) throws ExprException, IOException {
        Expr c = current;
        current = null;
        ExprToken e = null;
        ArrayList args = new ArrayList();
        while ((e = lexer.next()) != null) {
            if (e.type.equals(ExprTokenType.Comma)) {
                if (current == null)
                    args.add(ExprMissing.MISSING);
                else
                    args.add(current);
                current = null;
            } else if (e.type.equals(ExprTokenType.CloseBracket)) {
                if (current != null)
                    args.add(current);
                current = c;
                break;
            } else {
                parseToken(lexer, callback, e);
            }
        }

        ExprFunction f = new ExprFunction(callback, token.val, (Expr[]) args
                .toArray(new Expr[0]));

        if (visitor != null)
            visitor.annotateFunction(f);

        setValue(f);
    }

    private void parseExpression(ExprLexer lexer, IEvaluationCallback callback)
            throws IOException, ExprException {
        Expr c = current;
        current = null;
        ExprToken e = null;
        while ((e = lexer.next()) != null) {
            if (e.type.equals(ExprTokenType.CloseBracket)) {
                Expr t = current;
                current = c;
                setValue(new ExprExpression(t));
                break;
            } else {
                parseToken(lexer, callback, e);
            }
        }
    }

    private void parseArray(ExprLexer lexer, IEvaluationCallback callback)
            throws ExprException, IOException {
        Expr c = current;
        current = null;
        ExprToken e = null;
        ArrayList args = new ArrayList();
        while ((e = lexer.next()) != null) {
            if (e.type.equals(ExprTokenType.Comma)) {
                if (current == null)
                    args.add(ExprMissing.MISSING);
                else
                    args.add(current);
                current = null;
            } else if (e.type.equals(ExprTokenType.CloseBrace)) {
                if (current != null)
                    args.add(current);
                current = c;
                break;
            } else {
                parseToken(lexer, callback, e);
            }
        }

        ExprArray a = new ExprArray(1, args.size());
        for (int i = 0; i < args.size(); i++) {
            a.set(0, i, (Expr) args.get(i));
        }

        setValue(a);
    }

    private void parseValue(ExprToken e, IEvaluationCallback callback)
            throws ExprException {
        Expr value = null;
        switch (e.type) {
        case Decimal:
            value = new ExprDouble(e.doubleValue);
            break;
        case Integer:
            value = new ExprInteger(e.integerValue);
            break;
        case String:
            value = new ExprString(e.val);
            break;
        case Variable:
            value = new ExprVariable(callback, e.val);
            if (visitor != null)
                visitor.annotateVariable((ExprVariable) value);
            break;
        }
        setValue(value);
    }

    private void setValue(Expr value) throws ExprException {
        if (current == null) {
            current = value;
            return;
        } else {
            Expr c = current;
            do {
                if (!(c instanceof IBinaryOperator))
                    throw new ExprException("Expected operator not found");

                Expr rhs = ((IBinaryOperator) c).getRHS();
                if (rhs == null) {
                    ((IBinaryOperator) c).setRHS(value);
                    return;
                } else {
                    c = rhs;
                }
            } while (c != null);

            throw new ExprException("Unexpected token found");
        }
    }

    private void parseOperator(ExprToken e) throws ExprException {
        switch (e.type) {
        case Plus:
            Expr lhs = current;
            current = new ExprAddition(lhs, null);
            break;
        case Minus:
            lhs = current;
            current = new ExprSubtraction(lhs, null);
            break;
        case Multiply:
            parseMultiplyDivide(new ExprMultiplication(null, null));
            break;
        case Divide:
            parseMultiplyDivide(new ExprDivision(null, null));
            break;
        case Power:
            parseMultiplyDivide(new ExprPower(null, null));
            break;
        case StringConcat:
            parseMultiplyDivide(new ExprStringConcat(null, null));
            break;
        case LessThan:
            current = new ExprLessThan(current, null);
            break;
        case LessThanOrEqualTo:
            current = new ExprLessThanOrEqualTo(current, null);
            break;
        case GreaterThan:
            current = new ExprGreaterThan(current, null);
            break;
        case GreaterThanOrEqualTo:
            current = new ExprGreaterThanOrEqualTo(current, null);
            break;
        case NotEqual:
            current = new ExprNotEqual(current, null);
            break;
        case Equal:
            current = new ExprEqual(current, null);
            break;
        default:
            throw new ExprException("Unhandled operator type: " + e.type);
        }
    }

    private void parseMultiplyDivide(IBinaryOperator md) throws ExprException {
        if (current == null)
            throw new ExprException("Unexpected null token");

        Expr c = current;
        Expr prev = null;
        while (c != null) {
            if (c instanceof ExprAddition || c instanceof ExprSubtraction) {
                prev = c;
                c = ((IBinaryOperator) c).getRHS();
            } else {
                if (prev == null) {
                    md.setLHS(current);
                    current = (Expr) md;
                    break;
                } else {
                    IBinaryOperator b = (IBinaryOperator) prev;
                    md.setLHS(b.getRHS());
                    b.setRHS((Expr) md);
                    break;
                }
            }
        }
    }

    public Expr get() {
        return current;
    }
}
