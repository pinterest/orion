package com.pinterest.orion.core.actions.memq;

import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.generic.GenericClusterWideAction;

/**
 * This class is used to run multiple MemqBrokerRebootAction in parallel.
 * Put MemqBrokerRebootAction in getChildAction() when extending this class.
 */
public abstract class MemqClusterRebootBrokers extends GenericClusterWideAction.ParallelRebootNodeAction {

    @Override
    public abstract Action getChildAction();
}
