package com.speedment.runtime.internal.field.finder;

import com.speedment.runtime.exception.SpeedmentException;
import com.speedment.runtime.field.CharField;
import com.speedment.runtime.manager.Manager;

/**
 * @param <ENTITY>    entity type
 * @param <FK_ENTITY> foreign entity type
 * 
 * @author Emil Forslund
 * @since  3.0.0
 */
public final class FindFromChar<ENTITY, FK_ENTITY> extends AbstractFindFrom<ENTITY, FK_ENTITY, CharField<ENTITY, ?>, CharField<FK_ENTITY, ?>> {
    
    public FindFromChar(CharField<ENTITY, ?> source, CharField<FK_ENTITY, ?> target, Manager<FK_ENTITY> manager) {
        super(source, target, manager);
    }
    
    @Override
    public FK_ENTITY apply(ENTITY entity) {
        final char value = getSourceField().getter().getAsChar(entity);
        return getTargetManager().findAny(getTargetField(), value)
            .orElseThrow(() -> new SpeedmentException(
                "Error! Could not find any " + 
                getTargetManager().getEntityClass().getSimpleName() + 
                " with '" + getTargetField().identifier().columnName() + 
                "' = '" + value + "'."
            ));
    }
}