package edu.berkeley.riselab.rlqopt.preopt;

import edu.berkeley.riselab.rlqopt.Operator;
import edu.berkeley.riselab.rlqopt.relalg.GroupByOperator;
import edu.berkeley.riselab.rlqopt.relalg.KWayJoinOperator;
import edu.berkeley.riselab.rlqopt.relalg.TableAccessOperator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BreakdownSubqueries {

  private static void traverse(
      Set<Operator> result,
      Operator curr,
      Operator lastMarked,
      boolean seenGroupBy,
      boolean seenKWayJoin) {

    // Edge case.
    if (curr instanceof TableAccessOperator) {
      result.add(lastMarked);
      assert curr.source.isEmpty();
      return;
    }

    if (curr instanceof GroupByOperator) {
      if (seenGroupBy) {
        result.add(lastMarked);
        for (Operator child : curr.source) {
          traverse(result, child, curr, true, false);
        }
      } else {
        for (Operator child : curr.source) {
          traverse(result, child, lastMarked, true, seenKWayJoin);
        }
      }
    } else if (curr instanceof KWayJoinOperator) {
      if (seenKWayJoin) {
        result.add(lastMarked);
        for (Operator child : curr.source) {
          traverse(result, child, curr, false, true);
        }
      } else {
        for (Operator child : curr.source) {
          traverse(result, child, lastMarked, seenGroupBy, true);
        }
      }
    } else {
      for (Operator child : curr.source) {
        traverse(result, child, lastMarked, seenGroupBy, seenKWayJoin);
      }
    }
  }

  /**
   * Breaks down "in" recursively, so that each operator in the resulting list
   *
   * <p>(1) is a subtree of "in",
   *
   * <p>(2) contains at most 1 KWayJoinOperator and at most 1 GroupByOperator that are _not_
   * contained in any other subtrees.
   *
   * <p>Intuitively, this is a heuristic way of chunking out subquery blocks.
   */
  public static List<Operator> apply(Operator in) {
    Set<Operator> result = new HashSet<>();
    traverse(result, in, in, false, false);
    return new ArrayList<>(result);
  }
}
