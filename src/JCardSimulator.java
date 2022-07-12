import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import com.android.javacard.keymaster.KMArray;
import com.android.javacard.keymaster.KMByteBlob;
import com.android.javacard.keymaster.KMSEProvider;
import com.android.javacard.keymaster.KMDecoder;
import com.android.javacard.keymaster.KMEncoder;
import com.android.javacard.keymaster.KMEnum;
import com.android.javacard.keymaster.KMEnumTag;
import com.android.javacard.keymaster.KMInteger;
import com.android.javacard.keymaster.KMJCardSimApplet;
import com.android.javacard.keymaster.KMKeyParameters;
import com.android.javacard.keymaster.KMKeymasterApplet;
import com.android.javacard.keymaster.KMType;
import com.licel.jcardsim.bouncycastle.util.Arrays;
import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;

import javacard.framework.AID;
import javacard.framework.Util;
import javacard.framework.Applet;

public class JCardSimulator implements Simulator {

  private KMSEProvider sim;
  private CardSimulator simulator;
  private KMEncoder encoder;
  private KMDecoder decoder;
  private KMSEProvider cryptoProvider;
  ResponseAPDU response;

  public JCardSimulator() {
    // cryptoProvider = KMCryptoProviderImpl.instance();
    // sim = KMCryptoProviderImpl.instance();
    simulator = new CardSimulator();
    encoder = new KMEncoder();
    decoder = new KMDecoder();
  }

  @Override
  public void initaliseSimulator() throws Exception {
    // Create simulator
    // KMJcardSimulator.jcardSim = true;
    AID appletAID1 = AIDUtil.create("A000000062");
    simulator.installApplet(appletAID1, KMJCardSimApplet.class);
  }

  private boolean provisionCmd(CardSimulator simulator) {
    // Argument 1
    short arrPtr = KMArray.instance((short) 1);
    KMArray vals = KMArray.cast(arrPtr);
    vals.add((short) 0, KMEnumTag.instance(KMType.ALGORITHM, KMType.RSA));
    short keyparamsPtr = KMKeyParameters.instance(arrPtr);
    // Argument 2
    short keyFormatPtr = KMEnum.instance(KMType.KEY_FORMAT, KMType.X509);
    // Argument 3
    byte[] byteBlob = new byte[48];
    for (short i = 0; i < 48; i++) {
      byteBlob[i] = (byte) i;
    }
    short keyBlobPtr = KMByteBlob.instance(byteBlob, (short) 0, (short) byteBlob.length);
    // Array of expected arguments
    short argPtr = KMArray.instance((short) 3);
    KMArray arg = KMArray.cast(argPtr);
    arg.add((short) 0, keyparamsPtr);
    arg.add((short) 1, keyFormatPtr);
    arg.add((short) 2, keyBlobPtr);
    CommandAPDU apdu = encodeApdu((byte) 0x23, argPtr);
    // print(commandAPDU.getBytes());
    ResponseAPDU response = simulator.transmitCommand(apdu);
    return 0x9000 == response.getSW();
  }

  private CommandAPDU encodeApdu(byte ins, short cmd) {
    byte[] buf = new byte[1024];
    buf[0] = (byte) 0x80;
    buf[1] = ins;
    buf[2] = (byte) 0x40;
    buf[3] = (byte) 0x00;
    buf[4] = 0;
    short len = encoder.encode(cmd, buf, (short) 7);
    Util.setShort(buf, (short) 5, len);
    byte[] apdu = new byte[7 + len];
    Util.arrayCopyNonAtomic(buf, (short) 0, apdu, (short) 0, (short) (7 + len));
    // CommandAPDU commandAPDU = new CommandAPDU(0x80, 0x10, 0x40, 0x00, buf, 0,
    // actualLen);
    return new CommandAPDU(apdu);
  }

  private boolean setBootParams(CardSimulator simulator) {
    // Argument 1 OS Version
    short versionPatchPtr = KMInteger.uint_16((short) 1);
//    short versionTagPtr = KMIntegerTag.instance(KMType.UINT_TAG, KMType.OS_VERSION,versionPatchPtr);
    // Argument 2 OS Patch level
//    short patchTagPtr = KMIntegerTag.instance(KMType.UINT_TAG, KMType.OS_PATCH_LEVEL, versionPatchPtr);
    // Argument 3 Verified Boot Key
    byte[] bootKeyHash = "00011122233344455566677788899900".getBytes();
    short bootKeyPtr = KMByteBlob.instance(bootKeyHash, (short) 0, (short) bootKeyHash.length);
    // Argument 4 Verified Boot Hash
    short bootHashPtr = KMByteBlob.instance(bootKeyHash, (short) 0, (short) bootKeyHash.length);
    // Argument 5 Verified Boot State
    short bootStatePtr = KMEnum.instance(KMType.VERIFIED_BOOT_STATE, KMType.VERIFIED_BOOT);
    // Argument 6 Device Locked
    short deviceLockedPtr = KMEnum.instance(KMType.DEVICE_LOCKED, KMType.DEVICE_LOCKED_FALSE);
    // Arguments
    short arrPtr = KMArray.instance((short) 6);
    KMArray vals = KMArray.cast(arrPtr);
    vals.add((short) 0, versionPatchPtr);
    vals.add((short) 1, versionPatchPtr);
    vals.add((short) 2, bootKeyPtr);
    vals.add((short) 3, bootHashPtr);
    vals.add((short) 4, bootStatePtr);
    vals.add((short) 5, deviceLockedPtr);
    CommandAPDU apdu = encodeApdu((byte) 0x24, arrPtr);
    // print(commandAPDU.getBytes());
    ResponseAPDU response = simulator.transmitCommand(apdu);
    return 0x9000 == response.getSW();
  }

  @Override
  public void disconnectSimulator() throws Exception {
    AID appletAID1 = AIDUtil.create("A000000062");
    // Delete i.e. uninstall applet
    simulator.deleteApplet(appletAID1);
  }

  @Override
  public boolean setupKeymasterOnSimulator() throws Exception {
    AID appletAID1 = AIDUtil.create("A000000062");
    // Select applet
    simulator.selectApplet(appletAID1);
    // provision attest key
    // return provisionCmd(simulator);// && setBootParams(simulator);
    return true;
  }

  private final byte[] intToByteArray(int value) {
    return new byte[] { (byte) (value >>> 8), (byte) value };
  }

  @Override
  public byte[] executeApdu(byte[] apdu) throws Exception {
    System.out.println("Executing APDU = " + Utils.byteArrayToHexString(apdu));
    CommandAPDU apduCmd = new CommandAPDU(apdu);
    response = simulator.transmitCommand(apduCmd);
    System.out.println("Status = " + Utils.byteArrayToHexString(intToByteArray(response.getSW())));
    return intToByteArray(response.getSW());
  }

  private byte[] processApdu(byte[] apdu) {
    //if (apdu.length > 256) {
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
    return response.getData();
  }

}
