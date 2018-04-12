package edu.berkeley.riselab.rlqopt.opt;

import edu.berkeley.riselab.rlqopt.Attribute;
import edu.berkeley.riselab.rlqopt.Expression;
import edu.berkeley.riselab.rlqopt.ExpressionList;
import edu.berkeley.riselab.rlqopt.Operator;
import edu.berkeley.riselab.rlqopt.Relation;
import edu.berkeley.riselab.rlqopt.relalg.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

public class TableStatisticsModel extends HashMap<Attribute, LinkedList<AttributeStatistics>>
    implements CostModel {

  private double defaultSelectivity = 0.1;

  public TableStatisticsModel() {
    super();
  }

  public void putStats(Attribute a, AttributeStatistics s) {

    if (this.containsKey(a)) {
      LinkedList<AttributeStatistics> hist = this.get(a);
      hist.add(s);
    } else {

      LinkedList<AttributeStatistics> hist = new LinkedList();
      // System.out.println(a + " " + a.hashCode() + " : " + s.cardinality);
      hist.add(s);
      this.put(a, hist);
    }
  }

  private long cardinalityEstimate(Relation r) {

    return cardinalityEstimate(r.attributes());
  }

  private long cardinalityEstimate(Iterable<Attribute> list) {
    long card = 0;
    for (Attribute a : list) {
      long sum = 0;

      if (containsKey(a)) {
        for (AttributeStatistics s : get(a)) sum += s.cardinality;
      }

      // System.out.println(a + " : " + sum);
      // for(Attribute b: this.keySet())
      // System.out.println(a + " " + sum + " " + a.hashCode());

      card = Math.max(card, sum);
    }

    return card;
  }

  public Cost tableAccessOperator(Operator in) {
    ExpressionList el = in.params.expression;
    LinkedList<Attribute> al = el.getAllVisibleAttributes();
    long estimate = cardinalityEstimate(al);
    return new Cost(estimate, estimate, 0);
  }

  public Cost projectOperator(Operator in, Cost costIn) {
    return new Cost(costIn.resultCardinality, costIn.resultCardinality, 0);
  }

  public Cost groupByOperator(Operator in, Cost costIn) {
    LinkedList<Attribute> al = in.params.secondary_expression.getAllVisibleAttributes();
    long estimate = cardinalityEstimate(al);

    return new Cost(costIn.resultCardinality, estimate, 0);
  }

  public Cost selectOperator(Operator in, Cost costIn) {

    double rf = estimateReductionFactor(in.params.expression.get(0));
    return new Cost(costIn.resultCardinality, (long) (rf * costIn.resultCardinality), 0);
  }

  public double estimateReductionFactor(Expression e) {

    if (e.op.equals(Expression.NOT)) return 1.0 - estimateReductionFactor(e.children.get(0));
    else if (e.op.equals(Expression.AND))
      return estimateReductionFactor(e.children.get(0))
          * estimateReductionFactor(e.children.get(1));
    else if (e.op.equals(Expression.OR))
      return estimateReductionFactor(e.children.get(0))
          + estimateReductionFactor(e.children.get(1));
    else {

      HashSet<Attribute> attrSet = e.getVisibleAttributeSet();
      Iterator<Attribute> attrSetIter = attrSet.iterator();

      if (attrSet.size() == 1) {

        Attribute a = attrSetIter.next();

        try {

          double rf = 0;
          for (AttributeStatistics astats : get(a)) rf += astats.estimateReductionFactor(e);

          return rf;
        } catch (Exception ex) {
          return defaultSelectivity;
        }

      } else if (attrSet.size() == 2) {

        Attribute a1 = attrSetIter.next();
        Attribute a2 = attrSetIter.next();

        try {
          double rf = estimateReductionFactorDouble(e, get(a1).get(0), get(a2).get(0));
          return rf;
        } catch (Exception ex) {
          return defaultSelectivity;
        }

      } else return defaultSelectivity;
    }
  }

  private double estimateReductionFactorDouble(
      Expression e, AttributeStatistics a1, AttributeStatistics a2) throws CannotEstimateException {

    if (e.op.equals(Expression.EQUALS)) {

      return clip10(1.0 / Math.max(a1.distinctValues, a2.distinctValues));

    } else if (e.op.equals(Expression.GREATER_THAN)
        || e.op.equals(Expression.GREATER_THAN_EQUALS)) {

      return 1.0;
    } else if (e.op.equals(Expression.LESS_THAN) || e.op.equals(Expression.LESS_THAN_EQUALS)) {

      return 1.0;
    } else throw new CannotEstimateException(e);
  }

  private double clip10(double val) {
    return Math.max(Math.min(val, 1.0), 0.0);
  }

  public Cost joinOperator(Operator in, Cost l, Cost r) {

    // System.out.println(in);

    if (in.params.expression.get(0).isEquality()) {

      // long cartesian = l.resultCardinality * r.resultCardinality;
      // System.out.println(l.resultCardinality + " " + r.resultCardinality);
      long result = (long) (Math.min(l.resultCardinality, r.resultCardinality));
      // System.out.println(result + " " + l.resultCardinality + " " + r.resultCardinality);

      return new Cost(2 * l.resultCardinality + 2 * r.resultCardinality, result, 0);
    } else
      return new Cost(
          l.resultCardinality * r.resultCardinality, l.resultCardinality * r.resultCardinality, 0);
  }

  public Cost estimate(Operator in) {
    if (in instanceof TableAccessOperator) return tableAccessOperator(in);

    Cost left_cost = estimate(in.source.get(0));

    if (in instanceof ProjectOperator) return projectOperator(in, left_cost).plus(left_cost);

    if (in instanceof SelectOperator) return selectOperator(in, left_cost).plus(left_cost);

    if (in instanceof GroupByOperator) return groupByOperator(in, left_cost).plus(left_cost);

    if (in instanceof JoinOperator) {
      Cost right_cost = estimate(in.source.get(1));
      return joinOperator(in, left_cost, right_cost).plus(left_cost).plus(right_cost);
    }

    return new Cost(0, 0, 0);
  }
}
