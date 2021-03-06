/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.data.permissions;

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * A collaborator on a patient record, either a user or a group that has been granted a specific {@link AccessLevel
 * access level}.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
public interface Collaborator
{
    /** The XClass used to store collaborators in the patient record. */
    EntityReference CLASS_REFERENCE = new EntityReference("CollaboratorClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /**
     * Returns the type of collaborator; either "user" or "group".
     *
     * @return the {@link Collaborator} type, as string
     */
    String getType();

    /**
     * Checks if the collaborator is a user.
     *
     * @return {@code true} iff the {@link Collaborator} is a user, {@code false} otherwise
     */
    boolean isUser();

    /**
     * Checks if the collaborator is a group.
     *
     * @return {@code true} iff the {@link Collaborator} is a group, {@code false} otherwise
     */
    boolean isGroup();

    /**
     * Returns the user or group that has been set as collaborator.
     *
     * @return a reference to the user's or group's profile
     */
    EntityReference getUser();

    /**
     * Returns the username or group name.
     *
     * @return the name of the document holding the user or group (just the name without the space or instance name)
     */
    String getUsername();

    /**
     * Returns the access that has been granted to this collaborator.
     *
     * @return an access level, must not be {@code null}
     */
    AccessLevel getAccessLevel();
}
