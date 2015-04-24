package org.provebit.hashcache;

/**
 * Hashcache TX creator entry point
 *
 */
public class Main 
{
    public static void main( String[] args )
    {
    	AppWallet wallet = AppWallet.INSTANCE;
    	System.out.println("----------------------------------------------");
        System.out.println("You have " + 
        		wallet.getBalance().toFriendlyString() +
        		" in the wallet.");
        System.out.println("Deposit coins into " + 
        		wallet.getReceivingAddress());
        System.out.println("Ctrl-C to exit");
        System.out.println("----------------------------------------------");
        ProverTask prover = new ProverTask();
        //System.exit(0);
    }
}
