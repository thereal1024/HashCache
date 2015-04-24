package org.provebit.hashcache;

/**
 * Hello world!
 *
 */
public class Main 
{
    public static void main( String[] args )
    {
    	AppWallet.INSTANCE.toString(); // load class
    	System.out.println("----------------------------------------------");
        System.out.println("You have " + 
        		AppWallet.INSTANCE.getBalance().toFriendlyString() +
        		" in the wallet.");
        System.out.println("Deposit coins into " + 
        		AppWallet.INSTANCE.getReceivingAddress());
        System.out.println("Ctrl-C to exit");
        System.out.println("----------------------------------------------");
        //System.exit(0);
    }
}
