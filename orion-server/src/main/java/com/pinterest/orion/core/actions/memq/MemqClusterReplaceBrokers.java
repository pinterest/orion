package com.pinterest.orion.core.actions.memq;

import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.generic.GenericClusterWideAction;

/**
 * This class is used to run multiple MemqBrokerReplaceAction in parallel.
 * Put MemqBrokerReplaceAction in getChildAction() when extending this class.
 */
public abstract class MemqClusterReplaceBrokers extends GenericClusterWideAction.ParallelReplaceNodeAction {

    @Override
    public abstract Action getChildAction();
}
