package stest.tron.wallet.dailybuild.trctoken;

import static org.tron.protos.Protocol.TransactionInfo.code.FAILED;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;

@Slf4j
public class ContractTrcToken043 {
  private final String tokenOwnerKey = Configuration.getByPath("testng.conf")
      .getString("tokenFoundationAccount.slideTokenOwnerKey");
  private final byte[] tokenOnwerAddress = PublicMethedForDailybuild.getFinalAddress(tokenOwnerKey);
  private final String tokenId = Configuration.getByPath("testng.conf")
      .getString("tokenFoundationAccount.slideTokenId");


  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethedForDailybuild.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private static final long now = System.currentTimeMillis();
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private static ByteString assetAccountUser = null;
  private static final long TotalSupply = 1000L;
  private byte[] transferTokenContractAddress = null;
  private byte[] resultContractAddress = null;

  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] user001Address = ecKey2.getAddress();
  private String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  private ECKey ecKey3 = new ECKey(Utils.getRandom());
  private byte[] user002Address = ecKey3.getAddress();
  private String user002Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethedForDailybuild.printAddress(dev001Key);
    PublicMethedForDailybuild.printAddress(user001Key);
    assetAccountId = ByteString.copyFromUtf8(tokenId);
    Assert.assertTrue(PublicMethedForDailybuild.transferAsset(dev001Address, assetAccountId.toByteArray(),
        10000000L, tokenOnwerAddress, tokenOwnerKey, blockingStubFull));
  }


  @Test(enabled = true, description = "TransferToken with invalid tokenId, deploy transferContract")
  public void test01DeployTransferTokenContract() {
    Assert.assertTrue(PublicMethedForDailybuild.sendcoin(dev001Address, 5048_000_000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethedForDailybuild.sendcoin(user001Address, 5048_000_000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethedForDailybuild.sendcoin(user002Address, 5048_000_000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceForReceiver(fromAddress,
        PublicMethedForDailybuild.getFreezeBalanceCount(dev001Address, dev001Key, 170000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceForReceiver(fromAddress, 10_000_000L,
        0, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    logger.info("The token name: " + tokenName);
    logger.info("The token ID: " + assetAccountId.toStringUtf8());

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethedForDailybuild.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethedForDailybuild.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountBefore = PublicMethedForDailybuild
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));
    logger.info("before AssetId: " + assetAccountId.toStringUtf8()
        + ", devAssetCountBefore: " + devAssetCountBefore);

    String filePath = "./src/test/resources/soliditycode/contractTrcToken043.sol";
    String contractName = "transferTokenContract";
    HashMap retMap = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    String transferTokenTxid = PublicMethedForDailybuild
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            assetAccountId.toStringUtf8(), 100, null, dev001Key,
            dev001Address, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethedForDailybuild
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    if (transferTokenTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    transferTokenContractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethedForDailybuild.getContract(transferTokenContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    accountResource = PublicMethedForDailybuild.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethedForDailybuild.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountAfter = PublicMethedForDailybuild
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));
    logger.info("after AssetId: " + assetAccountId.toStringUtf8()
        + ", devAssetCountAfter: " + devAssetCountAfter);

    Long contractAssetCount = PublicMethedForDailybuild.getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: "
        + contractAssetCount);

    Assert.assertEquals(Long.valueOf(100), Long.valueOf(devAssetCountBefore - devAssetCountAfter));
    Assert.assertEquals(Long.valueOf(100), contractAssetCount);
  }

  @Test(enabled = true, description = "TransferToken with invalid tokenId, deploy receive contract")
  public void test02DeployRevContract() {
    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceForReceiver(fromAddress,
        PublicMethedForDailybuild.getFreezeBalanceCount(dev001Address, dev001Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    // before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethedForDailybuild.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethedForDailybuild.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountBefore = PublicMethedForDailybuild
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balance is " + Long.toString(balanceBefore));
    logger.info("before AssetId: " + assetAccountId.toStringUtf8()
        + ", devAssetCountBefore: " + devAssetCountBefore);

    String filePath = "./src/test/resources/soliditycode/contractTrcToken043.sol";
    String contractName = "Result";
    HashMap retMap = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    final String recieveTokenTxid = PublicMethedForDailybuild
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 1000, assetAccountId.toStringUtf8(),
            100, null, dev001Key, dev001Address, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    // after deploy, check account resource
    accountResource = PublicMethedForDailybuild.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethedForDailybuild.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountAfter = PublicMethedForDailybuild
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));
    logger.info("after AssetId: " + assetAccountId.toStringUtf8()
        + ", devAssetCountAfter: " + devAssetCountAfter);

    Optional<TransactionInfo> infoById = PublicMethedForDailybuild
        .getTransactionInfoById(recieveTokenTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    if (recieveTokenTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy receive failed with message: " + infoById.get().getResMessage());
    }

    resultContractAddress = infoById.get().getContractAddress().toByteArray();

    SmartContract smartContract = PublicMethedForDailybuild
        .getContract(resultContractAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    Long contractAssetCount = PublicMethedForDailybuild.getAssetIssueValue(resultContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: "
        + contractAssetCount);

    Assert.assertEquals(Long.valueOf(100), Long.valueOf(devAssetCountBefore - devAssetCountAfter));
    Assert.assertEquals(Long.valueOf(100), contractAssetCount);
  }

  @Test(enabled = true, description = "TransferToken with invalid tokenId, transferToken")
  public void test03TriggerContract() {
    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceForReceiver(fromAddress,
        PublicMethedForDailybuild.getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethedForDailybuild.transferAsset(user001Address,
        assetAccountId.toByteArray(), 10L, dev001Address, dev001Key, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethedForDailybuild.transferAsset(user002Address,
        assetAccountId.toByteArray(), 10L, dev001Address, dev001Key, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage accountResource = PublicMethedForDailybuild.getAccountResource(dev001Address,
        blockingStubFull);
    long devEnergyLimitBefore = accountResource.getEnergyLimit();
    long devEnergyUsageBefore = accountResource.getEnergyUsed();
    long devBalanceBefore = PublicMethedForDailybuild.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("before trigger, devEnergyLimitBefore is " + Long.toString(devEnergyLimitBefore));
    logger.info("before trigger, devEnergyUsageBefore is " + Long.toString(devEnergyUsageBefore));
    logger.info("before trigger, devBalanceBefore is " + Long.toString(devBalanceBefore));

    accountResource = PublicMethedForDailybuild.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitBefore = accountResource.getEnergyLimit();
    long userEnergyUsageBefore = accountResource.getEnergyUsed();
    long userBalanceBefore = PublicMethedForDailybuild.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("before trigger, userEnergyLimitBefore is " + Long.toString(userEnergyLimitBefore));
    logger.info("before trigger, userEnergyUsageBefore is " + Long.toString(userEnergyUsageBefore));
    logger.info("before trigger, userBalanceBefore is " + Long.toString(userBalanceBefore));

    Long transferAssetBefore = PublicMethedForDailybuild
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId,
            blockingStubFull);
    logger.info("before trigger, transferTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", Count is " + transferAssetBefore);

    Long receiveAssetBefore = PublicMethedForDailybuild.getAssetIssueValue(resultContractAddress, assetAccountId,
        blockingStubFull);
    logger.info("before trigger, resultContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", Count is " + receiveAssetBefore);

    // tokenId is 100_0000
    String tokenId = Long.toString(100_0000);
    Long tokenValue = Long.valueOf(1);
    Long callValue = Long.valueOf(0);

    String param = "\"" + Base58.encode58Check(resultContractAddress)
        + "\",\"" + tokenValue + "\"," + tokenId;

    String triggerTxid = PublicMethedForDailybuild.triggerContract(transferTokenContractAddress,
        "transferTokenTest(address,uint256,trcToken)", param, false, callValue,
        1000000000L, assetAccountId.toStringUtf8(), 2, user001Address, user001Key,
        blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethedForDailybuild
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("Triger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceForReceiver(fromAddress,
        PublicMethedForDailybuild.getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceForReceiver(fromAddress,
        PublicMethedForDailybuild.getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    // tokenId is -1
    tokenId = Long.toString(-1);
    tokenValue = Long.valueOf(1);
    callValue = Long.valueOf(0);

    param = "\"" + Base58.encode58Check(resultContractAddress)
        + "\",\"" + tokenValue + "\"," + tokenId;

    triggerTxid = PublicMethedForDailybuild.triggerContract(transferTokenContractAddress,
        "transferTokenTest(address,uint256,trcToken)", param, false, callValue,
        1000000000L, assetAccountId.toStringUtf8(), 2, user001Address, user001Key,
        blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethedForDailybuild
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    //Assert.assertEquals("validateForSmartContract failure, not valid token id",
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceForReceiver(fromAddress,
        PublicMethedForDailybuild.getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    // tokenId is 1
    tokenId = Long.toString(Long.MIN_VALUE);
    tokenValue = Long.valueOf(1);
    callValue = Long.valueOf(0);

    param = "\"" + Base58.encode58Check(resultContractAddress)
        + "\",\"" + tokenValue + "\"," + tokenId;

    triggerTxid = PublicMethedForDailybuild.triggerContract(transferTokenContractAddress,
        "transferTokenTest(address,uint256,trcToken)", param, false, callValue,
        1000000000L, assetAccountId.toStringUtf8(), 2, user001Address, user001Key,
        blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethedForDailybuild
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceForReceiver(fromAddress,
        PublicMethedForDailybuild.getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    // tokenId is long.min
    tokenId = Long.toString(Long.MIN_VALUE);
    tokenValue = Long.valueOf(1);
    callValue = Long.valueOf(0);

    param = "\"" + Base58.encode58Check(resultContractAddress)
        + "\",\"" + tokenValue + "\"," + tokenId;

    triggerTxid = PublicMethedForDailybuild.triggerContract(transferTokenContractAddress,
        "transferTokenTest(address,uint256,trcToken)", param, false, callValue,
        1000000000L, assetAccountId.toStringUtf8(), 2, user001Address, user001Key,
        blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethedForDailybuild
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceForReceiver(fromAddress,
        PublicMethedForDailybuild.getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));

    // tokenId is 0, contract not have trx
    tokenId = Long.toString(0);
    tokenValue = Long.valueOf(1);
    callValue = Long.valueOf(0);

    param = "\"" + Base58.encode58Check(resultContractAddress)
        + "\",\"" + tokenValue + "\"," + tokenId;

    triggerTxid = PublicMethedForDailybuild.triggerContract(transferTokenContractAddress,
        "transferTokenTest(address,uint256,trcToken)", param, false, callValue,
        1000000000L, assetAccountId.toStringUtf8(), 2, user001Address, user001Key,
        blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethedForDailybuild
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceForReceiver(fromAddress,
        PublicMethedForDailybuild.getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));

    // tokenId is 0, account does not have trx
    param = "\"" + Base58.encode58Check(dev001Address)
        + "\",\"" + tokenValue + "\"," + tokenId;

    triggerTxid = PublicMethedForDailybuild.triggerContract(transferTokenContractAddress,
        "transferTokenTest(address,uint256,trcToken)", param, false, callValue,
        1000000000L, assetAccountId.toStringUtf8(), 2, user001Address, user001Key,
        blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethedForDailybuild
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceForReceiver(fromAddress,
        PublicMethedForDailybuild.getFreezeBalanceCount(user002Address, user002Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user002Address), testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethedForDailybuild
        .sendcoin(transferTokenContractAddress, 5000000, fromAddress, testKey002,
            blockingStubFull));
    Assert.assertTrue(PublicMethedForDailybuild
        .sendcoin(user002Address, 5000000, fromAddress, testKey002,
            blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    // tokenId is 0, transfer contract has trx, transfer to a contract
    tokenId = Long.toString(0);
    tokenValue = Long.valueOf(1);
    callValue = Long.valueOf(1);

    param = "\"" + Base58.encode58Check(resultContractAddress)
        + "\",\"" + tokenValue + "\"," + tokenId;

    triggerTxid = PublicMethedForDailybuild.triggerContract(transferTokenContractAddress,
        "transferTokenTest(address,uint256,trcToken)", param, false, callValue,
        1000000000L, assetAccountId.toStringUtf8(), 2, user002Address, user002Key,
        blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethedForDailybuild
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceForReceiver(fromAddress,
        PublicMethedForDailybuild.getFreezeBalanceCount(user002Address, user002Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user002Address), testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    //tokenId is 0, transfer contract has trx, transfer to normal account
    tokenId = Long.toString(0);
    tokenValue = Long.valueOf(1);
    callValue = Long.valueOf(1);

    param = "\"" + Base58.encode58Check(dev001Address)
        + "\",\"" + tokenValue + "\"," + tokenId;

    triggerTxid = PublicMethedForDailybuild.triggerContract(transferTokenContractAddress,
        "transferTokenTest(address,uint256,trcToken)", param, false, callValue,
        1000000000L, assetAccountId.toStringUtf8(), 2, user002Address, user002Key,
        blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethedForDailybuild
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceForReceiver(fromAddress,
        PublicMethedForDailybuild.getFreezeBalanceCount(user002Address, user002Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user002Address), testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    // tokenid bigger than long.max, trigger to a contract
    callValue = Long.valueOf(0);

    param = "\"" + Base58.encode58Check(resultContractAddress) + "\"";

    triggerTxid = PublicMethedForDailybuild.triggerContract(transferTokenContractAddress,
        "transferTokenTestIDOverBigInteger(address)", param, false, callValue,
        1000000000L, assetAccountId.toStringUtf8(), 2, user002Address, user002Key,
        blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethedForDailybuild
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceForReceiver(fromAddress,
        PublicMethedForDailybuild.getFreezeBalanceCount(user002Address, user002Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user002Address), testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    // tokenid bigger than long.max, trigger to a normal account
    param = "\"" + Base58.encode58Check(dev001Address) + "\"";

    triggerTxid = PublicMethedForDailybuild.triggerContract(transferTokenContractAddress,
        "transferTokenTestIDOverBigInteger(address)", param, false, callValue,
        1000000000L, assetAccountId.toStringUtf8(), 2, user002Address, user002Key,
        blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethedForDailybuild.getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

    Long transferAssetAfter = PublicMethedForDailybuild.getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("after trigger, transferTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", transferAssetAfter is " + transferAssetAfter);

    Long receiveAssetAfter = PublicMethedForDailybuild.getAssetIssueValue(resultContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("after trigger, resultContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", receiveAssetAfter is " + receiveAssetAfter);

    Assert.assertEquals(receiveAssetAfter, receiveAssetBefore);
    Assert.assertEquals(transferAssetBefore, transferAssetAfter);

    // unfreeze resource
    PublicMethedForDailybuild.unFreezeBalance(fromAddress, testKey002, 1,
        dev001Address, blockingStubFull);
    PublicMethedForDailybuild.unFreezeBalance(fromAddress, testKey002, 0,
        dev001Address, blockingStubFull);
    PublicMethedForDailybuild.unFreezeBalance(fromAddress, testKey002, 1,
        user001Address, blockingStubFull);
    PublicMethedForDailybuild.unFreezeBalance(fromAddress, testKey002, 1,
        user002Address, blockingStubFull);
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
}


