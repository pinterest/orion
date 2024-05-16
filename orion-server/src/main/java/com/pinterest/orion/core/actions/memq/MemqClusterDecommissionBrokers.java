package com.pinterest.orion.core.actions.memq;

import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.generic.GenericClusterWideAction;

/**
 * This class is used to run multiple MemqBrokerDecommissionAction in parallel.
 * Put MemqBrokerDecommissionAction in getChildAction() when extending this class.
 */
public abstract class MemqClusterDecommissionBrokers extends GenericClusterWideAction.ParallelDecommissionNodeAction {

    @Override
    public abstract Action getChildAction();
}
