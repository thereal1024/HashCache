package org.provebit.hashcache.merkle;

import java.util.List;

/**
 * This class is used to convert a list MerklePathSteps and serializes them into a byte array
 * @author Shishir Kanodia
 * @organization ProveBit
 * @version 0.1
 */

public class MerkleStepSerializer {
    
    public static byte[] serialize(List<MerklePathStep> path){
        int list_size = path.size()*33;
        byte[] output = new byte[list_size];        
        
        for (int i = 0; i < path.size(); i++){
            output[(33*i)] = (byte) (path.get(i).onLeft()?1:0);
            for(int j = 1; j < 33; j++){
                output[(33*i) + j] = path.get(i).getHash()[j-1];
            }
        }
        return output;
    }

}