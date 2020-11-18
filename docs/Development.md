# Development Guide
We hope to use Orion to build a library of remediations as well as interfaces for management of various distributed systems.

To see already implemented Plugins

## Basic Concepts

The following section describes the concepts in Orion. Orion has the following key concepts, all of which are pluggable:
- Action
- Sensor
- Operator
- UI

Note: Orion uses Abstract OOP concept extensively, almost all modules are designed with extensibility in mind. 

#### Action
`Base class: com.pinterest.orion.core.actions.Action` 

Actions are programmed workflows that can be executed in Orion either via a human via UI or via an Operator (automation). 

An Action can have 0 or more children. This can used to breakdown a task into smaller tasks e.g. Rolling Restart Action will have one child Action for each node which in-turn can have Children of it's own. All Actions are executed by ActionEngine (com.pinterest.orion.core.actions.ActionEngine)

Note: Parent and Child Actions run in in-dependent queue in the ActionEngine to prevent a perpetual block situation.

Sample Action:

```
package com.sample

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.actions.Action;

public class SampleAction extends Action {

  @Override
  public String getName() {
    // TODO Name of Action as it appears on the UI
    // This can be changed dynamically to show updates in the UI
    return "Sample Action";
  }

  @Override
  public void runAction() throws Exception {
    ActionEngine engine = getEngine();
    Cluster cluster = engine.getCluster();
    // TODO add custom logic
    markSucceeded(); // or markFailed("reason");
  }
}
```

Once programmed, the Action needs to be deployed to an Orion instance. Update plugin configs in your Orion config file

```
plugins:
  actionConfigs:
    - key: sampleAction
      class: com.sample.SampleAction
      enabled: true
```

**NOTE:** If adding this Action to the UI the key MUST match with the key for Action in the UI.


#### Sensor
`Base class: com.pinterest.orion.core.automation.sensor.Sensor`

A Sensor is responsible to pull external information e.g. cluster metadata using native APIs of a system and populate/update State for the cluster. This state can be used to render components in Orion UI to inform for human analysis and for use by various Operators.

Sensors are run in an infinite loop with each Sensor running at it's own frequency specified via the config.

Sensors should be implemented such that they fail if their minimum input  isn't available. A Sensor may dependent on another Sensor to function and instead of creating a nested dependency tree Orion uses a continuous loop design where if Sensor 2 depends on Sensor 1 and Sensor 1 runs before Sensor 2 then Sensor 1 should and will get information from the last run of Sensor 2.

Sample Sensor:

```
package com.sample;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.automation.sensor.Sensor;

public class SampleSensor extends Sensor {

  @Override
  public String getName() {
    // TODO Name of the Sensor as seen on UI
    return "Sample Sensor";
  }

  @Override
  public void observe(Cluster cluster) throws Exception {
    // TODO Auto-generated method stub
     setAttribute(cluster, "attributeKey", "attributeValue");
  }

}
```

##### Attribute & State
An Attribute is a Key, Value, plus additional Metadata and is how Orion stores State regarding a Cluster. Attributes are how Sensors add information to Orion and Operators get information regarding a Cluster.

The Cluster base class extends State and is how State / Information is shared between various key subsystems of Orion i.e. Sensor, Operator, Action and UI

Each Attribute contains the last update timestamp (ms) and the names of Sensors that update this Attribute which can be used to build a Sensor Operator dependency tree analysis of what an Operator needs to to function and ensuring that those Sensors are available at the time of start.
 

#### Operator
An Operator is the plugable part of Orion's Automation Engine and allows expression of logic to trigger automated tasks. An Operator is fed the current Cluster State (updated by the last run of Sensors) and allows it to analyze this state to decide if an Action should be taken. Orion keeps the constraints on Operators abstract so as to not limit the type and sophistication of automation that can be implemented.

Operators are run in a continuous loop with configurable frequency similar to Sensors.

```
package com.sample;

import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.automation.operator.Operator;

public class SampleOperator extends Operator {

  @Override
  public final void operate(Cluster cluster) throws Exception {
    // TODO Add some logic when to trigger this Action
    Action sampleAction = new SampleAction();
    dispatch(sampleAction);
  }

}
```

# Language Support
Orion is written in Java but it supports Plugins written in other JVM compliant languages like: jython, javascript, groovy etc.

Actions in other languages can be implemented using 
`com.pinterest.orion.core.actions.ScriptedAction`
which uses JSR223