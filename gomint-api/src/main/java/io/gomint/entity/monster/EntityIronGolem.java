/*
 * Copyright (c) 2020 Gomint team
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.entity.monster;

import io.gomint.GoMint;
import io.gomint.entity.EntityLiving;

/**
 * @author joserobjr
 * @since 2021-01-12
 */
public interface EntityIronGolem extends EntityLiving<EntityIronGolem> {

    /**
     * Create a new entity iron golem with no config
     *
     * @return empty, fresh iron golem
     */
    static EntityIronGolem create() {
        return GoMint.instance().createEntity( EntityIronGolem.class );
    }

}
