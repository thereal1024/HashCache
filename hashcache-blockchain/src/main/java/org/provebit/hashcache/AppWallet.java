package org.provebit.hashcache;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.Wallet.SendResult;
import org.bitcoinj.core.WalletEventListener;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.bitcoinj.utils.Threading;

import com.google.common.primitives.Bytes;

public enum AppWallet {
	INSTANCE;
	
	private static final String WALLET_NAME = "bitcoin";
	private static final byte[] PROOF_MAGIC;
	static {
			byte[] temp = null;
			try {
				temp = "#cache  ".getBytes("US-ASCII");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			PROOF_MAGIC = temp;
	}
	private static final String PROOF_MEMO = "!!ProofTransaction!!";
	// make the wallet work on main Bitcoin network
	private NetworkParameters params;
	private PeerGroup peerGroup;
	private org.bitcoinj.core.Wallet wallet;
	private WalletAppKit walletGen;
	private BlockChain chain;
	
	private AppWallet() {
		initWallet(WALLET_NAME, ApplicationDirectory.INSTANCE.getRoot());
	}

	private void initWallet(String walletName, File directory) {
		assert(walletName != null);
		if (walletName.toLowerCase().startsWith("testnet")) {
			params = TestNet3Params.get();
			System.out.println(walletName + " is a testnet wallet");
		}
		else {
			params = MainNetParams.get();
		}
		walletGen = new WalletAppKit(params, directory,  walletName);
		
		// configure wallet service
		walletGen.setBlockingStartup(false);

		// and launch
		walletGen.startAsync();
		walletGen.awaitRunning();
		
		// post configuration
		peerGroup = walletGen.peerGroup();
		chain = walletGen.chain();
		peerGroup.setMaxConnections(12);
		wallet = walletGen.wallet();
		wallet.allowSpendingUnconfirmedTransactions();
	}
	
	public void addEventListener(WalletEventListener listener) {
		wallet.addEventListener(listener);
	}
	
	public void removeEventListener(WalletEventListener listener) {
		wallet.removeEventListener(listener);
	}
	
	public Coin getBalance() {
		Coin balance = wallet.getBalance();
		return balance;
	}
	
	public Address getReceivingAddress() {
		return wallet.currentReceiveAddress();
	}
	
	public Wallet getWallet() {
		return wallet;
	}
	
	public Peer getPeer(){
		return peerGroup.getDownloadPeer();
	}
	
	public BlockChain getChain() {
		return chain;
	}
	
	/**
	 * Sends bitcoin
	 * @param btc - amount of bitcoin to send
	 * @param destAddress - what address to send to
	 * @throws AddressFormatException
	 * @throws InsufficientMoneyException
	 */
	public void simpleSendCoins(Coin btc, String destAddress) 
			throws AddressFormatException, InsufficientMoneyException {
		Address destination = new Address(params, destAddress);
		
		// TODO do something about failed sends
		SendResult res = wallet.sendCoins(peerGroup, destination, btc);
		res.broadcastComplete.addListener(new Runnable() {
			@Override
			public void run() {
				// TODO inform UI that transaction is sent
			}
			
		}, Threading.USER_THREAD);
	}
	
	public boolean isProofTX(Transaction tx) {
		//String memo = tx.getMemo();
		//if (memo == null) {
		//	return false;
		//}
		boolean found = false;
		for (TransactionOutput out : tx.getOutputs()) {
			byte[] check = out.getScriptBytes();
			int pos = Bytes.indexOf(check, PROOF_MAGIC);
			if (pos >= 0) {
				found = true;
				break;
			}
		}
		//if (!found)
		//	System.out.println("huh");
		//else
		//	System.out.println("good");
		return found;
	}
	
	public byte[] proofTXextractHash(Transaction tx) {
		if (!isProofTX(tx)) {
			return null;
		}
			
		String hexhash = tx.getMemo().split("@")[1];
		try {
			return Hex.decodeHex(hexhash.toCharArray());
		} catch (DecoderException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Transaction proofTX(byte[] hash) throws InsufficientMoneyException {
		if (hash.length != 32) {
			throw new RuntimeException("not a hash");
		}
		
		byte[] embedData = new byte[40];
		byte[] id = PROOF_MAGIC;
		assert(id.length == 8);
		System.arraycopy(id, 0, embedData, 0, id.length);
		System.arraycopy(hash, 0, embedData, id.length, hash.length);
		
		Transaction dataTx = new Transaction(params);
		dataTx.addOutput(Coin.ZERO, new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(embedData).build());
		dataTx.setMemo(PROOF_MEMO + "@" + Hex.encodeHexString(hash));
		Wallet.SendRequest srq = Wallet.SendRequest.forTx(dataTx);
		//wallet.completeTx(srq); // remove later
		Wallet.SendResult res = wallet.sendCoins(srq);
		return res.tx;
	}
	
}
