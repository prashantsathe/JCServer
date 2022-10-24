package com.android.jcserver;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import com.android.javacard.keymaster.KMJCardSimApplet;
import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;
import static com.android.jcserver.config.*;
import javacard.framework.*;

public class JCardSimulator implements Simulator {

    private CardSimulator simulator;
    ResponseAPDU response;

    public JCardSimulator() {
    }

    @Override
    public void initaliseSimulator() throws Exception {
        // Create simulator
        simulator = new CardSimulator();
    }

    @Override
    public void disconnectSimulator() throws Exception {
        AID appletAID1 = AIDUtil.create(keymasterAid);
        simulator.deleteApplet(appletAID1);
    }

    private void installKeymaster() throws JCOPException {
        AID appletAID1 = AIDUtil.create(keymasterAid);
        simulator.installApplet(appletAID1, KMJCardSimApplet.class);
    }

    private void installFira() throws JCOPException {
    }

    @Override
    public void setupSimulator(String target) throws Exception {
        if (target.equals("keymaster")) {
            installKeymaster();
        } else if (target.equals("fira")) {
            installFira();
        }
    }

    private final byte[] intToByteArray(int value) {
        return new byte[] { (byte) (value >>> 8), (byte) value };
    }

    @Override
    public byte[] executeApdu(byte[] apdu) throws Exception {
        System.out.println("Executing APDU = " + Utils.byteArrayToHexString(apdu));
        CommandAPDU apduCmd = new CommandAPDU(apdu);
        response = simulator.transmitCommand(apduCmd);
        System.out.println(
                "Status = " + Utils.byteArrayToHexString(intToByteArray(response.getSW())));
        return intToByteArray(response.getSW());
    }

    @Override
    public byte[] decodeDataOut() {
        byte[] resp  = response.getData();
        byte[] status = intToByteArray(response.getSW());
        byte[] out = new byte[(resp.length + status.length)];
        System.arraycopy(resp, 0, out, 0, resp.length);
        System.arraycopy(status, 0, out, resp.length, status.length);
        return out;
    }

}
