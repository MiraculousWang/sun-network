package stest.tron.wallet.dailybuild.tvmnewcommand.triggerconstant;

import static org.hamcrest.core.StringContains.containsString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;

@Slf4j
public class TriggerConstant001 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethedForDailybuild.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;


  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  byte[] contractAddressNoAbi = null;
  byte[] contractAddressWithAbi = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


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
    PublicMethedForDailybuild.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    {
      Assert.assertTrue(PublicMethedForDailybuild
          .sendcoin(contractExcAddress, 10000_000_000L, testNetAccountAddress, testNetAccountKey,
              blockingStubFull));
      PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
      String filePath = "src/test/resources/soliditycode/TriggerConstant001.sol";
      String contractName = "testConstantContract";
      HashMap retMap = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName);
      String code = retMap.get("byteCode").toString();
      final String abi = retMap.get("abI").toString();

      contractAddressNoAbi = PublicMethedForDailybuild.deployContract(contractName, "[]", code, "", maxFeeLimit,
          0L, 100, null, contractExcKey,
          contractExcAddress, blockingStubFull);
      PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
      SmartContract smartContract = PublicMethedForDailybuild.getContract(
          contractAddressNoAbi, blockingStubFull);
      Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
      Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
      Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());

      contractAddressWithAbi = PublicMethedForDailybuild.deployContract(contractName, abi, code, "", maxFeeLimit,
          0L, 100, null, contractExcKey,
          contractExcAddress, blockingStubFull);
      PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
      SmartContract smartContract2 = PublicMethedForDailybuild.getContract(
          contractAddressWithAbi, blockingStubFull);
      Assert.assertFalse(smartContract2.getAbi().toString().isEmpty());
      Assert.assertTrue(smartContract2.getName().equalsIgnoreCase(contractName));
      Assert.assertFalse(smartContract2.getBytecode().toString().isEmpty());

    }
  }

  @Test(enabled = true, description = "TriggerConstantContract a payable function without ABI")
  public void test01TriggerConstantContract() {

    String txid = "";

    TransactionExtention transactionExtention = PublicMethedForDailybuild
        .triggerConstantContractForExtention(contractAddressNoAbi,
            "testPayable()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out
        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert
        .assertThat(transactionExtention.getResult().getCode().toString(),
            containsString("CONTRACT_EXE_ERROR"));
    Assert
        .assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
            containsString("Attempt to call a state modifying opcode inside STATICCALL"));


  }

  @Test(enabled = true, description = "TriggerConstantContract a non-payable function without ABI")
  public void test02TriggerConstantContract() {

    TransactionExtention transactionExtention = PublicMethedForDailybuild
        .triggerConstantContractForExtention(contractAddressNoAbi,
            "testNoPayable()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out
        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert
        .assertThat(transactionExtention.getResult().getCode().toString(),
            containsString("CONTRACT_EXE_ERROR"));
    Assert
        .assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
            containsString("Attempt to call a state modifying opcode inside STATICCALL"));


  }

  @Test(enabled = true, description = "TriggerConstantContract a view function without ABI")
  public void test03TriggerConstantContract() {


    TransactionExtention transactionExtention = PublicMethedForDailybuild
        .triggerConstantContractForExtention(contractAddressNoAbi,
            "testView()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));
  }

  @Test(enabled = true, description = "TriggerConstantContract a pure function without ABI")
  public void test04TriggerConstantContract() {


    TransactionExtention transactionExtention = PublicMethedForDailybuild
        .triggerConstantContractForExtention(contractAddressNoAbi,
            "testPure()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));


  }

  @Test(enabled = true, description = "TriggerConstantContract a payable function with ABI")
  public void test05TriggerConstantContract() {

    TransactionExtention transactionExtention = PublicMethedForDailybuild
        .triggerConstantContractForExtention(contractAddressNoAbi,
            "testPayable()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out
        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("CONTRACT_EXE_ERROR"));
    Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
        containsString("Attempt to call a state modifying opcode inside STATICCALL"));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);


  }

  @Test(enabled = true, description = "TriggerConstantContract a non-payable function with ABI")
  public void test06TriggerConstantContract() {


    TransactionExtention transactionExtention = PublicMethedForDailybuild
        .triggerConstantContractForExtention(contractAddressWithAbi,
            "testNoPayable()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    System.out.println("Code = " + transactionExtention.getResult().getCode());
    System.out
        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());

    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("CONTRACT_EXE_ERROR"));
    Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
        containsString("Attempt to call a state modifying opcode inside STATICCALL"));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);


  }

  @Test(enabled = true, description = "TriggerConstantContract a view function with ABI")
  public void test07TriggerConstantContract() {

    TransactionExtention transactionExtention = PublicMethedForDailybuild
        .triggerConstantContractForExtention(contractAddressWithAbi,
            "testView()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));


  }

  @Test(enabled = true, description = "TriggerConstantContract a pure function with ABI")
  public void test08TriggerConstantContract() {


    TransactionExtention transactionExtention = PublicMethedForDailybuild
        .triggerConstantContractForExtention(contractAddressWithAbi,
            "testPure()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));


  }

  @Test(enabled = true, description = "TriggerContract a payable function without ABI")
  public void test09TriggerContract() {
    Account info;

    AccountResourceMessage resourceInfo = PublicMethedForDailybuild.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethedForDailybuild.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";

    txid = PublicMethedForDailybuild
        .triggerContract(contractAddressNoAbi,
            "testPayable()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethedForDailybuild.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethedForDailybuild.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);


  }

  @Test(enabled = true, description = "TriggerContract a non-payable function without ABI")
  public void test10TriggerContract() {
    Account info;

    AccountResourceMessage resourceInfo = PublicMethedForDailybuild.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethedForDailybuild.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";

    txid = PublicMethedForDailybuild
        .triggerContract(contractAddressNoAbi,
            "testNoPayable()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethedForDailybuild.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethedForDailybuild.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);


  }

  @Test(enabled = true, description = "TriggerContract a view function without ABI")
  public void test11TriggerContract() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethedForDailybuild.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethedForDailybuild.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";

    txid = PublicMethedForDailybuild
        .triggerContract(contractAddressNoAbi,
            "testView()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethedForDailybuild.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethedForDailybuild.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);


  }

  @Test(enabled = true, description = "TriggerContract a pure function without ABI")
  public void test12TriggerContract() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethedForDailybuild.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethedForDailybuild.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";

    txid = PublicMethedForDailybuild
        .triggerContract(contractAddressNoAbi,
            "testPure()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethedForDailybuild.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethedForDailybuild.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);


  }

  @Test(enabled = true, description = "TriggerContract a pure function with ABI")
  public void test18TriggerContract() {

    TransactionExtention transactionExtention = PublicMethedForDailybuild
        .triggerContractForExtention(contractAddressWithAbi,
            "testPure()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));

  }

  @Test(enabled = true, description = "TriggerContract a payable function with ABI")
  public void test19TriggerContract() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethedForDailybuild.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethedForDailybuild.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";
    txid = PublicMethedForDailybuild
        .triggerContract(contractAddressWithAbi,
            "testPayable()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethedForDailybuild.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethedForDailybuild.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);

  }

  @Test(enabled = true, description = "TriggerContract a non-payable function with ABI")
  public void test20TriggerContract() {
    Account info;

    AccountResourceMessage resourceInfo = PublicMethedForDailybuild.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethedForDailybuild.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";
    txid = PublicMethedForDailybuild
        .triggerContract(contractAddressNoAbi,
            "testNoPayable()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();

    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Account infoafter = PublicMethedForDailybuild.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethedForDailybuild.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnnumber);

  }

  @Test(enabled = true, description = "TriggerContract a view function with ABI")
  public void test21TriggerContract() {

    TransactionExtention transactionExtention = PublicMethedForDailybuild
        .triggerContractForExtention(contractAddressWithAbi,
            "testView()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));

  }

  @Test(enabled = true, description = "TriggerConstantContract a view method with ABI ,method has "
      + "revert()")
  public void test24TriggerConstantContract() {


    TransactionExtention transactionExtention = PublicMethedForDailybuild
        .triggerConstantContractForExtention(contractAddressWithAbi,
            "testView2()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert
        .assertThat(transaction.getRet(0).getRet().toString(),
            containsString("FAILED"));
    Assert
        .assertThat(ByteArray
                .toStr(transactionExtention.getResult().getMessage().toByteArray()),
            containsString("REVERT opcode executed"));


  }

  @Test(enabled = true, description = "TriggerContract a view method with ABI ,method has "
      + "revert()")
  public void test25TriggerContract() {

    TransactionExtention transactionExtention = PublicMethedForDailybuild
        .triggerContractForExtention(contractAddressWithAbi,
            "testView2()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));

    Assert.assertThat(transaction.getRet(0).getRet().toString(),
        containsString("FAILED"));
    Assert.assertThat(ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("REVERT opcode executed"));


  }

  @Test(enabled = true, description = "TriggerConstantContract a view method without ABI,method has"
      + "revert()")
  public void testTriggerConstantContract() {


    TransactionExtention transactionExtention = PublicMethedForDailybuild
        .triggerConstantContractForExtention(contractAddressNoAbi,
            "testView2()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert
        .assertThat(transaction.getRet(0).getRet().toString(),
            containsString("FAILED"));
    Assert
        .assertThat(ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
            containsString("REVERT opcode executed"));


  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
