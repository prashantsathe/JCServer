package com.android.jcserver;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import com.android.javacard.keymaster.KMJCardSimApplet;
import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;
import static com.android.jcserver.config.*;
import static com.android.jcserver.Utils.*;

import java.io.IOException;
import java.util.Vector;

import javacard.framework.AID;
import javacard.framework.ISO7816;


public class JCardSimulator implements Simulator {

    private CardSimulator simulator;
    private ResponseAPDU response;
    private Vector<String> channelAid;
    private int currentChannel;

    public JCardSimulator() {
        // Creating an empty Vector 
        channelAid = new Vector<String>(MAX_LOGICAL_CHANNEL);
        channelAid.add("ZeroChannelOccupied");
        for (int ch = 1; ch < MAX_LOGICAL_CHANNEL; ch++) {
            channelAid.add(null);
        }
        currentChannel = -1;
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

    private void installWeaver() throws JCOPException {
    }

    private void installIC() throws JCOPException {
    }

    @Override
    public void setupSimulator(String[] target, String pathToCapFiles) throws Exception {
        // TODO add Weaver
        for (String name : target) {
            switch (name) {
            case "keymaster":
                installKeymaster();
                break;
            case "fira":
                installFira();
                break;
            case "weaver":
                installWeaver();
                break;
            case "ic":
                installIC();
                break;
            default:
                // Ignore already handled in main function
                break;
            }
        }
    }

    private final byte[] intToByteArray(int value) {
        return new byte[] { (byte) (value >>> 8), (byte) value };
    }

    private byte getchannelNumber(byte cla) throws IOException {
        byte ch = (byte) (cla & 0x03);
        boolean b7 = (cla & 0x40) == (byte) 0x40;

        // b7 = 1 indicates the inter-industry class byte coding
        if (b7) {
            ch -= 4;
        }

        if (!(ch >= (byte) 0x00 && ch <= (byte) 0x14)) {
            throw new IOException("class byte error");
        }

        return ch;
    }

    private ResponseAPDU processManageCommand(byte[] apdu) {
        int ch, maxCH = channelAid.size();

        // Close the channel if p1 = 0x80
        if (apdu[ISO7816.OFFSET_P1] == (byte) 0x80) {
            channelAid.set(apdu[ISO7816.OFFSET_P2], null);
            return new ResponseAPDU(new byte[] {(byte) 0x90, 0x00});
        }

        for (ch = 1; ch < maxCH; ch++) {
            if (channelAid.get(ch) == null)
                break;
        }

        if (ch >= maxCH) {
            return new ResponseAPDU(new byte[] {(byte) 0x68, (byte) 0x81});
        }

        currentChannel = ch;
        return new ResponseAPDU(new byte[] {(byte) ch, (byte) 0x90, 0x00});
    }

    /*
     * Jcard Simulator design is based on one applet and one channel at a time
     *
     * In order to communicate with multiple applets simultaneously on different channels,
     * we have added Logical channels implementation here. For following implementation it requires
     * following variables 
     *  - Vector[AID] (index 0 represent channel 0... so on)
     *  - CurrentChannelnumber
     * Generalized flow between SE hal and SE applet via JCserver is as follow
     *
     *    SE HAL                     JCServer                                     JcardSim
     *  ------------------------------------------------------------------------------------------
     *  Managechannel ->         check if any channel is 
     *                           free, if yes set occupied
     *                           and return channel number.
     *                           Else Error
     *
     *  Select Cmd    ->         select Command                              -->    select cmd
     *
     *                           if success copy AID to
     *                           respective array and set
     *                           CurrentChannelnumber = CH(CLA)
     *
     *
     *  Non-Select Cmd ->      if (CH(CLA) == CurrentChannelnumber)
     *                             send "Non-Select" cmd                    --> "Non-Select" cmd
     *                         else 
     *                             send "select(AID(CH(CLA))"               -->  select cmd
     *                                  "CurrentChannelnumber = CH(CLA)"
     *                             send "Non-Select" cmd                    --> "Non-Select" cmd
     */
    @Override
    public byte[] executeApdu(byte[] apdu) throws Exception {

        System.out.println("Executing APDU = " + Utils.byteArrayToHexString(apdu));

        // Check if ManageChannel Command
        if (apdu[ISO7816.OFFSET_INS] == INS_MANAGE_CHANNEL) {
            response = processManageCommand(apdu);
        } else {
            CommandAPDU apduCmd = new CommandAPDU(apdu);
            byte ch = getchannelNumber((byte) apduCmd.getCLA());

            if (ch == currentChannel || (byte) apduCmd.getINS() == INS_SELECT) {
                response = simulator.transmitCommand(apduCmd);
                // save AIDs if command is select
                if ((byte) apduCmd.getINS() == INS_SELECT && response.getSW() == (int) 0x9000) {
                    channelAid.set(ch, byteArrayToHexString(apdu, ISO7816.OFFSET_CDATA,
                            apdu[ISO7816.OFFSET_LC]));
                    currentChannel = ch;
                }
            } else {
                // send select command
                byte[] aid = hexStringToByteArray(channelAid.get(ch));
                byte[] selApdu = new byte[6 + aid.length];
                selApdu[0] = 0x00;
                selApdu[1] = INS_SELECT;
                selApdu[2] = (byte) 0x04;
                selApdu[3] = (byte) 0x00;
                selApdu[4] = (byte) aid.length;
                System.arraycopy(aid, 0, selApdu, 5, aid.length);
                selApdu[selApdu.length - 1] = 0x00;

                CommandAPDU selectCmd = new CommandAPDU(selApdu);
                response = simulator.transmitCommand(selectCmd);
                if (response.getSW() == 0x9000) {
                    currentChannel = ch;
                    response = simulator.transmitCommand(apduCmd);
                }
            }

            System.out.println(
                "Status = " + Utils.byteArrayToHexString(intToByteArray(response.getSW())));
        }
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
