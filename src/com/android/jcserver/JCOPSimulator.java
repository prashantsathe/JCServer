package com.android.jcserver;

import static com.android.jcserver.config.*;

public class JCOPSimulator implements Simulator {

    private JCOPOpenCard openCardSim = null;
    private opencard.core.terminal.ResponseAPDU response;

    public JCOPSimulator() {
    }

    @Override
    public void initaliseSimulator() throws Exception {
        openCardSim = JCOPOpenCard.getInstance();
        if (!openCardSim.isConnected()) {
            try {
                openCardSim.connect(JCOP_PORT);
            } catch (JCOPException e) {
                openCardSim.close();
                throw new JCOPException(e.getMessage());
            }
        }
    }

    @Override
    public void disconnectSimulator() throws Exception {
        openCardSim.close();
    }

    private void installKeymaster(String capPath) throws JCOPException {
        openCardSim.installApplet(getAbsolutePath(capPath + "/" + CAP_SEPRIVIDER), null,
                SEPROVIDER_PKG_AID);
        openCardSim.installApplet(getAbsolutePath(capPath + "/" + CAP_KEYMASTER), KEYMASTER_AID,
                KEYMASTER_PKG_AID);
    }
    
    private void installWeaver(String capPath) throws JCOPException {
        openCardSim.installApplet(getAbsolutePath(capPath + "/" + CAP_WEAVER), WEAVER_AID,
                WEAVER_PKG_AID);
        openCardSim.installApplet(getAbsolutePath(capPath + "/" + CAP_WEAVER_CORE), WEAVER_CORE_AID,
                WEAVER_CORE_PKG_AID);
    } 

    private void installFira(String capPath) throws JCOPException {
        openCardSim.installApplet(getAbsolutePath(capPath + "/" + CAP_BER), null, BER_PKG_AID);
        openCardSim.installApplet(getAbsolutePath(capPath + "/" + CAP_SUS), null, SUS_EXT_PKG_AID);
        openCardSim.installApplet(getAbsolutePath(capPath + "/" + CAP_FIRA_SERVICE_APPLET),
                SERVICE_APPLET_AID, SERVICE_APPLET_PKG_AID);
        openCardSim.installApplet(getAbsolutePath(capPath + "/" + CAP_FIRA_SECURECHANNEL), null,
                SC_PKG_AID);
        openCardSim.installApplet(getAbsolutePath(capPath + "/" + CAP_FIRA_APPLET), FIRA_APPLET_AID,
                FIRA_APPLET_PKG_AID);
        openCardSim.installApplet(getAbsolutePath(capPath + "/" + CAP_SUS_APPLET), SUS_APPLET_AID,
                SUS_APPLET_PKG_AID);
    }

    private void installIC(String capPath) throws JCOPException {
        openCardSim.installApplet(getAbsolutePath(capPath + "/" + CAP_IC_SEPRIVIDER), null,
                IC_SEPROVIDER_PKG_AID);
        openCardSim.installApplet(getAbsolutePath(capPath + "/" + CAP_IC_KEYMASTER),
                IC_KEYMASTER_AID, IC_KEYMASTER_PKG_AID);
        openCardSim.installApplet(getAbsolutePath(capPath + "/" + CAP_IC_READY), IC_READY_AID,
                IC_READY_PKG_AID);
    }

    @Override
    public void setupSimulator(String[] target, String pathToCapFiles) throws Exception {
        try {
            for (String name : target) {
                switch (name) {
                case "keymaster":
                    installKeymaster(pathToCapFiles);
                    break;
                case "fira":
                    installFira(pathToCapFiles);
                    break;
                case "weaver":
                    installWeaver(pathToCapFiles);
                    break;
                case "ic":
                    installIC(pathToCapFiles);
                    break;
                default:
                    // Ignore already handled in main function
                    break;
                }
            }
        } catch (JCOPException e) {
            openCardSim.close();
            throw new JCOPException(e.getMessage());
        }
    }

    private final byte[] intToByteArray(int value) {
        return new byte[] { (byte) (value >>> 8), (byte) value };
    }

    private javax.smartcardio.CommandAPDU validateApdu(byte[] apdu)
            throws IllegalArgumentException {
        javax.smartcardio.CommandAPDU apduCmd = new javax.smartcardio.CommandAPDU(apdu);
        return apduCmd;
    }

    @Override
    public byte[] executeApdu(byte[] apdu) throws Exception {
        System.out.println("Executing APDU = " + Utils.byteArrayToHexString(apdu));
        if (null == validateApdu(apdu)) {
            throw new IllegalArgumentException();
        }
        opencard.core.terminal.CommandAPDU cmdApdu = new opencard.core.terminal.CommandAPDU(apdu);
        response = openCardSim.transmitCommand(cmdApdu);
        System.out.println("Status = " + Utils.byteArrayToHexString(intToByteArray(response.sw())));
        return intToByteArray(response.sw());
    }

    @Override
    public byte[] decodeDataOut() {
        return response.getBytes();
    }

}
