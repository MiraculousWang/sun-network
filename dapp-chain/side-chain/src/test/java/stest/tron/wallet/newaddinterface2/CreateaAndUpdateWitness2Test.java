package stest.tron.wallet.newaddinterface2;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;
import stest.tron.wallet.common.client.utils.TransactionUtils;
import stest.tron.wallet.common.client.utils.TransactionUtilsForDailybuild;

//import stest.tron.wallet.common.client.AccountComparator;

@Slf4j
public class CreateaAndUpdateWitness2Test {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");

  private static final byte[] INVAILD_ADDRESS = Base58
      .decodeFromBase58Check("27cu1ozb4mX3m2afY68FSAqn3HmMp815d48");

  private final byte[] fromAddress = PublicMethedForDailybuild.getFinalAddress(testKey002);
  private static final Long costForCreateWitness = 9999000000L;
  String createWitnessUrl = "http://www.createwitnessurl.com";
  String updateWitnessUrl = "http://www.updatewitnessurl.com";
  String nullUrl = "";
  String spaceUrl = "          ##################~!@#$%^&*()_+}{|:'/.,<>?|]=-";
  byte[] createUrl = createWitnessUrl.getBytes();
  byte[] updateUrl = updateWitnessUrl.getBytes();
  byte[] wrongUrl = nullUrl.getBytes();
  byte[] updateSpaceUrl = spaceUrl.getBytes();
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  //get account
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] lowBalAddress = ecKey.getAddress();
  String lowBalTest = ByteArray.toHexString(ecKey.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    logger.info(lowBalTest);
    logger.info(ByteArray.toHexString(PublicMethedForDailybuild.getFinalAddress(lowBalTest)));
    logger.info(Base58.encode58Check(PublicMethedForDailybuild.getFinalAddress(lowBalTest)));

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test
  public void testInvaildToApplyBecomeWitness2() {
    GrpcAPI.Return ret1 = createWitness2(INVAILD_ADDRESS, createUrl, testKey002);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Invalid address");
  }

  @Test(enabled = true)
  public void testCreateWitness2() {
    //If you are already is witness, apply failed
    createWitness(fromAddress, createUrl, testKey002);
    GrpcAPI.Return ret1 = createWitness2(fromAddress, createUrl, testKey002);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Witness[415624c12e308b03a1a6b21d9b86e3942fac1ab92b] "
            + "has existed");
    //balance is not enouhg,try to create witness.
    Assert.assertTrue(sendcoin(lowBalAddress, 1000000L, fromAddress, testKey002));
    ret1 = createWitness2(lowBalAddress, createUrl, lowBalTest);
    Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : balance < AccountUpgradeCost");
    //Send enough coin to the apply account to make that account
    // has ability to apply become witness.
    WitnessList witnesslist = blockingStubFull
        .listWitnesses(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<WitnessList> result = Optional.ofNullable(witnesslist);
    WitnessList witnessList = result.get();
    if (result.get().getWitnessesCount() < 6) {
      Assert.assertTrue(sendcoin(lowBalAddress, costForCreateWitness, fromAddress, testKey002));
      ret1 = createWitness2(lowBalAddress, createUrl, lowBalTest);
      Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.SUCCESS);
      Assert.assertEquals(ret1.getMessage().toStringUtf8(), "");
    }
  }

  @Test(enabled = true)
  public void testUpdateWitness2() {
    WitnessList witnesslist = blockingStubFull
        .listWitnesses(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<WitnessList> result = Optional.ofNullable(witnesslist);
    WitnessList witnessList = result.get();
    if (result.get().getWitnessesCount() < 6) {
      //null url, update failed
      GrpcAPI.Return ret1 = updateWitness2(lowBalAddress, wrongUrl, lowBalTest);
      Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR);
      Assert.assertEquals(ret1.getMessage().toStringUtf8(),
          "contract validate error : Invalid url");
      //Content space and special char, update success
      ret1 = updateWitness2(lowBalAddress, updateSpaceUrl, lowBalTest);
      Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.SUCCESS);
      Assert.assertEquals(ret1.getMessage().toStringUtf8(), "");
      //update success
      ret1 = updateWitness2(lowBalAddress, updateUrl, lowBalTest);
      Assert.assertEquals(ret1.getCode(), GrpcAPI.Return.response_code.SUCCESS);
      Assert.assertEquals(ret1.getMessage().toStringUtf8(), "");
    } else {
      logger.info("Update witness case had been test.This time skip it.");
    }
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * constructor.
   */

  public Boolean createWitness(byte[] owner, byte[] url, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.WitnessCreateContract.Builder builder = Contract.WitnessCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUrl(ByteString.copyFrom(url));
    Contract.WitnessCreateContract contract = builder.build();

    Protocol.Transaction transaction = blockingStubFull.createWitness(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    } else {
      return true;
    }

  }

  /**
   * constructor.
   */

  public GrpcAPI.Return createWitness2(byte[] owner, byte[] url, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.WitnessCreateContract.Builder builder = Contract.WitnessCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUrl(ByteString.copyFrom(url));
    Contract.WitnessCreateContract contract = builder.build();

    GrpcAPI.TransactionExtention transactionExtention = blockingStubFull.createWitness2(contract);

    if (transactionExtention == null) {
      return transactionExtention.getResult();
    }
    GrpcAPI.Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return ret;
    } else {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
    }
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return transactionExtention.getResult();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return response;
    }
    return ret;

  }

  /**
   * constructor.
   */

  public Boolean updateWitness(byte[] owner, byte[] url, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.WitnessUpdateContract.Builder builder = Contract.WitnessUpdateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUpdateUrl(ByteString.copyFrom(url));
    Contract.WitnessUpdateContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.updateWitness(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      logger.info("response.getRestult() == false");
      return false;
    } else {
      return true;
    }

  }

  /**
   * constructor.
   */

  public GrpcAPI.Return updateWitness2(byte[] owner, byte[] url, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.WitnessUpdateContract.Builder builder = Contract.WitnessUpdateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUpdateUrl(ByteString.copyFrom(url));
    Contract.WitnessUpdateContract contract = builder.build();

    GrpcAPI.TransactionExtention transactionExtention = blockingStubFull.updateWitness2(contract);
    if (transactionExtention == null) {
      return transactionExtention.getResult();
    }
    GrpcAPI.Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return ret;
    } else {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
    }
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return transactionExtention.getResult();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      logger.info("response.getRestult() == false");
      return response;
    }
    return ret;
  }

  /**
   * constructor.
   */

  public Boolean sendcoin(byte[] to, long amount, byte[] owner, String priKey) {

    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    Contract.TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    } else {
      return true;
    }
  }

  /**
   * constructor.
   */

  public Account queryAccount(String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    if (ecKey == null) {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
  }

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  /**
   * constructor.
   */

  public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /**
   * constructor.
   */

  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());

  }

  private Protocol.Transaction signTransaction(ECKey ecKey, Protocol.Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtilsForDailybuild
        .sign(transaction, ecKey, PublicMethed.getMaingatewayByteAddr(),false);
  }
}


