package com.pinterest.orion.core.actions.memq;

import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.generic.GenericClusterWideAction;

public abstract class MemqClusterReplaceBrokers extends GenericClusterWideAction.ParallelReplaceNodeAction {

    @Override
    public abstract Action getChildAction();
}
