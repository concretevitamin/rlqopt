package edu.berkeley.riselab.rlqopt.opt.learning;

import edu.berkeley.riselab.rlqopt.opt.*;
import edu.berkeley.riselab.rlqopt.Relation;
import edu.berkeley.riselab.rlqopt.preopt.*;
import java.util.LinkedList;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;

// the main planner class
public class LearningPlanner extends Planner {

  public LearningPlanner(LinkedList<Relation> allRelations) {
    super(new LinkedList(), new LinkedList(), new LinkedList());
    // this.preopt.add(new ExposeProjection());
    this.preopt.add(new CascadedSelect());
    this.preopt.add(new CorrespondAttributes());
    this.preopt.add(new FlattenJoin());
    this.init.add(new EagerSelectProject());
    this.planners.add(new TDJoinExecutor(allRelations));
    this.setPlannerName("learning");
  }

  public void setNetwork(MultiLayerNetwork net) {
    ((TDJoinExecutor) this.planners.get(0)).net = net;
  }
}
