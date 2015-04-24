package org.provebit.hashcache.merkle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.provebit.hashcache.merkle.HashType;
import org.provebit.hashcache.merkle.Merkle;
import org.provebit.hashcache.merkle.MerkleStepSerializer;
import org.provebit.hashcache.AppWallet;

/**
 * TransactionMerkleSerializer This class serializes the path up a transaction merkle tree
 * 
 * @author noahmalmed
 *
 */
public class TransactionMerkleSerializer {
	
	private Peer peer;
	
	/**
	 * All this class needs to exist is a connection to a peer
	 */
	public TransactionMerkleSerializer(){
		peer = AppWallet.INSTANCE.getPeer();
	}
	
	/**
	 * This function returns the serialized path up the block merkle tree
	 * 
	 * Note: Returns null if:
	 * 1) The transaction is not found in the block
	 * 2) Peer times out when looking for the block
	 * 3) The merkle root calculated does not match the merkle root in the block
	 * 
	 * Note: The path follows the following endianess:
	 * 
	 * @param transactionID - Big endian transaction hash
	 * @param blockHeader - Big endian transaction hash
	 * @return SerializedPath - All intermediate hash values are based off a little endian transactionID
	 */
	public byte[] SerializedPathUpMerkle(byte[] transactionID, byte[] blockHeader){
		try {
			// This call blocks until the block is received
			Block targetBlock = peer.getBlock(new Sha256Hash(blockHeader)).get(10,TimeUnit.SECONDS);
			List<Transaction> transactions = targetBlock.getTransactions(); 
			List<byte[]> transHashes = new ArrayList<byte[]>();
			
			boolean foundHash = false;
			// Add the hashes to a list
			for(Transaction trans: transactions){
				
				// This hash is in big endian
				Sha256Hash hash = trans.getHash();
				
				// We want to save the bytes as little endian, so we flip the bytes
				transHashes.add(Utils.reverseBytes(hash.getBytes()));
				
				// Check if the transaction is in the block
				if(hash.equals(new Sha256Hash(transactionID))){
					foundHash = true;
				}
			}
			
			// Condition when the hash isn't found
			if(!foundHash){
				return null;
			}
			
			// Add test to check merkle root
			
			// Create a Merkle tree from the hashes
			Merkle blockMerkle = new Merkle(HashType.DOUBLE_SHA256);
			blockMerkle.makeTree(transHashes);
			
			return MerkleStepSerializer.serialize(blockMerkle.findPath(Utils.reverseBytes(transactionID)));
			
		} catch (InterruptedException | ExecutionException e) {
			// If the block is not found then return null
			e.printStackTrace();
			return null;
		} catch (TimeoutException t){
			System.out.println("PEER TIMED OUT!!");
			return null;
		}
	}
	
	

}
