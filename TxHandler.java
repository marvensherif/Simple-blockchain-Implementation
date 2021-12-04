import java.util.ArrayList;


public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. 
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    
    public boolean isValidTx(Transaction tx) {
    	ArrayList<Transaction.Input> inputs  = tx.getInputs(); 
    	double inValues = 0;
    	double outValues=0;
    	UTXOPool newPool = new UTXOPool();
    	for(int i = 0 ; i < inputs.size();i++ ) {
    		UTXO utxo = new UTXO(inputs.get(i).prevTxHash, inputs.get(i).outputIndex);
    		// check that the transaction is in the original pool 
    		if(!utxoPool.contains(utxo)) { 
    			return false;
    		}
    		Transaction.Output output = utxoPool.getTxOutput(utxo);
    		//verify the signature of the transaction
    		if(!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), inputs.get(i).signature)) {
    			return false;
    		}
    		
    		//check double spending
    		if(newPool.contains(utxo)) { 
    			return false;
    		}
    		newPool.addUTXO(utxo, output);
    		
    		inValues += output.value;
    	}
    	
    	ArrayList<Transaction.Output> outputs  = tx.getOutputs(); 
    	for (int i = 0 ; i < outputs.size() ; i++) {
    		//all of {@code tx}s output values are non-negative
    		if( outputs.get(i).value < 0) {
    			return false;
    		}
    		 outValues += outputs.get(i).value;
    	}
    	if(outValues > inValues) {
    		return false;
    	}
    	
    	
    	return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
 
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
    	 ArrayList<Transaction> validTransactions = new ArrayList<Transaction>();
    	//check that every transaction is valid
        for(int i =0 ; i < possibleTxs.length ; i++) {
        	if(isValidTx(possibleTxs[i])) {
        		validTransactions.add(possibleTxs[i]);
        		//remove valid transactions from utxo pool
        		for (int j = 0 ; j < possibleTxs[i].getInputs().size() ; j++) {
        			UTXO utxo = new UTXO(possibleTxs[i].getInputs().get(j).prevTxHash, possibleTxs[i].getInputs().get(j).outputIndex);
        			utxoPool.removeUTXO(utxo);
        		}
        		//after transaction completed add the new unspended transactions to the pool
        		for(int j = 0 ; j < possibleTxs[i].getOutputs().size(); j++) {
        			UTXO utxo = new UTXO(possibleTxs[i].getHash(),j);
        			utxoPool.addUTXO(utxo, possibleTxs[i].getOutput(j));
        		}
        		
        		
        	}
        }
   
        return validTransactions.toArray(new Transaction[0]);
    }
    public UTXOPool getUTXOPool() {
        return utxoPool;
    }

}
