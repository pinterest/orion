/*******************************************************************************
 * Copyright 2020 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.pinterest.orion.agent.tools;

public class AgentCliTool {

//  private static final Logger LOG = Logger.getLogger(AgentCliTool.class.getCanonicalName());
//  private static final String HELP_OPT = "help";
//  private static final String HOST_OPT = "host";
//  private static final String OPERATION_OPT = "operation";
//  private static final String DEFAULT_HOST = "localhost";
//  private static final String PORT_OPT = "port";
//  private static final String DEFAULT_PORT = "9099";
//  private static final String CONFIG_PATH_OPT = "config-file";
//  private static final Options opts = new Options()
//      .addRequiredOption("c", CONFIG_PATH_OPT, true, "file with connection configs")
//      .addOption("p", PORT_OPT, true, "the port to connect to")
//      .addOption(null, HOST_OPT, true, "the host to connect to").addOption("h", "help")
//      .addOption("o", OPERATION_OPT, true,
//          "the operation to perform, one of {STOP, START, RESTART, METRICS}");
//
//  protected static NodeAgentBlockingStub buildAgentClient(String host,
//                                                          int port,
//                                                          Properties props) throws Exception {
//    ManagedChannel channel = NettyChannelBuilder.forAddress(host, port)
//        .sslContext(GrpcUtils.buildClientSslContext(props))
//        .build();
//    return NodeAgentGrpc.newBlockingStub(channel);
//  }
//
//  protected static CommandLine parseCommandLine(String[] args) throws ParseException {
//    CommandLineParser parser = new DefaultParser();
//    return parser.parse(opts, args);
//  }
//
//  protected static void printCommandLineHelp() {
//    new HelpFormatter().printHelp(AgentCliTool.class.getName(), opts);
//  }
//
//  public static void main(String[] args) throws Exception {
//    try {
//      CommandLine cmd = parseCommandLine(args);
//      if (cmd.hasOption(HELP_OPT)) {
//        printCommandLineHelp();
//        exit(0);
//      }
//      String host = cmd.getOptionValue(HOST_OPT, DEFAULT_HOST);
//      int port = Integer.parseInt(cmd.getOptionValue(PORT_OPT, DEFAULT_PORT));
//
//      Map<String, Object> configs = ConfigUtils
//          .parseAndLoadConfig(cmd.getOptionValue(CONFIG_PATH_OPT));
//      Properties props = new Properties();
//      props.putAll(configs);
//
//      System.out.println("Attempting to connect to " + host + ":" + port);
//      NodeAgentBlockingStub client = buildAgentClient(host, port, props);
//      if (!cmd.hasOption(OPERATION_OPT)) {
//        healthCheck(client);
//      } else {
//        switch (cmd.getOptionValue(OPERATION_OPT)) {
//        case "STOP":
//          stop(client);
//          break;
//        case "START":
//          start(client);
//          break;
//        case "RESTART":
//          restart(client);
//          break;
//        case "METRICS":
//          metrics(client);
//          break;
//        default:
//          LOG.severe("Invalid operation parameter");
//          exit(-1);
//        }
//      }
//    } catch (ParseException pe) {
//      printCommandLineHelp();
//      throw pe;
//    }
//  }
//
//  public static void stop(NodeAgentBlockingStub client) {
//    try {
//      CmdResult res = client.stopService(Null.getDefaultInstance());
//      LOG.log(Level.INFO, "Result of stop service command: " + res);
//    } catch (Exception te) {
//      LOG.log(Level.SEVERE, "Failed to stop service", te);
//      exit(-1);
//    }
//  }
//
//  public static void start(NodeAgentBlockingStub client) {
//    try {
//      CmdResult res = client.startService(Null.getDefaultInstance());
//      LOG.log(Level.INFO, "Result of start service command: " + res);
//    } catch (Exception te) {
//      LOG.log(Level.SEVERE, "Failed to start service", te);
//      exit(-1);
//    }
//  }
//
//  public static void restart(NodeAgentBlockingStub client) {
//    try {
//      CmdResult res = client.restartService(Null.getDefaultInstance());
//      LOG.log(Level.INFO, "Result of restart service command: " + res);
//    } catch (Exception te) {
//      LOG.log(Level.SEVERE, "Failed to restart service", te);
//      exit(-1);
//    }
//  }
//
//  public static void metrics(NodeAgentBlockingStub client) {
//    try {
//      Metrics res = client.fetchServiceMetrics(Null.getDefaultInstance());
//      LOG.log(Level.INFO, "Fetched metrics: " + res);
//    } catch (Exception te) {
//      LOG.log(Level.SEVERE, "Failed to restart service", te);
//      exit(-1);
//    }
//  }
//
//  public static void healthCheck(NodeAgentBlockingStub client) {
//    if (client == null) {
//      LOG.log(Level.SEVERE, "Client is null");
//      exit(-1);
//    }
//
//    try {
//      StatusType agentHealth = client.getAgentStatus(Null.getDefaultInstance()).getStatusType();
//      LOG.log(Level.INFO, "Agent health check returned: " + agentHealth);
//    } catch (Exception te) {
//      LOG.log(Level.SEVERE, "Cannot retrieve agent health check", te);
//      exit(-1);
//    }
//
//    try {
//      StatusType serviceHealth = client.getServiceStatus(Null.getDefaultInstance()).getStatusType();
//      LOG.log(Level.INFO, "Service health check returned: " + serviceHealth);
//    } catch (Exception te) {
//      LOG.log(Level.SEVERE, "Cannot retrieve service health check", te);
//      exit(-1);
//    }
//
//    try {
//      NodeInfo info = client.getInfo(Null.getDefaultInstance());
//      LOG.log(Level.INFO, "Agent node info: " + info);
//    } catch (Exception te) {
//      LOG.log(Level.SEVERE, "Cannot retrieve agent node info", te);
//    }
//
//  }
}
