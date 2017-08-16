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

import org.phenotips.Constants;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Provides access to attached additional documents.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable("New controller in 1.4M1")
@Component(roles = { PatientDataController.class })
@Named("additionalDocuments")
@Singleton
public class AdditionalDocumentsController implements PatientDataController<Attachment>
{
    /** The XClass used for storing additional files. */
    public static final EntityReference CLASS_REFERENCE = new EntityReference("ExternalFileClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    private static final String DATA_NAME = "additional_documents";

    private static final String FILE_FIELD_NAME = "file";

    private static final String COMMENTS_FIELD_NAME = "comments";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private AttachmentAdapterFactory adapter;

    @Inject
    private Logger logger;

    @Override
    public PatientData<Attachment> load(Patient patient)
    {
        try {
            XWikiDocument doc = patient.getXDocument();
            List<BaseObject> data = doc.getXObjects(CLASS_REFERENCE);
            if (CollectionUtils.isEmpty(data)) {
                return null;
            }

            List<Attachment> result = new ArrayList<>(data.size());

            for (BaseObject xobject : data) {
                if (xobject == null) {
                    continue;
                }
                String filename = xobject.getStringValue(FILE_FIELD_NAME);
                XWikiAttachment xattachment = doc.getAttachment(filename);
                if (xattachment != null) {
                    Attachment attachment = this.adapter.fromXWikiAttachment(xattachment);
                    attachment.addAttribute(COMMENTS_FIELD_NAME, xobject.getLargeStringValue(COMMENTS_FIELD_NAME));
                    result.add(attachment);
                }
            }

            return new IndexedPatientData<>(getName(), result);
        } catch (Exception e) {
            this.logger.error(ERROR_MESSAGE_LOAD_FAILED, e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        PatientData<Attachment> files = patient.getData(getName());
        if (files == null) {
            return;
        }
        try {
            XWikiDocument doc = patient.getXDocument();
            doc.removeXObjects(CLASS_REFERENCE);

            for (Attachment file : files) {
                BaseObject xobject = doc.newXObject(CLASS_REFERENCE, this.contextProvider.get());
                XWikiAttachment xattachment = doc.getAttachment(file.getFilename());
                if (xattachment == null) {
                    xattachment = new XWikiAttachment(doc, file.getFilename());
                    doc.addAttachment(xattachment);
                }
                xattachment.setContent(file.getContent());
                DocumentReference author = file.getAuthorReference();
                if (author != null
                    && !this.contextProvider.get().getWiki().exists(author, this.contextProvider.get())) {
                    author = this.contextProvider.get().getUserReference();
                }
                xattachment.setAuthorReference(author);
                xattachment.setDate(file.getDate());
                xattachment.setFilesize((int) file.getFilesize());
                xobject.setStringValue(FILE_FIELD_NAME, file.getFilename());
                xobject.setLargeStringValue(COMMENTS_FIELD_NAME, (String) file.getAttribute(COMMENTS_FIELD_NAME));
            }
        } catch (Exception ex) {
            this.logger.error("Failed to save attachment: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !(selectedFieldNames.contains(getName()))) {
            return;
        }

        PatientData<Attachment> files = patient.getData(getName());
        JSONArray result = new JSONArray();
        if (files == null || !files.isIndexed() || files.size() == 0) {
            if (selectedFieldNames != null) {
                json.put(DATA_NAME, result);
            }
            return;
        }

        for (Attachment file : files) {
            result.put(file.toJSON());
        }
        json.put(DATA_NAME, result);
    }

    @Override
    public PatientData<Attachment> readJSON(JSONObject json)
    {
        if (!json.has(DATA_NAME) || json.optJSONArray(DATA_NAME) == null) {
            return null;
        }
        List<Attachment> result = new ArrayList<>();

        JSONArray files = json.getJSONArray(DATA_NAME);
        for (Object file : files) {
            result.add(this.adapter.fromJSON((JSONObject) file));
        }

        return new IndexedPatientData<>(getName(), result);
    }

    @Override
    public String getName()
    {
        return "additionalDocuments";
    }
}