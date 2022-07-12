import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import opencard.core.service.CardChannel;
import opencard.core.service.CardServiceException;
import opencard.core.service.CardServiceRegistry;
import opencard.core.service.CardServiceScheduler;
import opencard.core.service.InvalidCardChannelException;
import opencard.core.service.SmartCard;
import opencard.core.terminal.CardID;
import opencard.core.terminal.CardTerminal;
import opencard.core.terminal.CardTerminalException;
import opencard.core.terminal.CommandAPDU;
import opencard.core.terminal.ResponseAPDU;
import opencard.core.terminal.SlotChannel;
import opencard.opt.applet.AppletID;

import com.sun.javacard.apduio.CadTransportException;

import de.cardcontact.opencard.factory.GlobalPlatformCardServiceFactory;
import de.cardcontact.opencard.service.globalplatform.SecurityDomainCardService;
import de.cardcontact.opencard.terminal.jcopsim.JCOPSimCardTerminal;
import de.cardcontact.opencard.utils.CapFile;

public class JCOPOpenCard {

  public static final int port = 8050;
  public static final String address = "localhost";
  private boolean isConnected = false;
  SecurityDomainCardService sds = null;
  JCOPSimCardTerminal terminal = null;
  SlotChannel slotChannel = null;

  static byte[] secDomainAid = Utils.hexStringToByteArray("A000000151000000");
  public static final int KEY_DIVERSIFICATION_LEN = 10;
  public static final int KEY_INFO_LEN = 2;
  public static final int SEQ_COUNTER_LEN = 2;
  public static final int CARD_CHALLENGE_LEN = 6;
  public static final int CRYPTO_GRAM_LEN = 8;
  public static final byte[] KEY_SET = { 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C,
      0x4D, 0x4E, 0x4F };
  static byte[] hostChallenge = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
  // Session Keys
  static byte[] cmacSessionKey = new byte[16];
  static byte[] rmacSessionKey = new byte[16];
  static byte[] encSessionKey = new byte[16];
  static byte[] dekSessionKey = new byte[16];
  // authData
  static byte[] authData = null;

  private static JCOPOpenCard kJcopSimulator = null;

  public static JCOPOpenCard getInstance() {
    if (kJcopSimulator == null)
      kJcopSimulator = new JCOPOpenCard();
    return kJcopSimulator;
  }

  private JCOPOpenCard() {

  }

  public static byte[] calculateCMac(byte[] plainData, byte[] key, byte[] iv) throws JCOPException {
    byte[] mac = null;
    try {
      byte[] key1 = Arrays.copyOf(key, 8);
      byte[] key2 = Arrays.copyOfRange(key, 8, 16);
      SecretKey skA = new SecretKeySpec(key1, "DES");
      Cipher cA = Cipher.getInstance("DES/CBC/NoPadding");
      cA.init(Cipher.ENCRYPT_MODE, skA, new IvParameterSpec(iv));

      SecretKey skB = new SecretKeySpec(key2, "DES");
      Cipher cB = Cipher.getInstance("DES/CBC/NoPadding");
      cB.init(Cipher.DECRYPT_MODE, skB, new IvParameterSpec(iv));

      byte[] encData = cA.doFinal(plainData);
      byte[] lastBlock = new byte[8];
      System.arraycopy(encData, encData.length - 8, lastBlock, 0, 8);
      lastBlock = cB.doFinal(lastBlock);

      mac = cA.doFinal(lastBlock);

    } catch (Exception e) {
      throw new JCOPException("Failed to calculate CMAC.");
    }
    return mac;
  }

  public static byte[] desEncrypt(byte[] key, byte[] plainData, byte[] icv) throws JCOPException {
    try {
      key = Arrays.copyOf(key, 24);
      for (int i = 0; i < 8; i++) {
        key[16 + i] = key[i];
      }
      DESedeKeySpec keySpec = new DESedeKeySpec(key);
      SecretKeyFactory factory = SecretKeyFactory.getInstance("DESede");
      SecretKey secretKey = factory.generateSecret(keySpec);
      Cipher c = Cipher.getInstance("DESede/CBC/NoPadding", "BC");
      c.init(1, secretKey, new IvParameterSpec(icv));
      return c.doFinal(plainData);
    } catch (InvalidKeyException e) {
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    } catch (InvalidKeySpecException e) {
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    } catch (NoSuchProviderException e) {
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    } catch (NoSuchPaddingException e) {
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    } catch (InvalidAlgorithmParameterException e) {
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    } catch (IllegalBlockSizeException e) {
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    } catch (BadPaddingException e) {
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    }
  }

  public static void deriveSessionKeys(byte scHigh, byte scLow) throws JCOPException {
    cmacSessionKey[0] = 1;
    cmacSessionKey[1] = 1;
    cmacSessionKey[2] = scHigh;
    cmacSessionKey[3] = scLow;
    System.arraycopy(cmacSessionKey, 0, rmacSessionKey, 0, 4);
    rmacSessionKey[1] = 2;
    System.arraycopy(cmacSessionKey, 0, encSessionKey, 0, 4);
    encSessionKey[1] = -126;
    System.arraycopy(cmacSessionKey, 0, dekSessionKey, 0, 4);
    dekSessionKey[1] = -127;
    byte[] icv = new byte[8];
    Arrays.fill(icv, (byte) 0);
    cmacSessionKey = desEncrypt(KEY_SET, cmacSessionKey, icv);
    rmacSessionKey = desEncrypt(KEY_SET, rmacSessionKey, icv);
    encSessionKey = desEncrypt(KEY_SET, encSessionKey, icv);
    dekSessionKey = desEncrypt(KEY_SET, dekSessionKey, icv);
  }

  public static ResponseAPDU doExternalAutenticate(SecurityDomainCardService sds) throws JCOPException {
    byte[] apdu = new byte[30];
    apdu[0] = (byte) 0x84; // CLA
    apdu[1] = (byte) 0x82; // INS
    apdu[2] = (byte) 0x00; // P1 //Security level (No Secure Messaging).
    apdu[3] = (byte) 0x00; // P2

    // LC field shoud be 16.
    apdu[4] = 0x10;
    System.arraycopy(authData, 0, apdu, 5, authData.length);

    // Do Padding
    apdu[5 + authData.length] = (byte) 0x80;

    // Do Padding of zero's
    int totalLen = 5 + authData.length + 1;
    while (totalLen % 8 != 0)
      apdu[totalLen++] = 0x00;

    // ICV set to zero
    byte[] icv = new byte[8];
    icv = calculateCMac(Arrays.copyOfRange(apdu, 0, totalLen), cmacSessionKey, icv);

    byte[] data = new byte[16];
    System.arraycopy(authData, 0, data, 0, authData.length);
    System.arraycopy(icv, 0, data, 8, 8);
    try {
      return sds.externalAuthenticate((byte) 0x00, data);
    } catch (InvalidCardChannelException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    } catch (CardTerminalException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    } catch (CardServiceException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    }

  }

  public static void processInitializeUpdateResponse(SecurityDomainCardService sds, byte[] resp) throws JCOPException {
    byte[] seqCounter = new byte[SEQ_COUNTER_LEN];
    byte[] cardChallenge = new byte[SEQ_COUNTER_LEN + CARD_CHALLENGE_LEN];
    byte[] cardCryptogram = new byte[CRYPTO_GRAM_LEN];

    System.arraycopy(resp, KEY_DIVERSIFICATION_LEN + KEY_INFO_LEN, cardChallenge, 0,
        SEQ_COUNTER_LEN + CARD_CHALLENGE_LEN);
    System.arraycopy(resp, KEY_DIVERSIFICATION_LEN + KEY_INFO_LEN, seqCounter, 0, SEQ_COUNTER_LEN);
    System.arraycopy(resp, KEY_DIVERSIFICATION_LEN + KEY_INFO_LEN + SEQ_COUNTER_LEN + CARD_CHALLENGE_LEN,
        cardCryptogram, 0, CRYPTO_GRAM_LEN);

    deriveSessionKeys(seqCounter[0], seqCounter[1]);
    byte[] t = new byte[24];
    System.arraycopy(hostChallenge, 0, t, 0, 8);
    System.arraycopy(cardChallenge, 0, t, 8, 8);
    t[16] = Byte.MIN_VALUE;
    byte[] icv = new byte[8];
    Arrays.fill(icv, (byte) 0);
    byte[] encIcv = desEncrypt(encSessionKey, t, icv);
    icv = Arrays.copyOfRange(encIcv, encIcv.length - 8, encIcv.length);
    if (Arrays.equals(icv, cardCryptogram)) {
      System.out.println("Comparision of CardCryptogram success.");
    } else {
      System.out.println("Card cryptogram invalid.");
      throw new JCOPException("Card cryptogram invalid.");
    }

    authData = new byte[8];
    System.arraycopy(cardChallenge, 0, t, 0, 8);
    System.arraycopy(hostChallenge, 0, t, 8, 8);
    byte[] encAuthData = desEncrypt(encSessionKey, t, authData);
    authData = Arrays.copyOfRange(encAuthData, encAuthData.length - 8, encAuthData.length);
    System.out.println("authData: " + Utils.byteArrayToHexString(authData));

  }

  public static void doAuthentication(SmartCard sc) {
    SecurityDomainCardService sds;
    try {
      sds = (SecurityDomainCardService) sc.getCardService(SecurityDomainCardService.class, false);
      // INITIATE-UPDATE
      ResponseAPDU respApdu = sds.initializeUpdate((byte) 0x00, (byte) 0x00, hostChallenge);
      if (respApdu.sw() == 0x9000) {
        System.out.println("init-update is success" + Utils.byteArrayToHexString(respApdu.getBuffer()));
      } else {
        System.out.println(" Failed to init-update");
      }

      processInitializeUpdateResponse(sds, respApdu.getBuffer());

      // EXTERNAL-AUTHENTICATE
      if (authData != null) {
        ResponseAPDU resp = doExternalAutenticate(sds);
        if (resp.sw() == 0x9000) {
          System.out.println("ext-auth is success" + Utils.byteArrayToHexString(resp.getBuffer()));
        } else {
          System.out.println(" Failed to init-update");
        }
      } else
        throw new JCOPException("authData is null, Failed to do ExtAuth.");
    } catch (CardServiceException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidCardChannelException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (CardTerminalException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JCOPException e) {
      e.printStackTrace();
    }
  }

  public void installApplet(String capfile, byte[] appletAid, byte[] packageAid) throws JCOPException {
    try {
      // Load and install applet
      // 1. Load for install
      ResponseAPDU resp = sds.installForLoad(packageAid, secDomainAid, null, null, null);
      if (resp.sw() != 0x9000) {
        System.out.println("Failed to installForLoad. error:" + resp.sw());
        return;
      }
      System.out.println("Load for install is successfully done.");
      // 2. Load the package.
      CapFile cap = new CapFile();
      cap.read(capfile);
      resp = sds.load(cap);
      if (resp.sw() != 0x9000) {
        System.out.println("Failed to load CAP file exit. error:" + resp.sw());
        throw new JCOPException("Failed to load CAP FILE");
      }
      System.out.println("CAP file loaded successfully.");

      // Install applet.
      byte[] privileges = new byte[] { 0x00 };
      byte[] installParam = new byte[] { (byte) 0xC9, 0x00 };
      resp = sds.installForInstall(packageAid, appletAid, appletAid, privileges, installParam, null, true);
      if (resp.sw() != 0x9000) {
        System.out.println("Failed to install Applet. error:" + resp.sw());
        return;
      }
      System.out.println("Applet installed successfully.");
    } catch (CardTerminalException e) {
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    }
  }

  public void deleteApplet(byte[] packageAid) {
    try {
      ResponseAPDU deleteResp = sds.deleteAID(packageAid);
      if (deleteResp.sw() != 0x9000) {
        System.out.println("Failed delete applet. error:" + deleteResp.sw());
      } else {
        System.out.println("Applet deleted successfully.");
      }
    } catch (CardTerminalException e) {
      e.printStackTrace();
      // Ignore.
    }
  }

  public void selectApplet(byte[] aid) throws JCOPException {
    try {
      AppletID appId = new AppletID(aid);
      ResponseAPDU respApdu = sds.select(appId, false);
      if (respApdu.sw() != 0x9000) {
        System.out.println("Failed to select applet. error:" + respApdu.sw());
        throw new JCOPException("Failed to select applet. error:" + respApdu.sw());
      }
      System.out.println("Weaver Applet selected successfully.");
    } catch (CardTerminalException e) {
      throw new JCOPException(e.getMessage());
    }
  }

  public boolean isConnected() {
    return isConnected;
  }

  public boolean connect() throws JCOPException {

    ResponseAPDU resp = null;
    AppletID appId = null;
    Security.addProvider(new BouncyCastleProvider());
    try {
      terminal = new JCOPSimCardTerminal("jcopsimulator", "simulator", "localhost", "localhost", 8050, 0);

      terminal.connect();

      int slots = terminal.getSlots();
      System.out.println("Slots: " + slots);
      CardID cardid = terminal.getCardID(0);
      byte[] atr = cardid.getATR();
      System.out.println("ATR: " + Utils.byteArrayToHexString(atr));

      System.out.println("Get CardTerminal");
      CardTerminal ct = cardid.getCardTerminal();
      System.out.println("Open slot Channel on slot 0");
      slotChannel = ct.openSlotChannel(0);

      // Code to send init-update to card.
      GlobalPlatformCardServiceFactory gpcsf = new GlobalPlatformCardServiceFactory();
      CardServiceRegistry.getRegistry().add(gpcsf);
      CardServiceScheduler css = new CardServiceScheduler(slotChannel);
      SmartCard sc = new SmartCard(css, cardid);

      doAuthentication(sc);

      sds = (SecurityDomainCardService) sc.getCardService(SecurityDomainCardService.class, false);

      // Delete any previous instance of applet.
      // deleteApplet();
      isConnected = true;

      return true;

    } catch (CardTerminalException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    } catch (CardServiceException e) {
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      throw new JCOPException(e.getMessage());
    }
  }

  public ResponseAPDU transmitCommand(CommandAPDU apdu) {
    try {
      return sds.sendCommandAPDU(apdu);
    } catch (InvalidCardChannelException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (CardTerminalException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  public void close() throws JCOPException {
    try {

      // selectApplet(Utils.hexStringToByteArray("4A434F50546573744170706C657449"));

      if (slotChannel != null)
        slotChannel.close();

      if (terminal != null)
        terminal.close();

      isConnected = false;
    } catch (CardTerminalException e) {
      throw new JCOPException(e.getMessage());
    }
  }

}
