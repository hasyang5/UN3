package org.un.core.db;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.un.common.crypto.ECKey;
import org.un.common.utils.ByteArray;
import org.un.common.utils.Sha256Hash;
import org.un.common.utils.Utils;
import org.un.consensus.dpos.DposSlot;
import org.un.core.ChainBaseManager;
import org.un.core.capsule.AccountCapsule;
import org.un.core.capsule.BlockCapsule;
import org.un.core.capsule.WitnessCapsule;
import org.un.protos.Protocol.Account;

public class ManagerForTest {

  private Manager dbManager;
  private ChainBaseManager chainBaseManager;
  private DposSlot dposSlot;

  public ManagerForTest(Manager dbManager, DposSlot dposSlot) {
    this.dbManager = dbManager;
    this.chainBaseManager = dbManager.getChainBaseManager();
    this.dposSlot = dposSlot;
  }

  private Map<ByteString, String> addTestWitnessAndAccount() {
    chainBaseManager.getWitnesses().clear();
    return IntStream.range(0, 2)
        .mapToObj(
            i -> {
              ECKey ecKey = new ECKey(Utils.getRandom());
              String privateKey = ByteArray.toHexString(ecKey.getPrivKey().toByteArray());
              ByteString address = ByteString.copyFrom(ecKey.getAddress());

              WitnessCapsule witnessCapsule = new WitnessCapsule(address);
              chainBaseManager.getWitnessStore().put(address.toByteArray(), witnessCapsule);
              chainBaseManager.addWitness(address);

              AccountCapsule accountCapsule =
                  new AccountCapsule(Account.newBuilder().setAddress(address).build());
              chainBaseManager.getAccountStore().put(address.toByteArray(), accountCapsule);

              return Maps.immutableEntry(address, privateKey);
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private ByteString getWitnessAddress(long time) {
    return dposSlot.getScheduledWitness(dposSlot.getSlot(time));
  }

  public BlockCapsule createTestBlockCapsule(long time,
      long number, ByteString hash) {

    Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();
    ByteString witnessAddress = getWitnessAddress(time);

    BlockCapsule blockCapsule = new BlockCapsule(number, Sha256Hash.wrap(hash), time,
        witnessAddress);
    blockCapsule.generatedByMyself = true;
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(ByteArray.fromHexString(addressToProvateKeys.get(witnessAddress)));
    return blockCapsule;
  }

  public boolean pushNTestBlock(int count) {
    try {
      for (int i = 1; i <= count; i++) {
        ByteString hash = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
            .getByteString();
        long time =
            chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 3000L;
        long number =
            chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1;
        BlockCapsule blockCapsule = createTestBlockCapsule(time, number, hash);
        dbManager.pushBlock(blockCapsule);
      }
    } catch (Exception ignore) {
      return false;
    }
    return true;
  }
}