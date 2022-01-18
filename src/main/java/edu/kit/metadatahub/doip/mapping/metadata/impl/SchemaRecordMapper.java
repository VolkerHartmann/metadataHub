/*
 * Copyright 2021 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.metadatahub.doip.mapping.metadata.impl;

import edu.kit.metadatahub.doip.mapping.metadata.IMetadataMapper;
import edu.kit.turntable.mapping.Datacite43Schema;
import edu.kit.turntable.mapping.Date;
import edu.kit.turntable.mapping.Description;
import edu.kit.turntable.mapping.Identifier;
import edu.kit.turntable.mapping.SchemaRecordSchema;
import edu.kit.turntable.mapping.Title;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Map SchemaRecord to datacite.
 */
public class SchemaRecordMapper implements IMetadataMapper<SchemaRecordSchema> {

  /**
   * Logger.
   */
  private final static Logger LOGGER = LoggerFactory.getLogger(SchemaRecordMapper.class);

  @Override
  public SchemaRecordSchema mapFromDatacite(Datacite43Schema datacite) {
    SchemaRecordSchema metadata = new SchemaRecordSchema();
    metadata.setSchemaId(datacite.getTitles().iterator().next().getTitle());
    metadata.setType(SchemaRecordSchema.Type.fromValue(datacite.getFormats().iterator().next()));

    return metadata;
  }

  @Override
  public Datacite43Schema mapToDatacite(Object metadataObject) {
    SchemaRecordSchema metadata = (SchemaRecordSchema) metadataObject;
    Datacite43Schema datacite = new Datacite43Schema();
    datacite.getFormats().add(metadata.getType().value());
    Title title = new Title();
    title.setTitle(metadata.getSchemaId());
    title.setTitleType(Title.TitleType.OTHER);
    datacite.getTitles().add(title);
    Date dates = new Date();
    dates.setDate(metadata.getCreatedAt().toString());
    dates.setDateType(Date.DateType.CREATED);
    Date upDate = new Date();
    upDate.setDate(metadata.getCreatedAt().toString());
    upDate.setDateType(Date.DateType.CREATED);
    datacite.getDates().add(dates);
    datacite.getDates().add(upDate);
    Identifier identifier = new Identifier();
    identifier.setIdentifier(metadata.getSchemaId());//Pid().getIdentifier());
    identifier.setIdentifierType(metadata.getPid().getIdentifierType());
    datacite.getIdentifiers().add(identifier);

    return datacite;
  }

}
