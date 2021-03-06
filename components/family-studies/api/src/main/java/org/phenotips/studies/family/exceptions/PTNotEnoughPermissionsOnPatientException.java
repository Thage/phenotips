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
package org.phenotips.studies.family.exceptions;

import org.xwiki.security.authorization.Right;

/**
 * TODO: redesign, move to another package.
 *
 * @version $Id$
 * @since 1.4
 */
public class PTNotEnoughPermissionsOnPatientException extends PTNotEnoughPermissionsException
{
    /**
     * @param missingRight the right that was required to perform an operation but which the requesting user does not
     *            posess
     * @param patientId the id of the patient that the user had no right for
     */
    public PTNotEnoughPermissionsOnPatientException(Right missingRight, String patientId)
    {
        super(missingRight, patientId);
    }
}
