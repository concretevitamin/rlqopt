package edu.berkeley.riselab.rlqopt.cost;

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

public class TableStatisticsModel extends HistogramRelation
    implements CostModel {

  private double defaultSelectivity = 0.1;

  public TableStatisticsModel(HashMap<Attribute, Histogram> data) {
    super(data);
  }

  public TableStatisticsModel() {
    super(new HashMap());
  }

  public Cost tableAccessOperator(Operator in) {
    int count = HistogramOperations.eval(this, in).count();
    return new Cost(count, count, 0);
  }

  public Cost projectOperator(Operator in, Cost costIn) {
    //int count = HistogramOperations.eval(this, in);
    return new Cost(costIn.resultCardinality, costIn.resultCardinality, 0);
  }

  public Cost groupByOperator(Operator in, Cost costIn) {
    int count = HistogramOperations.eval(this, in).count();
    return new Cost(costIn.resultCardinality, count, 0);
  }

  public Cost selectOperator(Operator in, Cost costIn) {
    int count = HistogramOperations.eval(this, in).count();
    return new Cost(costIn.resultCardinality, count, 0);
  }

  public Cost joinOperator(Operator in, Cost l, Cost r) {

    int count = HistogramOperations.eval(this, in).count();

    return new Cost(2 * l.resultCardinality + 2 * r.resultCardinality, count, 0);
    
  }

  public Cost cartesianOperator(Operator in, Cost l, Cost r) {

    int count = HistogramOperations.eval(this, in).count();

    return new Cost(
        l.resultCardinality * r.resultCardinality, count, 0);
  }

  private Cost doEstimate(Operator in) {

    Cost runningCost = new Cost(0, 0, 0);

    if (in instanceof TableAccessOperator) return tableAccessOperator(in);

    if (in instanceof ProjectOperator)
      return projectOperator(in, doEstimate(in.source.get(0))).plus(doEstimate(in.source.get(0)));

    if (in instanceof SelectOperator)
      return selectOperator(in, doEstimate(in.source.get(0))).plus(doEstimate(in.source.get(0)));

    if (in instanceof GroupByOperator)
      return groupByOperator(in, estimate(in.source.get(0))).plus(doEstimate(in.source.get(0)));

    if (in instanceof JoinOperator)
      return joinOperator(in, doEstimate(in.source.get(0)), doEstimate(in.source.get(1)))
          .plus(estimate(in.source.get(0)))
          .plus(estimate(in.source.get(1)));

    if (in instanceof CartesianOperator)
      return cartesianOperator(in, doEstimate(in.source.get(0)), doEstimate(in.source.get(1)))
          .plus(estimate(in.source.get(0)))
          .plus(estimate(in.source.get(1)));

    return runningCost;
  }

  public Cost estimate(Operator in) {
    Cost runningCost = new Cost(0, 0, 0);
    runningCost = doEstimate(in);

    if (runningCost.operatorIOcost < 0) runningCost.operatorIOcost = Long.MAX_VALUE;

    if (runningCost.operatorCPUcost < 0) runningCost.operatorCPUcost = Long.MAX_VALUE;

    if (runningCost.resultCardinality < 0) runningCost.resultCardinality = Long.MAX_VALUE;

    return runningCost;
  }
}
