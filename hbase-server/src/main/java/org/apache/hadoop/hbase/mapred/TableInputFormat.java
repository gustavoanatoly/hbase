/**
 *
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.mapred;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.classification.InterfaceAudience;
import org.apache.hadoop.hbase.classification.InterfaceStability;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.mapreduce.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobConfigurable;
import org.apache.hadoop.util.StringUtils;

/**
 * Convert HBase tabular data into a format that is consumable by Map/Reduce.
 */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class TableInputFormat extends TableInputFormatBase implements
    JobConfigurable {
  private static final Log LOG = LogFactory.getLog(TableInputFormat.class);

  /**
   * space delimited list of columns
   */
  public static final String COLUMN_LIST = "hbase.mapred.tablecolumns";
  /**
   * Job parameter that specifies the input table.
   * */
  public static final String INPUT_TABLE =
      org.apache.hadoop.hbase.mapreduce.TableInputFormat.INPUT_TABLE;
  /** Base-64 encoded scanner. All other SCAN_ confs are ignored if this is specified.
   * See {@link org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil#convertScanToString(Scan)} for more details.
   */
  public static final String SCAN =
      org.apache.hadoop.hbase.mapreduce.TableInputFormat.SCAN;

  public void configure(JobConf job) {
    try {
      initialize(job);
    } catch (Exception e) {
      LOG.error(StringUtils.stringifyException(e));
    }
  }

  @Override
  protected void initialize(JobConf job) throws IOException {
    Path[] tableNames = FileInputFormat.getInputPaths(job);
    String colArg = job.get(COLUMN_LIST);
    String[] colNames = colArg.split(" ");
    byte [][] m_cols = new byte[colNames.length][];
    for (int i = 0; i < m_cols.length; i++) {
      m_cols[i] = Bytes.toBytes(colNames[i]);
    }
    setInputColumns(m_cols);
    Connection connection = ConnectionFactory.createConnection(job);
    initializeTable(connection, TableName.valueOf(tableNames[0].getName()));
  }

  public void validateInput(JobConf job) throws IOException {
    // expecting exactly one path
    Path [] tableNames = FileInputFormat.getInputPaths(job);
    if (tableNames == null || tableNames.length > 1) {
      throw new IOException("expecting one table name");
    }

    // connected to table?
    if (getTable() == null) {
      throw new IOException("could not connect to table '" +
        tableNames[0].getName() + "'");
    }

    // expecting at least one column
    String colArg = job.get(COLUMN_LIST);
    if (colArg == null || colArg.length() == 0) {
      throw new IOException("expecting at least one column");
    }
  }
}
