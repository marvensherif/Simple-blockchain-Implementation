// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.
import java.util.ArrayList;
import java.util.HashMap;


public class BlockChain {
	
    public static final int CUT_OFF_AGE = 10;
    private TransactionPool transactionPool;
    private HashMap<ByteArrayWrapper, BlockNode> blockChain;
    private BlockNode maxHeightBlock;
    int flag=1;

    //Block class
    public class BlockNode {
    	public Block block;
    	public BlockNode parent;
    	public int height; 
    	public UTXOPool utxoPool;

       
        public BlockNode(Block block, BlockNode parent, UTXOPool utxoPool) {
            this.block = block;
            this.parent = parent;
            this.utxoPool = utxoPool;

            //if the the block is not genesis block
            if (parent != null) {
            	height = parent.height + 1;
            }else {
            	height=1; //genesisBlock
            }
            
        }
        //getting the UTXOPool of this block
        public UTXOPool getUtxoPool() {
            return new UTXOPool(utxoPool);
        }
    }
    /**
     * create an empty blockchain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
    	blockChain = new HashMap<>();
    	
    	//handle coinbase transactions of this block
        UTXOPool utxoPool = new UTXOPool();      
        ArrayList<Transaction.Output> txOutputs = genesisBlock.getCoinbase().getOutputs();
  	    // iterate over output transactions
  	    for (int i = 0; i < txOutputs.size(); i++) {
	    	    utxoPool.addUTXO(new UTXO(genesisBlock.getCoinbase().getHash(), i), txOutputs.get(i));
	    }
	    
        //create genesisBlock
        BlockNode genesisNode = new BlockNode(genesisBlock, null, utxoPool);

        // add genesisBlock to blockChain
        blockChain.put(new ByteArrayWrapper(genesisBlock.getHash()), genesisNode);
        transactionPool = new TransactionPool();
        maxHeightBlock = genesisNode;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return maxHeightBlock.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return maxHeightBlock.getUtxoPool();
    }


    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return transactionPool;
    }
    /**
     * Add {@code block} to the blockchain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}, where maxHeight is 
     * the current height of the blockchain.
	 * <p>
	 * Assume the Genesis block is at height 1.
     * For example, you can try creating a new block over the genesis block (i.e. create a block at 
	 * height 2) if the current blockchain height is less than or equal to CUT_OFF_AGE + 1. As soon as
	 * the current blockchain height exceeds CUT_OFF_AGE + 1, you cannot create a new block at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        
    	byte[] previousBlockHash = block.getPrevBlockHash();
        BlockNode previousBlock = blockChain.get(new ByteArrayWrapper(previousBlockHash));
        
        // check if the block is a genesisBlock or if previous block is not valid reject 
        if (previousBlockHash == null || previousBlock == null) {
            return false;
        }

        TxHandler handler = new TxHandler(previousBlock.getUtxoPool());
        
        //get the transactions of that block
        Transaction[] blockTransactions = block.getTransactions().toArray(new Transaction[block.getTransactions().size()]);

        // get valid transactions from block transactions
        Transaction[] validTransactions = handler.handleTxs(blockTransactions);

        // check that blockTransactions are the same as validTransactions
        if (blockTransactions.length != validTransactions .length) {
            return false;
        }
        

        //handle coinbase transactions of this block
        UTXOPool utxoPool = handler.getUTXOPool();
        ArrayList<Transaction.Output> txOutputs = block.getCoinbase().getOutputs();
  	    // iterate over output transactions
  	    for (int i = 0; i < txOutputs.size(); i++) {
	    	    utxoPool.addUTXO(new UTXO(block.getCoinbase().getHash(), i), txOutputs.get(i));
	    }
  	    
  	    
    	 //remove block that exceed CUT_OFF_AGE from memory to prevent overflow
        
      	if(previousBlock.height + 1 >maxHeightBlock.height && maxHeightBlock.height>CUT_OFF_AGE) {
   
          ArrayList<byte[]> deletedBlocks = new ArrayList<>();
        	for (BlockNode value : blockChain.values()) {
        		byte[] current = value.block.getHash();
       	    if(value.height==flag) {
       	    	deletedBlocks.add(current);
       	    
       	    }
        	}
        	
       	for(int j=0;j<deletedBlocks.size();j++) {
       	    blockChain.remove(new ByteArrayWrapper(deletedBlocks.get(j)));

       	    }
       	

       	 flag++;
      }
  	    
         //Create newBlock
         BlockNode newBlock = new BlockNode(block, previousBlock, utxoPool);
         //Add newBlock to blockChain
         blockChain.put(new ByteArrayWrapper(block.getHash()), newBlock);

        


        // update maxHeightBlock
        if (previousBlock.height + 1> maxHeightBlock.height) { 
                 maxHeightBlock = newBlock;
        } 

        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        transactionPool.addTransaction(tx);
    }

}
