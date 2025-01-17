package stest.tron.wallet.committee;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.PaginatedMessage;
import org.tron.api.GrpcAPI.SideChainProposalList;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;


@Slf4j
public class WalletTestCommittee001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethedForDailybuild.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethedForDailybuild.getFinalAddress(testKey003);
  //Witness 47.93.9.236
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  //Witness 47.93.33.201
  private final String witnessKey002 = Configuration.getByPath("testng.conf")
      .getString("witness.key2");
  //Witness 123.56.10.6
  private final String witnessKey003 = Configuration.getByPath("testng.conf")
      .getString("witness.key3");
  //Wtiness 39.107.80.135
  private final String witnessKey004 = Configuration.getByPath("testng.conf")
      .getString("witness.key4");
  //Witness 47.93.184.2
  private final String witnessKey005 = Configuration.getByPath("testng.conf")
      .getString("witness.key5");


  private final byte[] witness001Address = PublicMethedForDailybuild.getFinalAddress(witnessKey001);
  private final byte[] witness002Address = PublicMethedForDailybuild.getFinalAddress(witnessKey002);
  private final byte[] witness003Address = PublicMethedForDailybuild.getFinalAddress(witnessKey003);
  private final byte[] witness004Address = PublicMethedForDailybuild.getFinalAddress(witnessKey004);
  private final byte[] witness005Address = PublicMethedForDailybuild.getFinalAddress(witnessKey005);


  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private static final long now = System.currentTimeMillis();

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

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
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }


  @Test
  public void testListProposals() {
    //List proposals
    SideChainProposalList proposalList = blockingStubFull.listSideChainProposals(EmptyMessage.newBuilder().build());
    Optional<SideChainProposalList> listProposals = Optional.ofNullable(proposalList);
    final Integer beforeProposalCount = listProposals.get().getProposalsCount();

    //CreateProposal
    final long now = System.currentTimeMillis();
    HashMap<Long, String> proposalMap = new HashMap<Long, String>();
    proposalMap.put(0L, "1000006");
    PublicMethed
        .sideChainProposalCreate(witness001Address, witnessKey001, proposalMap, blockingStubFull);

    //List proposals
    proposalList = blockingStubFull.listSideChainProposals(EmptyMessage.newBuilder().build());
    listProposals = Optional.ofNullable(proposalList);
    Integer afterProposalCount = listProposals.get().getProposalsCount();
    Assert.assertTrue(beforeProposalCount + 1 == afterProposalCount);
    logger.info(Long.toString(listProposals.get().getProposals(0).getCreateTime()));
    logger.info(Long.toString(now));
    //Assert.assertTrue(listProposals.get().getProposals(0).getCreateTime() >= now);
    Assert.assertTrue(listProposals.get().getProposals(0).getParametersMap().equals(proposalMap));

    //getProposalListPaginated
    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(0);
    pageMessageBuilder.setLimit(1);
   SideChainProposalList paginatedProposalList = blockingStubFull
        .getPaginatedSideChainProposalList(pageMessageBuilder.build());
    Assert.assertTrue(paginatedProposalList.getProposalsCount() >= 1);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


