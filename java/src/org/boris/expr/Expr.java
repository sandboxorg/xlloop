/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Peter Smith
 *******************************************************************************/
package org.boris.expr;

import org.boris.variant.Variant;

public abstract class Expr
{
    public final ExprType type;
    public final boolean evaluatable;

    Expr(ExprType type, boolean evaluatable) {
        this.type = type;
        this.evaluatable = evaluatable;
    }

    public boolean isVolatile() {
        return false;
    }

    public void validate() throws ExprException {
    }

    public Expr optimize() throws ExprException {
        return this;
    }

    public abstract Variant encode();

    protected Expr eval(Expr expr) throws ExprException {
        if (expr instanceof ExprEvaluatable) {
            return ((ExprEvaluatable) expr).evaluate();
        }
        return expr;
    }

    protected ExprBoolean bool(boolean bool) {
        return bool ? ExprBoolean.TRUE : ExprBoolean.FALSE;
    }
}
