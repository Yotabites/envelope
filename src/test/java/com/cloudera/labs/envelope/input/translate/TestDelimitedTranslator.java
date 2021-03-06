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

import static org.junit.Assert.assertEquals;

import org.apache.spark.sql.Row;
import org.junit.Test;

import com.cloudera.labs.envelope.utils.TranslatorUtils;
import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class TestDelimitedTranslator {

  @Test
  public void testTranslation() throws Exception {
    String delimited = "hello%$-100.1%$1000.5%$99%$888%$%$false";
    
    Config config = ConfigFactory.empty()
        .withValue(DelimitedTranslator.FIELD_NAMES_CONFIG_NAME, ConfigValueFactory.fromIterable(
            Lists.newArrayList("field1", "field2", "field3", "field4", "field5", "field6", "field7")))
        .withValue(DelimitedTranslator.FIELD_TYPES_CONFIG_NAME, ConfigValueFactory.fromIterable(
            Lists.newArrayList("string", "float", "double", "int", "long", "int", "boolean")))
        .withValue(DelimitedTranslator.DELIMITER_CONFIG_NAME, ConfigValueFactory.fromAnyRef("%$"));
    
    Translator<String, String> t = new DelimitedTranslator();
    t.configure(config);
    
    Row r = t.translate(null, delimited).iterator().next();
    
    assertEquals(r.length(), 7);
    assertEquals(r.get(0), "hello");
    assertEquals(r.get(1), -100.1f);
    assertEquals(r.get(2), 1000.5d);
    assertEquals(r.get(3), 99);
    assertEquals(r.get(4), 888L);
    assertEquals(r.get(5), null);
    assertEquals(r.get(6), false);
  }
  
  @Test
  public void testAppendRaw() throws Exception {
    String delimited = "hello%$-100.1%$1000.5%$99%$888%$%$false";
    
    Config config = ConfigFactory.empty()
        .withValue(DelimitedTranslator.FIELD_NAMES_CONFIG_NAME, ConfigValueFactory.fromIterable(
            Lists.newArrayList("field1", "field2", "field3", "field4", "field5", "field6", "field7")))
        .withValue(DelimitedTranslator.FIELD_TYPES_CONFIG_NAME, ConfigValueFactory.fromIterable(
            Lists.newArrayList("string", "float", "double", "int", "long", "int", "boolean")))
        .withValue(DelimitedTranslator.DELIMITER_CONFIG_NAME, ConfigValueFactory.fromAnyRef("%$"))
        .withValue(TranslatorUtils.APPEND_RAW_ENABLED_CONFIG_NAME, ConfigValueFactory.fromAnyRef(true));
    
    Translator<String, String> t = new DelimitedTranslator();
    t.configure(config);
    
    Row r = t.translate("testkey", delimited).iterator().next();
    
    assertEquals(r.length(), 9);
    assertEquals(r.get(0), "hello");
    assertEquals(r.get(1), -100.1f);
    assertEquals(r.get(2), 1000.5d);
    assertEquals(r.get(3), 99);
    assertEquals(r.get(4), 888L);
    assertEquals(r.get(5), null);
    assertEquals(r.get(6), false);
    assertEquals(r.get(7), "testkey");
    assertEquals(r.get(8), delimited);
  }
  
}
