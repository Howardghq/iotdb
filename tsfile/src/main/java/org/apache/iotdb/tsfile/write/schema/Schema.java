/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.tsfile.write.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.common.Path;
import org.apache.iotdb.tsfile.write.record.RowBatch;

/**
 * Schema stores the schema of the measurements and devices that exist in this file. All devices
 * written to the same TsFile shall have the same schema. Schema takes the JSON schema file as a
 * parameter and registers measurements in such JSON. Schema also records all existing device IDs in
 * this file.
 */
public class Schema {

  /**
   * Path (device + measurement) -> TimeseriesSchema By default, use the LinkedHashMap to store the
   * order of insertion
   */
  private Map<Path, TimeseriesSchema> timeseriesSchemaMap;

  /**
   * template name -> (measuremnet -> TimeseriesSchema)
   */
  private Map<String, Map<String, TimeseriesSchema>> deviceTemplates;

  /**
   * device -> template name
   */
  private Map<String, String> devices;

  /**
   * register a measurement schema map.
   */

  public Schema() {
    this.timeseriesSchemaMap = new LinkedHashMap<>();
  }

  public Schema(Map<Path, TimeseriesSchema> knownSchema) {
    this.timeseriesSchemaMap = knownSchema;
  }

  /**
   * Create a row batch to write aligned data
   *
   * @param deviceId the name of the device specified to be written in
   */
  public RowBatch createRowBatch(String deviceId) {
    return new RowBatch(deviceId, new ArrayList<>(timeseriesSchemaMap.values()));
  }

  /**
   * Create a row batch to write aligned data
   *
   * @param deviceId     the name of the device specified to be written in
   * @param maxBatchSize max size of rows in batch
   */
  public RowBatch createRowBatch(String deviceId, int maxBatchSize) {
    return new RowBatch(deviceId, new ArrayList<>(timeseriesSchemaMap.values()), maxBatchSize);
  }

  public void registerTimeseries(Path path, TimeseriesSchema descriptor) {
    this.timeseriesSchemaMap.put(path, descriptor);
  }

  public void regieterDeviceTemplate(String templateName, Map<String, TimeseriesSchema> template) {
    if (deviceTemplates == null) {
      deviceTemplates = new HashMap<>();
    }
    this.deviceTemplates.put(templateName, template);
  }

  public void extendTemplate(String templateName, TimeseriesSchema descriptor) {
    Map<String, TimeseriesSchema> template = this.deviceTemplates
        .getOrDefault(templateName, new HashMap<>());
    template.put(descriptor.getMeasurementId(), descriptor);
    this.deviceTemplates.put(templateName, template);
  }

  public void regiesterDevice(String deviceId, String templateName) {
    if (!deviceTemplates.containsKey(templateName)) {
      return;
    }
    if (devices == null) {
      devices = new HashMap<>();
    }
    this.devices.put(deviceId, templateName);
    Map<String, TimeseriesSchema> template = deviceTemplates.get(templateName);
    for (Map.Entry<String, TimeseriesSchema> entry : template.entrySet()) {
      Path path = new Path(deviceId, entry.getKey());
      registerTimeseries(path, entry.getValue());
    }
  }

  public TimeseriesSchema getSeriesSchema(Path path) {
    return timeseriesSchemaMap.get(path);
  }

  public TSDataType getTimeseriesDataType(Path path) {
    if (!timeseriesSchemaMap.containsKey(path)) {
      return null;
    }
    return timeseriesSchemaMap.get(path).getType();
  }

  public boolean containsDevice(String device) {
    return devices.containsKey(device);
  }

  public Map<Path, TimeseriesSchema> getTimeseriesSchemaMap() {
    return timeseriesSchemaMap;
  }

  /**
   * check if this schema contains a measurement named measurementId.
   */
  public boolean containsTimeseries(Path path) {
    return timeseriesSchemaMap.containsKey(path);
  }

}
