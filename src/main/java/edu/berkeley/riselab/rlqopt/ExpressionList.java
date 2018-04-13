package edu.berkeley.riselab.rlqopt;

import java.util.Collections;
import java.util.LinkedList;

/** Convenience class to represent a list of expressions. */
public class ExpressionList extends LinkedList<Expression> {

  public static ExpressionList of(ExpressionList... args) {
    ExpressionList result = new ExpressionList();
    for (ExpressionList list : args) {
      result.addAll(list);
    }
    return result;
  }

  // create it explicitly named
  public ExpressionList(Expression... args) {
    super();
    Collections.addAll(this, args);
  }

  // noop loading an attribute
  public ExpressionList(Relation r) {

    super();

    for (String attr : r) this.add(r.get(attr).getExpression());
  }

  public LinkedList<Attribute> getAllVisibleAttributes() {

    LinkedList<Attribute> visibleAttrs = new LinkedList<Attribute>();
    for (Expression e : this) visibleAttrs.addAll(e.getVisibleAttributes());

    return visibleAttrs;
  }
}
