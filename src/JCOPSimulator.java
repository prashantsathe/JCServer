import opencard.core.terminal.CommandAPDU;

public class JCOPSimulator implements Simulator {

  private JCOPOpenCard openCardSim = null;

  private byte[] keymasterAppletId;
  private byte[] keymasterAppletPackage;
  private String capFilePath;

  private opencard.core.terminal.ResponseAPDU response;

  public JCOPSimulator(String pckgAid, String appletAid, String capFilePath) {
    keymasterAppletPackage = Utils.hexStringToByteArray(pckgAid);
    keymasterAppletId = Utils.hexStringToByteArray(appletAid);
    this.capFilePath = capFilePath;
  }

  @Override
  public void initaliseSimulator() throws Exception {
    openCardSim = JCOPOpenCard.getInstance();
    if (!openCardSim.isConnected()) {
      try {
        openCardSim.connect();
        // openCardSim.deleteApplet(keymasterAppletPackage);
        //openCardSim.installApplet(capFilePath, keymasterAppletId, keymasterAppletPackage);
      } catch (JCOPException e) {
        openCardSim.close();
        throw new JCOPException(e.getMessage());
      }
    }
  }

  @Override
  public void disconnectSimulator() throws Exception {
    openCardSim.deleteApplet(keymasterAppletPackage);
    openCardSim.close();
  }

  @Override
  public boolean setupKeymasterOnSimulator() throws Exception {
    openCardSim.selectApplet(keymasterAppletId);
    return true;
  }

  private final byte[] intToByteArray(int value) {
    return new byte[] { (byte) (value >>> 8), (byte) value };
  }

  private javax.smartcardio.CommandAPDU validateApdu(byte[] apdu) throws IllegalArgumentException {
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

  private byte[] processApdu(byte[] apdu) {
    if (apdu[4] == 0x00 && apdu.length > 256) {
      byte[] returnApdu = new byte[apdu.length - 3];
      for (int i = 0; i < returnApdu.length; i++)
        returnApdu[i] = apdu[i];
      return returnApdu;// Expecting incoming apdu is already extended apdu
    }
    if (apdu.length == 6 && apdu[4] == (byte) 0 && apdu[5] == (byte) 0) {
      byte[] returnApdu = new byte[5];
      for (int i = 0; i < 5; i++)
        returnApdu[i] = apdu[i];
      return returnApdu;
    } else {
      // return apdu;
    }
    if (apdu[4] == (byte) 0)
      return apdu;
    byte[] finalApdu = new byte[apdu.length + 1];
    System.out.println("Incoming APDU = " + Utils.byteArrayToHexString(apdu));
    for (int i = 0; i < apdu.length; i++) {
      if (i < 4) {
        finalApdu[i] = apdu[i];
      } else if (i == apdu.length - 1 && apdu[i] == 0) {
      } else if (i > 4) {
        finalApdu[i + 2] = apdu[i];
      } else if (i == 4) {
        finalApdu[4] = (byte) 0;
        finalApdu[5] = (byte) 0x00;
        finalApdu[6] = apdu[i];
      }
    }
    return finalApdu;
  }

  @Override
  public byte[] decodeDataOut() {
    // TODO Auto-generated method stub
    return response.getBytes();
  }

}
