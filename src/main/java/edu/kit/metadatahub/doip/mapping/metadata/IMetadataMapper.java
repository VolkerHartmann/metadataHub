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
package edu.kit.metadatahub.doip.mapping.metadata;

import edu.kit.turntable.mapping.Datacite43Schema;

/**
 * Generic Mapping interface for metadata to datacite and vice versa.  
 * 
 */
public interface IMetadataMapper<M> {
  /** Map datacite to given metadata. */
  M mapFromDatacite(Datacite43Schema datacite);
  /** Map given metadata to datacite. */
  Datacite43Schema mapToDatacite(M metadata);
  
  
}
