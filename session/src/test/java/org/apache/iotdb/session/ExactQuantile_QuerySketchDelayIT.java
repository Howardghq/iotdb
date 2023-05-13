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

package org.apache.iotdb.session;

import org.apache.iotdb.db.conf.IoTDBConfig;
import org.apache.iotdb.db.conf.IoTDBDescriptor;
import org.apache.iotdb.db.utils.EnvironmentUtils;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class ExactQuantile_QuerySketchDelayIT {
  static int TEST_CASE = 5;
  static int startType = 1, endType = 2;
  private static final IoTDBConfig CONFIG = IoTDBDescriptor.getInstance().getConfig();
  private static Session session;
  private static int originCompactionThreadNum;
  private static final int baseSize = (int) (3e8 + 1e7);
  private static final boolean inMemory = false;
  private static final List<String> storageGroupList = new ArrayList<>();
  static List<String> aggrList = new ArrayList<>();

  @BeforeClass
  public static void setUp() throws Exception {
    storageGroupList.add("root.Normal01_Page4096_Sum256Byte_SepLatency_30_0");
    storageGroupList.add("root.Normal01_Page4096_Sum256Byte_SepLatency_30_6");
    storageGroupList.add("root.Normal01_Page4096_Sum256Byte_SepLatency_30_12");
    storageGroupList.add("root.Normal01_Page4096_Sum256Byte_SepLatency_30_18");
    storageGroupList.add("root.Normal01_Page4096_Sum256Byte_SepLatency_30_22");
    storageGroupList.add("root.Normal01_Page4096_Sum256Byte_SepLatency_30_26");
    storageGroupList.add("root.Normal01_Page4096_Sum256Byte_SepLatency_30_30");

    storageGroupList.add("root.Normal01_Page4096_Sum256Byte_NoSepLatency_30_0");
    storageGroupList.add("root.Normal01_Page4096_Sum256Byte_NoSepLatency_30_6");
    storageGroupList.add("root.Normal01_Page4096_Sum256Byte_NoSepLatency_30_12");
    storageGroupList.add("root.Normal01_Page4096_Sum256Byte_NoSepLatency_30_18");
    storageGroupList.add("root.Normal01_Page4096_Sum256Byte_NoSepLatency_30_22");
    storageGroupList.add("root.Normal01_Page4096_Sum256Byte_NoSepLatency_30_26");
    storageGroupList.add("root.Normal01_Page4096_Sum256Byte_NoSepLatency_30_30");

    //    aggrList.add("full_read_once");
    aggrList.add("exact_multi_quantiles_pr_kll_opt_summary");
    aggrList.add("exact_quantile_pr_kll_opt_stat");
    //    aggrList.add("exact_multi_quantiles_quick_select");

    originCompactionThreadNum = CONFIG.getConcurrentCompactionThread();
    CONFIG.setConcurrentCompactionThread(0);
    if (inMemory) EnvironmentUtils.envSetUp();
    session = new Session("127.0.0.1", 6667, "root", "root");
    session.open();
    if (inMemory) {}
  }

  @AfterClass
  public static void tearDown() throws Exception {
    session.close();
    if (inMemory) EnvironmentUtils.cleanEnv();
    CONFIG.setConcurrentCompactionThread(originCompactionThreadNum);
  }

  private String getQueryStatement(String body, long L, long R) {
    return body + " where time>=" + L + " and time<" + R;
  }

  //  private void testTime(int dataType, int queryN, int maxMemoryByte)
  //      throws IoTDBConnectionException, StatementExecutionException {
  //
  //    SessionDataSet dataSet;
  //    //    System.out.println("\t\t\tqueryN:" + queryN + "\tDataset: random");
  //    long[] LL = new long[TEST_CASE];
  //    long[] RR = new long[TEST_CASE];
  //    Random random = new Random(233);
  //    for (int i = 0; i < TEST_CASE; i++) {
  //      LL[i] = random.nextInt(baseSize - queryN + 1);
  //      RR[i] = LL[i] + queryN;
  //      //      System.out.println("\t\t\t"+(LL[i])+"  "+(RR[i]));
  //    }
  //    String sg = storageGroupList.get(dataType);
  //    //    System.out.print(queryN);
  //
  //    System.out.print(
  //        "TEST_CASE="
  //            + TEST_CASE
  //            + "\tDATASET:"
  //            + dataType
  //            + " "
  //            + storageGroupList.get(dataType).substring(5)
  //            + "\tqueryN:\t"
  //            + queryN
  //            + "\tmemory:\t"
  //            + maxMemoryByte
  //            + "\t\t");
  //
  //    double q_tmp = 1e-1, q_start = q_tmp, q_end = 1 - q_tmp;
  //    //    q_start = 0.48;
  //    //    q_end = 0.52;
  //    double q_add = 4e-1, q_count = Math.floor((q_end - q_start + 1e-10) / q_add) + 1;
  //    int aggrSize = aggrList.size();
  //    double[] resultT = new double[aggrSize];
  //    double[] resultV = new double[aggrSize];
  //    for (double quantile = q_start; quantile < q_end + 1e-10; quantile += q_add)
  //      for (int aggrID = 0; aggrID < aggrSize; aggrID++) {
  //        String aggr = aggrList.get(aggrID);
  //        //        if (aggr.equals("count")) if (!sg.contains("Summary0")) continue;
  //        String queryBody =
  //            "select "
  //                + aggr
  //                + "("
  //                + "s0"
  //                + ",'memory'='"
  //                + maxMemoryByte
  //                + "B','quantile'='"
  //                + quantile
  //                + "'"
  //                + ",'return_type'='iteration_num'"
  //                //                                + ",'return_type'='value'"
  //                + ") from "
  //                + sg
  //                + ".d0";
  //        //        session.executeQueryStatement(queryBody);
  //        if (quantile == q_start)
  //          for (int i = 0; i < TEST_CASE /* / 8 + 4*/; i++)
  //            session.executeQueryStatement(getQueryStatement(queryBody, LL[i], RR[i]));
  //        // warm up.
  //
  //        long TIME = new Date().getTime();
  //        LongArrayList tList = new LongArrayList();
  //        for (int t = 0; t < TEST_CASE; t++) {
  //          dataSet = session.executeQueryStatement(getQueryStatement(queryBody, LL[t], RR[t]));
  //          String tmpS = dataSet.next().getFields().toString();
  //          resultV[aggrID] +=
  //              Double.parseDouble(tmpS.substring(1, tmpS.length() - 1)) / TEST_CASE / q_count;
  //          long mmp = new Date().getTime();
  //          tList.add(mmp - TIME);
  //          TIME = mmp;
  //          //                    System.out.print("\t|"+tmpS+"|\t");
  //          //          System.out.println(getQueryStatement(queryBody,LL[t],RR[t]));
  //        }
  //        tList.sort(Long::compare);
  //        long sum = 0, cnt = 0;
  //        //        for (int i = TEST_CASE / 4; i < TEST_CASE * 2 / 4; cnt++, i++) sum +=
  //        // tList.getLong(i);
  //        for (int i = 0; i < TEST_CASE; cnt++, i++) sum += tList.getLong(i);
  //        double avgT = 1.0 * sum / cnt;
  //
  //        //        System.out.println("\t\t\t\t\tq:" + quantile + "\t\tavgT:" + avgT + "\t\t" +
  //        // tList);
  //        resultT[aggrID] += avgT / q_count;
  //      }
  //    for (int aggrID = 0; aggrID < aggrSize; aggrID++)
  //      System.out.print("\t\t\tavgT:\t" + resultT[aggrID] + "\tavgV:\t" + resultV[aggrID]);
  //
  //    System.out.println();
  //  }

  private void testMultiTime(int dataType, int queryN, int maxMemoryByte, int MULTI_QUANTILES)
      throws IoTDBConnectionException, StatementExecutionException {

    SessionDataSet dataSet;
    //    System.out.println("\t\t\tqueryN:" + queryN + "\tDataset: random");
    long[] LL = new long[TEST_CASE];
    long[] RR = new long[TEST_CASE];
    Random random = new Random(233);
    for (int i = 0; i < TEST_CASE; i++) {
      LL[i] = random.nextInt(baseSize - queryN + 1);
      RR[i] = LL[i] + queryN;
      //      System.out.println("\t\t\t"+(LL[i])+"  "+(RR[i]));
    }
    String sg = storageGroupList.get(dataType);
    //    System.out.print(queryN);

    System.out.print(
        "TEST_CASE="
            + TEST_CASE
            + "\tDATASET:"
            + dataType
            + " "
            + storageGroupList.get(dataType).substring(5)
            + "\tqueryN:\t"
            + queryN
            + "\tmemory:\t"
            + maxMemoryByte
            + "\t\t"
            + "MULTI_QUANTILES:\t"
            + MULTI_QUANTILES);

    int aggrSize = aggrList.size();
    double[] resultT = new double[aggrSize];
    double[] resultV = new double[aggrSize];
    for (int aggrID = 0; aggrID < aggrSize; aggrID++) {
      String aggr = aggrList.get(aggrID);
      //        if (aggr.equals("count")) if (!sg.contains("Summary0")) continue;
      String queryBody =
          "select "
              + aggr
              + "("
              + "s0"
              + ",'memory'='"
              + maxMemoryByte
              + "B','multi_quantiles'='"
              + MULTI_QUANTILES
              + "'"
              + ",'return_type'='iteration_num'"
              //                                + ",'return_type'='value'"
              + ") from "
              + sg
              + ".d0";
      //        session.executeQueryStatement(queryBody);
      //          for (int i = 0; i < TEST_CASE /* / 8 + 4*/; i++)
      //            session.executeQueryStatement(getQueryStatement(queryBody, LL[i], RR[i]));
      // warm up.

      long TIME = new Date().getTime();
      LongArrayList tList = new LongArrayList();
      for (int t = 0; t < TEST_CASE; t++) {
        dataSet = session.executeQueryStatement(getQueryStatement(queryBody, LL[t], RR[t]));
        String tmpS = dataSet.next().getFields().toString();
        resultV[aggrID] += Double.parseDouble(tmpS.substring(1, tmpS.length() - 1)) / TEST_CASE;
        long mmp = new Date().getTime();
        tList.add(mmp - TIME);
        TIME = mmp;
        //                    System.out.print("\t|"+tmpS+"|\t");
        //          System.out.println(getQueryStatement(queryBody,LL[t],RR[t]));
      }
      tList.sort(Long::compare);
      long sum = 0, cnt = 0;
      //        for (int i = TEST_CASE / 4; i < TEST_CASE * 2 / 4; cnt++, i++) sum +=
      // tList.getLong(i);
      for (int i = 0; i < TEST_CASE; cnt++, i++) sum += tList.getLong(i);
      double avgT = 1.0 * sum / cnt;

      //        System.out.println("\t\t\t\t\tq:" + quantile + "\t\tavgT:" + avgT + "\t\t" +
      // tList);
      resultT[aggrID] += avgT;
    }
    for (int aggrID = 0; aggrID < aggrSize; aggrID++)
      System.out.print("\t\t\tavgT:\t" + resultT[aggrID] + "\tavgV:\t" + resultV[aggrID]);

    System.out.println();
  }

  @Test
  public void run() throws IoTDBConnectionException, StatementExecutionException, IOException {
    long ST = new Date().getTime();
    for (String aggr : aggrList) System.out.print("\t\t\t\t\t\t\t" + aggr);
    System.out.println();
    System.out.println("Inner Merge.");

    int sgHalf = storageGroupList.size() / 2;
    for (int queryN : new int[] {100000000})
      for (int MULTI_QUANTILES : new int[] {1})
        for (int dataType :
            new int[] {
              //
              // 0,1,2,3,4,5,6,sgHalf+0,sgHalf+1,sgHalf+2,sgHalf+3,sgHalf+4,sgHalf+5,sgHalf+6
              /*0,sgHalf+0,1,sgHalf+1,2,sgHalf+2,3,sgHalf+3,4,sgHalf+4,*/ 5,
              sgHalf + 5,
              6,
              sgHalf + 6
            })
          for (int query_mem : new int[] {1024 * 1024 * 1})
            testMultiTime(dataType, queryN, query_mem, MULTI_QUANTILES);

    //    aggrList.removeIf(aggr -> !aggr.contains("summary"));
    //    for (String aggr : aggrList) System.out.print("\t\t\t\t\t\t\t" + aggr);
    //    System.out.println();
    //    for (int queryN : new int[]{100000000})
    //      for (int MULTI_QUANTILES : new int[]{/*1, 2, 4, 7, 10, 20, 40, 70, 100, 200, 400, 700,
    // */1000/*, 2000, 4000, 7000/*, 10000*/})
    //        for (int dataType : new int[]{1})
    //          for (int query_mem : new int[]{1024 * 1024 * 8,1024 * 1024 * 10,1024 * 1024 *
    // 12,1024 * 1024 * 16,1024 * 1024 * 24,1024 * 1024 * 32,1024 * 1024 * 48,1024 * 1024 * 64})
    //            testMultiTime(dataType, queryN, query_mem, MULTI_QUANTILES);

    System.out.println("\t\tALL_TIME:" + (new Date().getTime() - ST));
  }
}
