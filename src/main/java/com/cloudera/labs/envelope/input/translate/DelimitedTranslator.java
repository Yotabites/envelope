/**
 * Copyright © 2016-2017 Cloudera, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.labs.envelope.input.translate;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.StructType;

import com.cloudera.labs.envelope.utils.RowUtils;
import com.cloudera.labs.envelope.utils.TranslatorUtils;
import com.google.common.collect.Lists;
import com.typesafe.config.Config;

/**
 * A translator implementation for plain delimited text messages, e.g. CSV.
 */
public class DelimitedTranslator implements Translator<String, String> {

  private String delimiter;
  private List<String> fieldNames;
  private List<String> fieldTypes;
  private StructType schema;
  private List<Object> values = Lists.newArrayList();
  private boolean doesAppendRaw;

  public static final String DELIMITER_CONFIG_NAME = "delimiter";
  public static final String FIELD_NAMES_CONFIG_NAME = "field.names";
  public static final String FIELD_TYPES_CONFIG_NAME = "field.types";

  @Override
  public void configure(Config config) {
    delimiter = resolveDelimiter(config.getString(DELIMITER_CONFIG_NAME));
    fieldNames = config.getStringList(FIELD_NAMES_CONFIG_NAME);
    fieldTypes = config.getStringList(FIELD_TYPES_CONFIG_NAME);
    
    doesAppendRaw = TranslatorUtils.doesAppendRaw(config);
    if (doesAppendRaw) {
      fieldNames.add(TranslatorUtils.getAppendRawKeyFieldName(config));
      fieldTypes.add("string");
      fieldNames.add(TranslatorUtils.getAppendRawValueFieldName(config));
      fieldTypes.add("string");
    }
    
    schema = RowUtils.structTypeFor(fieldNames, fieldTypes);
  }

  @Override
  public Iterable<Row> translate(String key, String value) {
    String[] stringValues = value.split(Pattern.quote(delimiter));
    values.clear();

    for (int valuePos = 0; valuePos < stringValues.length; valuePos++) {
      String fieldValue = stringValues[valuePos];
      
      if (fieldValue.length() == 0) {
        values.add(null);
      }
      else {
        switch (fieldTypes.get(valuePos)) {
          case "string":
            values.add(fieldValue);
            break;
          case "float":
            values.add(Float.parseFloat(fieldValue));
            break;
          case "double":
            values.add(Double.parseDouble(fieldValue));
            break;
          case "int":
            values.add(Integer.parseInt(fieldValue));
            break;
          case "long":
            values.add(Long.parseLong(fieldValue));
            break;
          case "boolean":
            values.add(Boolean.parseBoolean(fieldValue));
            break;
          default:
            throw new RuntimeException("Unsupported delimited field type: " + fieldTypes.get(valuePos));
        }
      }

    }

    Row row = RowFactory.create(values.toArray());
    
    if (doesAppendRaw) {
      row = RowUtils.append(row, key);
      row = RowUtils.append(row, value);
    }

    return Collections.singleton(row);
  }

  @Override
  public StructType getSchema() {
    return schema;
  }

  private String resolveDelimiter(String delimiterArg) {
    if (delimiterArg.startsWith("chars:")) {
      String[] codePoints = delimiterArg.substring("chars:".length()).split(",");

      StringBuilder delimiter = new StringBuilder();
      for (String codePoint : codePoints) {
        delimiter.append(Character.toChars(Integer.parseInt(codePoint)));
      }

      return delimiter.toString();
    }
    else {
      return delimiterArg;
    }
  }

}
