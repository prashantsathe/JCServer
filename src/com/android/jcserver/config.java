package com.android.jcserver;

import java.io.File;

public class config {

    public static final String JCOP_PROVIDER = "jcop";
    public static final String JCARDSIM_PROVIDER = "jcardsim";
    public static final int MAX_LOGICAL_CHANNEL = 4;
    public static final int PORT = 8080;
    public static final int JCOP_PORT = 8050;

    public static final byte INS_SELECT = (byte) 0xA4;
    public static final byte INS_MANAGE_CHANNEL = (byte) 0x70;

    // keymaster
    public static final String keymasterAid = "A00000006203020C010101";
    public static final byte[] KEYMASTER_PKG_AID = 
            Utils.hexStringToByteArray("A00000006203020C0101");
    public static final byte[] KEYMASTER_AID = 
            Utils.hexStringToByteArray(keymasterAid);
    public static final byte[] SEPROVIDER_PKG_AID = 
            Utils.hexStringToByteArray("A00000006203020C0102");
    public static final byte[] WEAVER_PKG_AID = 
            Utils.hexStringToByteArray("A000000476575652434F4D4D");
    public static final byte[] WEAVER_CORE_PKG_AID = 
            Utils.hexStringToByteArray("A000000476575652434F5245300101");
    public static final byte[] WEAVER_AID = 
            Utils.hexStringToByteArray("A000000476575652434F4D4D30");
    public static final byte[] WEAVER_CORE_AID = 
            Utils.hexStringToByteArray("A000000476575652434F524530010101");
    public static final String CAP_SEPRIVIDER = "seprovider.cap";
    public static final String CAP_KEYMASTER = "keymaster.cap";
    public static final String CAP_WEAVER = "weaver.cap";
    public static final String CAP_WEAVER_CORE = "core.cap";
    public static final String DEFAULT_CAP_PATH = "ExtBinaries";

    // FiRa
    public static final byte[] BER_PKG_AID = Utils.hexStringToByteArray("a0000008675303");
    public static final byte[] SUS_EXT_PKG_AID = Utils.hexStringToByteArray("a000000867534941");
    public static final byte[] SERVICE_APPLET_PKG_AID = Utils.hexStringToByteArray("a00000086709");
    public static final byte[] SERVICE_APPLET_AID = Utils.hexStringToByteArray("a00000086753555302");
    public static final byte[] SC_PKG_AID = Utils.hexStringToByteArray("a00000086706"); // secure channel
    public static final byte[] FIRA_APPLET_PKG_AID = Utils.hexStringToByteArray("a0000008670304");
    public static final byte[] FIRA_APPLET_AID = Utils.hexStringToByteArray("A00000086746415000");
    public static final byte[] SUS_APPLET_PKG_AID = Utils.hexStringToByteArray("a0000008675301");
    public static final byte[] SUS_APPLET_AID = Utils.hexStringToByteArray("a00000086753555300");

    public static final String CAP_BER = "ExtBinaries/ber.cap";
    public static final String CAP_SUS = "ExtBinaries/sus.cap";
    public static final String CAP_FIRA_SERVICE_APPLET = "ExtBinaries/FiRaServiceApplet.cap";
    public static final String CAP_FIRA_SECURECHANNEL = "ExtBinaries/SecureChannels.cap";
    public static final String CAP_FIRA_APPLET = "ExtBinaries/FiraApplet.cap";
    public static final String CAP_SUS_APPLET = "ExtBinaries/SusApplet.cap";

    public static String getAbsolutePath(String path) {
        File file = new File(path);
        return file.getAbsolutePath();
    }
}
