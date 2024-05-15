package com.pinterest.orion.core.actions.memq;

import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.generic.GenericClusterWideAction;

public abstract class MemqClusterRebootBrokers extends GenericClusterWideAction.ParallelRebootNodeAction {

    @Override
    public abstract Action getChildAction();
}
