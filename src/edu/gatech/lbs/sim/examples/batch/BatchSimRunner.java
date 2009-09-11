// Copyright (c) 2009, Georgia Tech Research Corporation
// Authors:
//   Peter Pesti (pesti@gatech.edu)
//
package edu.gatech.lbs.sim.examples.batch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;

import com.sun.net.httpserver.HttpServer;

import edu.gatech.lbs.core.FileHelper;
import edu.gatech.lbs.core.logging.Logz;
import edu.gatech.lbs.core.logging.Varz;
import edu.gatech.lbs.sim.Simulation;

/**
 * A simulation runner that allows running a batch of simulations one after another.
 * A single modified configuration file may also specify several simulations that only
 * differ in some parameters from each other.
 *
 */
public class BatchSimRunner {
  private static SimMonitorHttpServer simMonitorHttpServer;
  private static long wallStartTime;

  public long getRunTime() {
    return (long) ((System.nanoTime() - wallStartTime) / 1e6);
  }

  protected Simulation makeSimulation() {
    return new Simulation();
  }

  protected void runSimulationBySpecification(String configText) {
    Simulation sim = makeSimulation();
    sim.loadConfigurationFromSpecification(configText);
    sim.initSimulation();
    sim.runSimulation();
    sim.endSimulation();
  }

  public void runSimulationSet(String configFilename) {
    if (simMonitorHttpServer != null) {
      simMonitorHttpServer.batchRunner = this;
    }
    wallStartTime = System.nanoTime();

    try {
      System.out.println("Loading configuration file '" + configFilename + "'... ");
      String varzFilename = "varz." + configFilename + ".dat";
      InputStream in = FileHelper.openFileOrUrl(configFilename);
      String contents = FileHelper.getContentsFromInputStream(in);
      in.close();

      String[] parts = contents.split("[\\{\\}]");

      if (parts.length == 1) {
        Varz.set("configFilename", configFilename);
        Varz.set("varzFilename", varzFilename);
        runSimulationBySpecification(contents);
      } else {
        String[][] options = new String[parts.length][];
        options[0] = new String[1];
        options[0][0] = parts[0];
        int permutationCount = 1;
        for (int i = 1; i < parts.length; i += 2) {
          options[i] = parts[i].split(",");
          for (int j = 0; j < options[i].length; j++) {
            options[i][j] = options[i][j].trim();
          }
          options[i + 1] = new String[1];
          options[i + 1][0] = parts[i + 1];
          permutationCount *= options[i].length;
        }

        int[] permutation = new int[parts.length];
        for (int i = 0; i < permutation.length; i++) {
          permutation[i] = 0;
        }

        for (int c = 0; c < permutationCount; c++) {
          Logz.print("Configuration #" + (c + 1) + " of " + permutationCount + ", permutation=[");
          StringBuffer newText = new StringBuffer();
          StringBuffer permutationStr = new StringBuffer();
          for (int i = 0; i < permutation.length; i++) {
            if (options[i].length > 1) {
              if (i > 1) {
                Logz.print(";");
                permutationStr.append('\t');
              }
              Logz.print("" + permutation[i]);
              permutationStr.append(permutation[i]);
            }
            newText.append(options[i][permutation[i]]);
          }
          Logz.println("]...");
          permutationStr.append('\t');

          // run simulation:
          Varz.set("configFilename", configFilename);
          Varz.set("logFilename", varzFilename);
          Varz.set("permutationStr", permutationStr.toString());
          Varz.set("permutationStrShort", permutationStr.substring(0, permutationStr.length() - 1).replace('\t', '.'));
          runSimulationBySpecification(newText.toString());

          Logz.println();

          // next permutation:
          for (int i = 0; i < permutation.length; i++) {
            permutation[i]++;
            if (permutation[i] == options[i].length) {
              permutation[i] = 0;
            } else {
              break;
            }
          }
        }
      }
    } catch (IOException e) {
      Logz.println("" + e);
      System.exit(-1);
    }
  }

  public void runSimulationSets(Collection<String> configFilenames) {
    startHttpServer();

    for (String configFilename : configFilenames) {
      runSimulationSet(configFilename);
    }
  }

  public static void startHttpServer() {
    try {
      System.out.print("Starting monitoring HTTP server at http://127.0.0.1:" + SimMonitorHttpServer.portNum + SimMonitorHttpServer.contextStr + "... ");
      HttpServer server = HttpServer.create(new InetSocketAddress(SimMonitorHttpServer.portNum), 5);
      simMonitorHttpServer = new SimMonitorHttpServer();
      server.createContext(SimMonitorHttpServer.contextStr, simMonitorHttpServer);
      server.setExecutor(null); // creates a default executor
      server.start();
      System.out.println("done.");
    } catch (IOException e) {
      System.out.println("failed.");
    }
  }

  public void go(String[] args) {
    if (args.length > 0) {
      if (args[0].compareToIgnoreCase("-all") == 0) {
        System.out.print("Loading list of all simulation sets from current directory... ");
        // get all config files in current directory:
        File dir = new File(".");
        String[] configFilenamesArray = dir.list();
        FilenameFilter filter = new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return name.endsWith(".xml") || name.endsWith(".bxml");
          }
        };
        configFilenamesArray = dir.list(filter);
        Collection<String> configFilenames = new ArrayList<String>();
        for (int i = 0; i < configFilenamesArray.length; i++) {
          configFilenames.add(configFilenamesArray[i]);
        }
        System.out.println("done. (" + configFilenames.size() + " simulation sets)");

        // run simulation with all simulation sets:
        if (configFilenames != null) {
          runSimulationSets(configFilenames);
        }

      } else if (args[0].compareToIgnoreCase("-set") == 0 && args.length > 1) {
        Collection<String> configFilenames = new ArrayList<String>();
        for (int i = 1; i < args.length; i++) {
          configFilenames.add(args[i]);
        }
        System.out.print("Loading list of simulation set filenames from command line... ");
        System.out.println("done. (" + configFilenames.size() + " simulation sets)");

        // run simulation with all simulation sets:
        if (configFilenames.size() > 0) {
          runSimulationSets(configFilenames);
        }

      } else if (args[0].compareToIgnoreCase("-setfile") == 0 && args.length > 1) {
        String setFilename = args[1];
        System.out.print("Loading list of simulation set filenames from '" + setFilename + "'... ");
        Collection<String> configFilenames = new ArrayList<String>();
        try {
          BufferedReader in = new BufferedReader(new FileReader(setFilename));
          String line;
          while ((line = in.readLine()) != null) {
            configFilenames.add(line.trim());
          }
          in.close();
        } catch (IOException e) {
          System.out.println("IOException");
          System.exit(-1);
        }
        System.out.println("done. (" + configFilenames.size() + " simulation sets)");

        // run simulation with all simulation sets:
        if (configFilenames.size() > 0) {
          runSimulationSets(configFilenames);
        }
        // } else if (args[0].compareToIgnoreCase("-gui") == 0) {

      } else {
        startHttpServer();
        String configFileName = args[args.length - 1];
        runSimulationSet(configFileName);
      }

    } else {
      String execStr = "  java " + BatchSimRunner.class.getName() + " ";
      System.out.println("Usage:");
      System.out.println(execStr + "config.xml");
      System.out.println(execStr + "config.bxml");
      System.out.println(execStr + "-set config1.bxml config2.xml");
      System.out.println(execStr + "-setfile config_filenames.txt");
      System.out.println(execStr + "-all");
    }
  }

  public static void main(String[] args) {
    new BatchSimRunner().go(args);
  }
}
