package org.provebit.hashcache;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.Wallet.SendResult;
import org.bitcoinj.core.WalletEventListener;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.bitcoinj.utils.Threading;

public enum AppWallet {
	INSTANCE;
	
	private static final String WALLET_NAME = "bitcoin";

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
	
	public Transaction proofTX(byte[] hash) throws InsufficientMoneyException {
		if (hash.length != 32) {
			throw new RuntimeException("not a hash");
		}
		
		byte[] embedData = new byte[40];
		byte[] id = null;
		try {
			id = "ProveBit".getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			// should not happen
			e.printStackTrace();
		}
		assert(id.length == 8);
		System.arraycopy(id, 0, embedData, 0, id.length);
		System.arraycopy(hash, 0, embedData, id.length, hash.length);
		
		Transaction dataTx = new Transaction(params);
		dataTx.addOutput(Coin.ZERO, new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(embedData).build());
		Wallet.SendRequest srq = Wallet.SendRequest.forTx(dataTx);
		//wallet.completeTx(srq); // remove later
		Wallet.SendResult res = wallet.sendCoins(srq);
		return res.tx;
	}
	
}
