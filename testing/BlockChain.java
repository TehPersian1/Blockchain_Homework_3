import java.util.ArrayList;
import java.util.HashMap;
import java.sql.Timestamp;

/* Block Chain should maintain only limited block nodes to satisfy the functions
   You should not have the all the blocks added to the block chain in memory 
   as it would overflow memory
 */

public class BlockChain {
   public static final int CUT_OFF_AGE = 10;
   private TransactionPool tPool = new TransactionPool();
   private ArrayList<BlockNode> bChain = new ArrayList<>();
   private BlockNode tallestNode;

   public BlockNode getParentNode(byte[] blockHash) {
      ByteArrayWrapper b1 = new ByteArrayWrapper(blockHash);
      for (BlockNode b : bChain) {
         ByteArrayWrapper b2 = new ByteArrayWrapper(b.b.getHash());
         if (b1.equals(b2)) {
            return b;
         }
      }
      return null;
   }

   // all information required in handling a block in block chain
   private class BlockNode {
      public Block b;
      public BlockNode parent;
      public ArrayList<BlockNode> children;
      public int height;
      // utxo pool for making a new block on top of this block
      private UTXOPool uPool;
      private TransactionPool tPool;
      private Timestamp createAt;

      public BlockNode(Block b, BlockNode parent, UTXOPool uPool, TransactionPool tPool) {
         this.b = b;
         this.parent = parent;
         children = new ArrayList<BlockNode>();
         this.uPool = uPool;
         if (parent != null) {
            height = parent.height + 1;
            parent.children.add(this);
         } else {
            height = 1;
         }
         this.tPool = tPool;
         this.createAt = new Timestamp(System.currentTimeMillis());
      }

      public UTXOPool getUTXOPoolCopy() {
         return new UTXOPool(uPool);
      }
      public TransactionPool getTransactionPoolCopy() {return new TransactionPool(tPool);
      }
   }

   /* create an empty block chain with just a genesis block.
    * Assume genesis block is a valid block
    */
   public BlockChain(Block genesisBlock) {
      UTXOPool uPool = new UTXOPool();
      TransactionPool tPool = new TransactionPool();
      for (int i = 0; i < genesisBlock.getCoinbase().numOutputs(); i++) {
         uPool.addUTXO(new UTXO(genesisBlock.getCoinbase().getHash(),i),genesisBlock.getCoinbase().getOutput(i));
      }
      tPool.addTransaction(genesisBlock.getCoinbase());
      for (Transaction t : genesisBlock.getTransactions()) {
         if (t != null) {
            for (int i=0;i<t.numOutputs();i++) {
               Transaction.Output output = t.getOutput(i);
               UTXO utxo = new UTXO(t.getHash(),i);
               uPool.addUTXO(utxo,output);
            }
            tPool.addTransaction(t);
         }
      }
      BlockNode b = new BlockNode(genesisBlock, tallestNode, uPool, tPool);
      tallestNode = b;
      bChain.add(b);
   }

   /* Get the maximum height block
    */
   public Block getMaxHeightBlock() {
      return tallestNode.b;
      // IMPLEMENT THIS
   }
   
   /* Get the UTXOPool for mining a new block on top of 
    * max height block
    */
   public UTXOPool getMaxHeightUTXOPool() {
      // IMPLEMENT THIS
      return tallestNode.uPool;
   }
   
   /* Get the transaction pool to mine a new block
    */
   public TransactionPool getTransactionPool() {
      // IMPLEMENT THIS
      return this.tPool;
   }

   /* Add a block to block chain if it is valid.
    * For validity, all transactions should be valid
    * and block should be at height > (maxHeight - CUT_OFF_AGE).
    * For example, you can try creating a new block over genesis block 
    * (block height 2) if blockChain height is <= CUT_OFF_AGE + 1. 
    * As soon as height > CUT_OFF_AGE + 1, you cannot create a new block at height 2.
    * Return true of block is successfully added
    */
   public boolean addBlock(Block b) {
      // IMPLEMENT THIS
      if (b.getPrevBlockHash() == null) {
         return false;
      }
      BlockNode parentNode = getParentNode(b.getPrevBlockHash());
      if (parentNode == null) {
         return false;
      }
      int blockHeight = parentNode.height + 1;
      if (blockHeight <= tallestNode.height - CUT_OFF_AGE) {
         return false;
      }
      UTXOPool uPool = new UTXOPool(parentNode.getUTXOPoolCopy());
      TransactionPool tPool = new TransactionPool(parentNode.getTransactionPoolCopy());
      for (Transaction t : b.getTransactions()) {
         TxHandler txHandler = new TxHandler(uPool);
         if (!txHandler.isValidTx(t)) {
            return false;
         }
         for (Transaction.Input input : t.getInputs()) {
            int outputIndex = input.outputIndex;
            byte[] prevTxHash = input.prevTxHash;
            UTXO utxo = new UTXO(prevTxHash, outputIndex);
            uPool.removeUTXO(utxo);
         }
         //add new utxo
         byte[] hash = t.getHash();
         for (int i = 0; i < t.numOutputs(); i++) {
            UTXO utxo = new UTXO(hash, i);
            uPool.addUTXO(utxo, t.getOutput(i));
         }
      }
      for (int i = 0; i < b.getCoinbase().numOutputs(); i++) {
         uPool.addUTXO(new UTXO(b.getCoinbase().getHash(), i), b.getCoinbase().getOutput(i));
      }
      for (Transaction t : b.getTransactions()) {
         tPool.removeTransaction(t.getHash());
      }

      BlockNode b2 = new BlockNode(b, parentNode, uPool, tPool);
      boolean addNewBlock = bChain.add(b2);
      if (addNewBlock) {
         {
            BlockNode currentMaxHeightNode = tallestNode;
            for (BlockNode b3 : bChain) {
               if (b3.height > currentMaxHeightNode.height) {
                  currentMaxHeightNode = b3;
               }
               else if (b3.height == currentMaxHeightNode.height) {
                  if (currentMaxHeightNode.createAt.after(b3.createAt)) {
                     currentMaxHeightNode = b3;
                  }
               }
            }
            tallestNode = currentMaxHeightNode;
         }
         }
         return addNewBlock;
      }

      /* Add a transaction in transaction pool
       */
      public void addTransaction (Transaction tx){
         // IMPLEMENT THIS
         this.tPool.addTransaction(tx);
      }
   }
