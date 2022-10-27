package com.android.jcserver;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.javacard.apduio.CadTransportException;
import javacard.framework.Util;
import static com.android.jcserver.config.*;

/**
 * This program demonstrates a simple TCP/IP socket server.
 *
 * @author www.codejava.net
 */
public class JCServer {

    public static void main(String[] args) {
        String providerName;
        String targetName;
        Simulator simulator;
        String[] targetNames;
        String pathToCapFiles = DEFAULT_CAP_PATH;

        if (args.length < 2 || args.length > 3) {
            System.out.println("Simulator and Target name are expected as argument.");
            return;
        } else {
            providerName = args[0];
            targetName = args[1];
            if (args.length == 3) {
                pathToCapFiles = args[2];
            }
        }
        targetNames = targetName.split(",");
        for (String name : targetNames) {
            if (!(name.equals("fira") || name.equals("keymaster") || name.equals("weaver"))) {
                System.out.println("Target name must be either 'fira' or 'keymaster' or 'Weaver");
                return;
                }
        }

        if (JCOP_PROVIDER.equals(providerName)) {
            simulator = new JCOPSimulator();
        } else if (JCARDSIM_PROVIDER.equals(providerName)) {
            simulator = new JCardSimulator();
        } else {
            System.out.println("Unsupported provider.");
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            simulator.initaliseSimulator();
            simulator.setupSimulator(targetNames, pathToCapFiles);

            byte[] outData;
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("\n\n\n\n\n");
                    System.out.println("------------------------New client connected on "
                            + socket.getPort() + "--------------------");
                    OutputStream output = null;
                    InputStream isReader = null;
                    try {
                        socket.setReceiveBufferSize(1024 * 5);
                        output = socket.getOutputStream();
                        isReader = socket.getInputStream();

                        byte[] inBytes = new byte[65536];
                        int readLen = 0, index = 0;
                        short totalLen = 0;
                        short totalReadLen = 0;
                        System.out.println(
                                "Socket input buffer size: " + socket.getReceiveBufferSize());
                        while ((readLen = isReader.read(inBytes, index, 1024 * 5)) > 0) {
                            if (readLen > 0) {
                                System.out.println("Bytes read from index (" + index + ") socket: "
                                        + readLen + " Estimate read: " + isReader.available());
                                if (totalLen == 0) {
                                    // First two bytes holds the actual request length.
                                    totalLen = Util.getShort(inBytes, (short) 0);
                                    totalLen += 2;
                                }
                                totalReadLen += readLen;
                                if (totalReadLen < totalLen) {
                                    // Read from the socket till all the bytes are read.
                                    index += readLen;
                                    continue;
                                }
                                simulator.executeApdu(
                                        Arrays.copyOfRange(inBytes, (short) 2, totalReadLen));
                                outData = simulator.decodeDataOut();

                                byte[] finalOutData = new byte[outData.length + 2];
                                Util.setShort(finalOutData, (short) 0, (short) outData.length);
                                System.arraycopy(outData, 0, finalOutData, 2, outData.length);
                                output.write(finalOutData);
                                System.out.println("Return Data = "
                                        + Utils.byteArrayToHexString(finalOutData));
                                output.flush();
                                index = 0;
                                totalLen = 0;
                                totalReadLen = 0;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (output != null)
                            output.close();
                        if (isReader != null)
                            isReader.close();
                        socket.close();
                    }
                } catch (IOException e) {
                    break;
                } catch (Exception e) {
                    break;
                }
                System.out.println("Client disconnected.");
            }
            simulator.disconnectSimulator();
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        } catch (CadTransportException e1) {
            e1.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}
