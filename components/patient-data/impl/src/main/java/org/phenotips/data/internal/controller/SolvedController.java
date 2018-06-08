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
package org.phenotips.data.internal.controller;

import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.ObjectPropertyReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Handles fields for solved patient records, including solved status, PubMed ID, gene symbol, and notes.
 *
 * @version $Id$
 * @since 1.2M2
 */
@Component(roles = { PatientDataController.class })
@Named("solved")
@Singleton
public class SolvedController extends AbstractSimpleController implements Initializable
{
    private static final String SOLVED_STRING = "solved";

    private static final String DATA_NAME = SOLVED_STRING;

    private static final String INTERNAL_PROPERTY_NAME = SOLVED_STRING;

    private static final String STATUS_KEY = SOLVED_STRING;

    private static final String STATUS_SOLVED = SOLVED_STRING;

    private static final String STATUS_UNSOLVED = "unsolved";

    private static final String STATUS_SOLVED_NUMERIC = "1";

    private static final String STATUS_UNSOLVED_NUMERIC = "0";

    private static final String STATUS_UNKNOWN = "";

    private Map<String, String> fields = new LinkedHashMap<>();

    @Override
    public void initialize() throws InitializationException
    {
        this.fields.put(STATUS_KEY, "status");
        this.fields.put("solved__pubmed_id", "pubmed_id");
        this.fields.put("solved__notes", "notes");
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }

    @Override
    protected String getJsonPropertyName()
    {
        return INTERNAL_PROPERTY_NAME;
    }

    protected String getJsonPropertyName(String property)
    {
        String name = this.fields.get(property);
        if (name == null) {
            name = property;
        }
        return name;
    }

    @Override
    protected List<String> getProperties()
    {
        Set<String> properties = this.fields.keySet();
        return new ArrayList<>(properties);
    }

    private String parseSolvedStatus(String status)
    {
        if ("1".equals(status)) {
            return STATUS_SOLVED;
        } else if ("0".equals(status)) {
            return STATUS_UNSOLVED;
        } else {
            return STATUS_UNKNOWN;
        }
    }

    /** Given a status converts it back into `1` or `0`, or if status is unknown into an {@code null}. */
    private String invertSolvedStatus(String status)
    {
        if (STATUS_SOLVED.equals(status)) {
            return STATUS_SOLVED_NUMERIC;
        } else if (STATUS_UNSOLVED.equals(status)) {
            return STATUS_UNSOLVED_NUMERIC;
        } else {
            return "";
        }
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        PatientData<String> data = patient.getData(getName());
        if (data == null || !data.isNamed()) {
            if (selectedFieldNames != null && selectedFieldNames.contains(DATA_NAME)) {
                json.put(getJsonPropertyName(), new JSONObject());
            }
            return;
        }

        Iterator<Entry<String, String>> dataIterator = data.dictionaryIterator();
        final JSONObject container = json.optJSONObject(getJsonPropertyName()) != null
            ? json.optJSONObject(getJsonPropertyName()) : new JSONObject();

        while (dataIterator.hasNext()) {
            Entry<String, String> datum = dataIterator.next();
            String key = datum.getKey();

            if (selectedFieldNames == null || selectedFieldNames.contains(key)) {
                // Parse value
                String value = STATUS_KEY.equals(key) ? parseSolvedStatus(datum.getValue()) : datum.getValue();

                if (StringUtils.isNotBlank(value)) {
                    // Get internal property name
                    String name = getJsonPropertyName(key);
                    container.put(name, value);
                }
            }
        }

        if (container.length() > 0) {
            json.put(getJsonPropertyName(), container);
        }
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        if (!json.has(this.getJsonPropertyName())) {
            // no data supported by this controller is present in provided JSON
            return null;
        }
        Map<String, String> result = new LinkedHashMap<>();

        // since the loader always returns dictionary data, this should always be a block.
        Object jsonBlockObject = json.get(this.getJsonPropertyName());
        if (!(jsonBlockObject instanceof JSONObject)) {
            return null;
        }
        JSONObject jsonBlock = (JSONObject) jsonBlockObject;
        for (String property : this.fields.values()) {
            if (jsonBlock.has(property)) {
                String value = jsonBlock.getString(property);
                if (this.fields.get(STATUS_KEY).equals(property)) {
                    value = invertSolvedStatus(value);
                }
                result.put(property, value);
            }
        }

        return new DictionaryPatientData<>(this.getName(), result);
    }

    @Override
    void saveFieldValue(
        @Nonnull final BaseObject xwikiDataObject,
        @Nonnull final String property,
        @Nullable final String value)
    {
        @SuppressWarnings("unchecked")
        final BaseProperty<ObjectPropertyReference> field =
            (BaseProperty<ObjectPropertyReference>) xwikiDataObject.getField(property);
        if (field != null) {
            field.setValue(applyCast(value));
        }
    }

    @Override
    String getValueForProperty(@Nonnull final PatientData<String> data, @Nonnull final String property)
    {
        return data.get(this.fields.get(property));
    }

    @Override
    boolean containsProperty(@Nonnull final PatientData<String> data, @Nonnull final String property)
    {
        return data.containsKey(this.fields.get(property));
    }

    private Object applyCast(String value)
    {
        if (value == null) {
            return null;
        }
        if (STATUS_SOLVED_NUMERIC.equals(value) || STATUS_UNSOLVED_NUMERIC.equals(value)) {
            return Integer.parseInt(value);
        } else {
            return value;
        }
    }
}
