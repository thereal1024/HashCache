package org.provebit.hashcache;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.sql.*;

import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletEventListener;
import org.bitcoinj.script.Script;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.provebit.hashcache.merkle.TransactionMerkleSerializer;

public class ProverTask {

	final Runnable taskRunner;
	private ScheduledFuture<?> periodicCheck;
	private WalletStateTracker wst;
	/** Seconds between window opening */
	final static private int SECONDS_INT = 600; // 10 minutes

	public ProverTask() {
		ScheduledExecutorService scheduler = Executors
				.newScheduledThreadPool(1);
		taskRunner = new Runnable() {
			@Override
			public void run() {
				windowopen();
			}
		};

		periodicCheck = scheduler.scheduleWithFixedDelay(taskRunner, 0,
				SECONDS_INT, TimeUnit.SECONDS);
		wst = new WalletStateTracker();
		AppWallet.INSTANCE.addEventListener(wst);
	}

	public void end() {
		periodicCheck.cancel(true);
		if (wst != null) {
			AppWallet.INSTANCE.removeEventListener(wst);
		}
	}

	/**
	 * Open a new submission window
	 */
	private void windowopen() {
		Connection conn = null;
		try {
			// open the window
			conn = DBConnector.getConnection();
			CallableStatement cs = conn.prepareCall("{call windowopen(?)}");
			cs.registerOutParameter(1, Types.INTEGER);
			cs.execute();
			
			int window = cs.getInt(1);
			System.out.printf("opened window number: %d\n", window);
			
			int lastwin = window - 1;
			if (lastwin < 1) {
				return;
			}
			
			// launch a tx based on the last merkle root
			String rootquery = "SELECT NodeHash.hash FROM MerkleRoot, NodeHash "
                + "WHERE NodeHash.nodeID = MerkleRoot.nodeID "
                + "AND NodeHash.windowID = ?";
			PreparedStatement stmt = conn.prepareStatement(rootquery);
			stmt.setInt(1, lastwin);
			
			ResultSet rs = stmt.executeQuery();
			byte[] hashToProve = null;
			if (rs.next()) {
				hashToProve = rs.getBytes(1);
			}
			rs.close();
			if (hashToProve == null) {
				System.out.println("empty window, nothing to prove");
				return;
			}
			
			System.out.println("Going to prove previous root: "
					+ Hex.encodeHexString(hashToProve));
			
			// create Bitcoin transactions
			Transaction prooftx = AppWallet.INSTANCE.proofTX(hashToProve);
			
			// record TX in the DB
			byte[] txbin = prooftx.bitcoinSerialize();
			byte[] txhash = prooftx.getHash().getBytes();
			
			System.out.printf("Launched proof TX: %s \n", Hex.encodeHexString(txhash));
			
			String addtxquery = "INSERT into Transaction Values(?,?,?,?,?,?,?,?)";
			PreparedStatement stmt2 = conn.prepareStatement(addtxquery);
			stmt2.setBytes(1, txhash); // do we need to reverse?
			stmt2.setBoolean(2, false); // TX has not failed yet
			stmt2.setInt(3, 0); // 0 confirmations
			stmt2.setNull(4, Types.BINARY); // no block yet
			stmt2.setLong(5, prooftx.getFee().getValue()); // amount of satoshis paid
			stmt2.setNull(6, Types.VARBINARY); // delay tx path
			stmt2.setNull(7, Types.VARBINARY); // delay tx data
			java.sql.Date sqltxdate = new java.sql.Date(prooftx.getUpdateTime().getTime());
			stmt2.setDate(8, sqltxdate); // set TX date
			
			stmt2.executeUpdate();
			
			String linkProof = "INSERT into Proof Values (?,?)";
			PreparedStatement stmt3 = conn.prepareStatement(linkProof);
			stmt3.setBytes(1, txhash);
			stmt3.setInt(2, lastwin);
			
			stmt3.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InsufficientMoneyException e) {
			System.out.println("failure to have enough money!");
			System.out.println("the window will not be proved.");
		} finally {
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}
		}
	}

	/**
	 * Checks if a transaction (whose confirmation confidence should have
	 * changed) is proof relevant.
	 * If so then, it makes sure if its in a block, then it will be updated
	 * in the database.
	 * @param wallet - wallet containing the transaction
	 * @param tx - transaction to inspect
	 */
	private void maybeUpdateTx(Transaction tx) {
		AppWallet w = AppWallet.INSTANCE;
		if (!w.isProofTX(tx)) {
			// maybe its a regular wallet transaction
			System.out.println("Skipping update over: " + tx.getHashAsString());
			return;
		}
		
		//byte[] merkleroot = w.proofTXextractHash(tx); we don't need this for now
		byte[] txid = tx.getHash().getBytes();
		byte[] txdata = tx.bitcoinSerialize();
		TransactionConfidence confidence = tx.getConfidence();
		ConfidenceType ct = confidence.getConfidenceType();
		int confirmations = confidence.getDepthInBlocks();
		
		if (ct == ConfidenceType.DEAD) {
			System.out.printf("TX %s is dead.\n", Hex.encodeHexString(txid));
			return;
		}
		
		if (confirmations < 1) {
			System.out.printf("TX %s is updated but not ready.\n", Hex.encodeHexString(txid));
			return;
		}
		
		// we should have filtered a large portion of events by here
		// however we still need a good way to ignore long confirmed tx
		
		// TODO add check based on uptime to filter old TX that should have been caught
		
		Connection conn = null;
		try {
			// check on the old conf number | if we have the proof registered
			conn = DBConnector.getConnection();
			String query = "SELECT confirmations, blockpath FROM Transaction "
					+ "WHERE transactionID = ?";
			PreparedStatement stmt = conn.prepareStatement(query);
			stmt.setBytes(1, txid);
			ResultSet rs = stmt.executeQuery();
			
			boolean ok = false;
			int oldConfirmations = -1;
			byte[] oldBlockPath = null;
			if (rs.next()) {
				ok = true;
				oldConfirmations = rs.getInt(1);
				oldBlockPath = rs.getBytes(2);
			}
			rs.close();
			if (!ok) {
				System.out.printf("TX %s could not be looked up.\n", Hex.encodeHexString(txid));
				return;
			}
			
			if (oldConfirmations >= 1 && oldBlockPath != null) {
				// already registered
				return;
			}
			
			BlockStore store = AppWallet.INSTANCE.getChain().getBlockStore();
			Map<Sha256Hash, Integer> blocksin = tx.getAppearsInHashes();
			
			if (blocksin.size() == 0) {
				System.out.printf("warn: TX %s is in no blocks, but is should be.\n", Hex.encodeHexString(txid));
				return;
			}
			
			Sha256Hash optimalHash = null;
			for (Entry<Sha256Hash, Integer> e : blocksin.entrySet()) {
				if (store.get(e.getKey()) != null) {
					optimalHash = e.getKey();
					break;
				}
			}
			if (optimalHash == null) {
				System.out.println("warn: couldn't find the block in the store");
				optimalHash = blocksin.keySet().iterator().next();
			}
			
			
			TransactionMerkleSerializer tms = new TransactionMerkleSerializer();
			byte[] path = tms.SerializedPathUpMerkle(txid, optimalHash.getBytes());
			
			if (path == null) {
				System.out.printf("error: requested path failure %s %s\n", 
						Hex.encodeHexString(txid), optimalHash.toString());
				return;
			}
			
			System.out.printf("Update recorded for TX %s.\n", Hex.encodeHexString(txid));
			
			String query2 = "UPDATE Transaction SET confirmations = ?, blockpath = ?, rawdata = ?, "
					+ "includedBlock = ? WHERE transactionID = ?";
			PreparedStatement stmt2 = conn.prepareStatement(query2);
			stmt2.setInt(1, confirmations);
			stmt2.setBytes(2, path);
			stmt2.setBytes(3, txdata);
			stmt2.setBytes(4, optimalHash.getBytes());
			stmt2.setBytes(5, txid);
			
			stmt2.executeUpdate();
			// now the DB has the Window Proved !
			System.out.printf("TX %s now proved.\n", Hex.encodeHexString(txid));
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (BlockStoreException e1) {
			e1.printStackTrace();
		} finally {
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Primary listener for Bitcoin network events
	 */
	private class WalletStateTracker implements WalletEventListener {

		Coin walletbalance = AppWallet.INSTANCE.getBalance();
		
		@Override
		public void onKeysAdded(List<ECKey> keys) {
			// do nothing
		}

		@Override
		public void onCoinsReceived(Wallet wallet, Transaction tx,
				Coin prevBalance, Coin newBalance) {
			// do nothing
		}

		@Override
		public void onCoinsSent(Wallet wallet, Transaction tx,
				Coin prevBalance, Coin newBalance) {
			// do nothing
		}

		@Override
		public void onReorganize(Wallet wallet) {
			// do nothing
		}

		@Override
		public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
			maybeUpdateTx(tx);
		}

		@Override
		public void onWalletChanged(Wallet wallet) {
			Coin newbalance = wallet.getBalance();
			if (!newbalance.equals(walletbalance)) {
				System.out.println("------------------------------");
				System.out.println("New wallet balance: " + newbalance.toFriendlyString());
				System.out.println("------------------------------");
				walletbalance = newbalance;
			}
		}

		@Override
		public void onScriptsAdded(Wallet wallet, List<Script> scripts) {
			// do nothing
		}
		
	}
}
